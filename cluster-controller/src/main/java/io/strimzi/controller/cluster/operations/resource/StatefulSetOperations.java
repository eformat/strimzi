/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.strimzi.controller.cluster.operations.resource;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.extensions.DoneableStatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSet;
import io.fabric8.kubernetes.api.model.extensions.StatefulSetList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Operations for {@code StatefulSets}s, which supports {@link #rollingUpdate(String, String, Handler)}
 * in addition to the usual operations.
 */
public class StatefulSetOperations extends AbstractScalableOperations<KubernetesClient, StatefulSet, StatefulSetList, DoneableStatefulSet, RollableScalableResource<StatefulSet, DoneableStatefulSet>> {

    private static final Logger log = LoggerFactory.getLogger(StatefulSetOperations.class.getName());
    private final PodOperations podOperations;

    /**
     * Constructor
     * @param vertx The Vertx instance
     * @param client The Kubernetes client
     */
    public StatefulSetOperations(Vertx vertx, KubernetesClient client) {
        super(vertx, client, "StatefulSet");
        this.podOperations = new PodOperations(vertx, client);
    }

    @Override
    protected MixedOperation<StatefulSet, StatefulSetList, DoneableStatefulSet, RollableScalableResource<StatefulSet, DoneableStatefulSet>> operation() {
        return client.apps().statefulSets();
    }

    public void rollingUpdate(String namespace, String name, Handler<AsyncResult<Void>> handler) {
        final int replicas = get(namespace, name).getSpec().getReplicas();
        vertx.createSharedWorkerExecutor("kubernetes-ops-pool").executeBlocking(
                future -> {
                    try {
                        log.info("Doing rolling update of stateful set {} in namespace {}", name, namespace);

                        for (int i = 0; i < replicas; i++) {
                            String podName = name + "-" + i;
                            log.info("Rolling pod {}", podName);
                            Future deleted = Future.future();
                            Watcher<Pod> watcher = new RollingUpdateWatcher(deleted);

                            Watch watch = podOperations.watch(namespace, podName, watcher);
                            Future fut = podOperations.delete(namespace, podName);

                            // TODO do this async
                            while (!fut.isComplete() && !deleted.isComplete()) {
                                log.info("Waiting for pod {} to be deleted", podName);
                                Thread.sleep(1000);
                            }
                            // TODO Check success of fut and deleted futures

                            watch.close();

                            while (!podOperations.isPodReady(namespace, podName)) {
                                log.info("Waiting for pod {} to get ready", podName);
                                Thread.sleep(1000);
                            };

                            log.info("Pod {} rolling update complete", podName);
                        }

                        future.complete();
                    }
                    catch (Exception e) {
                        log.error("Caught exception while doing manual rolling update of stateful set {} in namespace {}", name, namespace);
                        future.fail(e);
                    }
                },
                false,
                res -> {
                    if (res.succeeded()) {
                        log.info("Stateful set {} in namespace {} has been rolled", name, namespace);
                        handler.handle(Future.succeededFuture());
                    }
                    else {
                        log.error("Failed to do rolling update of stateful set {} in namespace {}: {}", name, namespace, res.cause().toString());
                        handler.handle(Future.failedFuture(res.cause()));
                    }
                }
        );
    }

    static class RollingUpdateWatcher implements Watcher<Pod> {
        //private static final Logger log = LoggerFactory.getLogger(RollingUpdateWatcher.class.getName());
        private final Future deleted;

        public RollingUpdateWatcher(Future deleted) {
            this.deleted = deleted;
        }

        @Override
        public void eventReceived(Action action, Pod pod) {
            switch (action) {
                case DELETED:
                    log.info("Pod has been deleted");
                    deleted.complete();
                    break;
                case ADDED:
                case MODIFIED:
                    log.info("Ignored action {} while waiting for Pod deletion", action);
                    break;
                case ERROR:
                    log.error("Error while waiting for Pod deletion");
                    break;
                default:
                    log.error("Unknown action {} while waiting for pod deletion", action);
            }
        }

        @Override
        public void onClose(KubernetesClientException e) {
            if (e != null && !deleted.isComplete()) {
                log.error("Kubernetes watcher has been closed with exception!", e);
                deleted.fail(e);
            }
            else {
                log.info("Kubernetes watcher has been closed!");
            }
        }
    }
}
