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

package io.enmasse.barnabas.operator.topic;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class TopicBuilderTest {

    @Test
    public void testConstructorWithNameAndPartitions() {
        Topic topic = new Topic.Builder("my_topic", 1).build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());
        assertEquals(emptyMap(), topic.getConfig());
    }

    @Test
    public void testConstructorWithNameAndPartitions2() {
        Topic topic = new Topic.Builder(new TopicName("my_topic"), 1).build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());
        assertEquals(emptyMap(), topic.getConfig());
    }

    @Test
    public void testConstructorWithConfig() {
        Topic topic = new Topic.Builder("my_topic", 1, singletonMap("foo", "bar")).build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());
        assertEquals(singletonMap("foo", "bar"), topic.getConfig());

        // Check we take a copy of the config
        Map<String, String> config = new HashMap<>();
        config.put("foo", "bar");
        Topic.Builder builder = new Topic.Builder("my_topic", 1, config);
        config.clear();
        topic = builder.build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());;
        assertEquals(singletonMap("foo", "bar"), topic.getConfig());
    }

    // TODO testConstructorWithTopic
    // TODO testWithMapName

    @Test
    public void testWithConfig() {
        Topic.Builder builder = new Topic.Builder("my_topic", 1);
        builder.withConfig(singletonMap("foo", "bar"));
        Topic topic = builder.build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());
        assertEquals(singletonMap("foo", "bar"), topic.getConfig());

        // Check we take a copy of the config
        Map<String, String> config = new HashMap<>();
        config.put("foo", "bar");
        builder = new Topic.Builder("my_topic", 1);
        builder.withConfig(config);
        config.clear();
        topic = builder.build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());;
        assertEquals(singletonMap("foo", "bar"), topic.getConfig());
    }

    @Test
    public void testWithConfigEntry() {
        Topic.Builder builder = new Topic.Builder("my_topic", 1);
        builder.withConfigEntry("foo", "bar");
        Topic topic = builder.build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());
        assertEquals(singletonMap("foo", "bar"), topic.getConfig());
    }

    @Test
    public void testWithoutConfigEntry() {
        Topic.Builder builder = new Topic.Builder("my_topic", 1, singletonMap("foo", "bar"));
        builder.withoutConfigEntry("foo");
        Topic topic = builder.build();
        assertEquals(new TopicName("my_topic"), topic.getTopicName());
        assertEquals(1, topic.getNumPartitions());
        assertEquals(-1, topic.getNumReplicas());
        assertEquals(emptyMap(), topic.getConfig());
    }
}
