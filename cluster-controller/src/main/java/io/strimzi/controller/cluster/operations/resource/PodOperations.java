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

import io.fabric8.kubernetes.api.model.DoneablePod;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.vertx.core.Vertx;

/**
 * Operations for {@code Pod}s, which support {@link #isPodReady(String, String)} and
 * {@link #watch(String, String, Watcher)} in addition to the usual operations.
 */
public class PodOperations extends AbstractOperations<KubernetesClient, Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> {
    /**
     * Constructor
     * @param vertx The Vertx instance
     * @param client The Kubernetes client
     */
    public PodOperations(Vertx vertx, KubernetesClient client) {
        super(vertx, client, "Pods");
    }

    @Override
    protected MixedOperation<Pod, PodList, DoneablePod, PodResource<Pod, DoneablePod>> operation() {
        return client.pods();
    }

    /**
     * Returns whether the pod given by {@code namespace} and {@code name} is ready
     * @param namespace The namespace.
     * @param name The name.
     * @return True iff the pod is ready.
     */
    public boolean isPodReady(String namespace, String name) {
        return operation().inNamespace(namespace).withName(name).isReady();
    }

    /**
     * Watch the pod identified by the given {@code namespace} and {@code name} using the given {@code watcher}.
     * @param namespace The namespace
     * @param name The name
     * @param watcher The watcher
     * @return The watch
     */
    public Watch watch(String namespace, String name, Watcher<Pod> watcher) {
        return operation().inNamespace(namespace).withName(name).watch(watcher);
    }
}
