/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.eclipse.microprofile.lra.tck.service;

import javax.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class LRAMetricService {

    private static final String ALL = "all-participants";
    
    // maintain metrics on a per LRA basis
    private Map<URI, Map<String, LRAMetric>> metrics = new HashMap<>();
    
    public void incrementMetric(LRAMetricType name, URI lraId) {
        incrementMetric(name, lraId, ALL);
    }
    
    public void incrementMetric(LRAMetricType name, URI lraId, String participant) {
        metrics.putIfAbsent(lraId, new HashMap<>());
        metrics.get(lraId).putIfAbsent(participant, new LRAMetric());
        metrics.get(lraId).get(participant).increment(name);
    }

    public int getMetric(LRAMetricType name) {
        AtomicInteger result = new AtomicInteger();

        metrics.values().forEach(participantMap -> 
            participantMap.values().forEach(metric -> result.addAndGet(metric.get(name))));

        return result.get();
    }

    public int getMetric(LRAMetricType name, URI lraId) {
        return getMetric(name, lraId, ALL);
    }

    public int getMetric(LRAMetricType metric, URI lraId, String participant) {
        if (metrics.containsKey(lraId) && metrics.get(lraId).containsKey(participant)) {
            return metrics.get(lraId).get(participant).get(metric);
        } else {
            return -1;
        }
    }

    public void clear() {
        metrics.clear();
    }
    
    /**
     * A class to hold all of the metrics gathered in the context of a single LRA.
     * We need stats per LRA since a misbehaving test may leave an LRA in need of
     * recovery which means that the compensate/complete call will continue to be
     * called when subsequent tests run - ie it is not possible to fully tear down
     * a failing test.
     */
    private static class LRAMetric {
        Map<LRAMetricType, AtomicInteger> metrics = Arrays.stream(LRAMetricType.values())
            .collect(Collectors.toMap(Function.identity(), t -> new AtomicInteger(0)));

        void increment(LRAMetricType metric) {
            if (metrics.containsKey(metric)) {
                metrics.get(metric).incrementAndGet();
            }
        }

        int get(LRAMetricType metric) {
            if (metrics.containsKey(metric)) {
                return metrics.get(metric).get();
            }

            return -1;
        }
    }

}
