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

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

/**
 * Metric service is a storage container that test beans may use to store data about processing. It stores number of
 * call types (defined by {@link LRAMetric}) per LRA id per participant.
 */
@ApplicationScoped
public class LRAMetricService {

    private Map<URI, Map<String, LRAMetric>> metricsPerLra = new HashMap<>();

    /**
     * It increments counter of the metric type for particular LRA id and particular participant class which is
     * translated to fully qualified class name as participant name.
     *
     * @param metricType
     *            increment counter of the specific metric type
     * @param lraId
     *            increment counter of the metric type assigned to this particular lra id
     * @param participantClazz
     *            the participant class which the metric increment is accounted to
     */
    public void incrementMetric(LRAMetricType metricType, URI lraId, Class<?> participantClazz) {
        String participantName = participantClazz.getName();
        metricsPerLra.putIfAbsent(lraId, new HashMap<>());
        metricsPerLra.get(lraId).putIfAbsent(participantName, new LRAMetric());
        metricsPerLra.get(lraId).get(participantName).increment(metricType);
    }

    /**
     * Returns count number for particular metric type regardless of the LRA id or the participant's name.
     *
     * @param metricType
     *            metric type to take sum of the metric counter for
     * @return sum of metric counters if of the particular metric type
     */
    public int getMetricAll(LRAMetricType metricType) {
        AtomicInteger result = new AtomicInteger();

        metricsPerLra.values().forEach(
                participantMap -> participantMap.values().forEach(metric -> result.addAndGet(metric.get(metricType))));

        return result.get();
    }

    /**
     * Returns count number for particular metric type.
     *
     * @param metricType
     *            counter for which the metric type will be returned
     * @param lraId
     *            counter for which lra id will be returned
     * @return metric counter defined based on the method parameters
     */
    public int getMetric(LRAMetricType metricType, URI lraId) {
        if (metricsPerLra.containsKey(lraId)) {
            return metricsPerLra.get(lraId).values().stream()
                    .mapToInt(lraMetric -> lraMetric.get(metricType))
                    .sum();
        } else {
            return 0;
        }
    }

    /**
     * Returns count number for particular metric type filtered by LRA id and the participant class which defines the
     * participant name (the fully qualified class name is used for it).
     *
     * @param metricType
     *            counter for which the metric type will be returned
     * @param lraId
     *            counter for which lra id will be returned
     * @param participantClazz
     *            counter for which the participant will be returned
     * @return metric counter defined based on the method parameters
     */
    public int getMetric(LRAMetricType metricType, URI lraId, Class<?> participantClazz) {
        return getMetric(metricType, lraId, participantClazz.getName());
    }

    /**
     * Returns count number for particular metric type filtered by LRA id and the participant's name. It's expected that
     * the participant name is defined as fully qualified participant class name.
     *
     * @param metricType
     *            counter for which metric type will be returned
     * @param lraId
     *            counter for which lra id will be returned
     * @param participantClassName
     *            counter for which the participant name will be returned
     * @return metric counter defined based on the method parameters
     */
    public int getMetric(LRAMetricType metricType, URI lraId, String participantClassName) {
        if (metricsPerLra.containsKey(lraId) && metricsPerLra.get(lraId).containsKey(participantClassName)) {
            return metricsPerLra.get(lraId).get(participantClassName).get(metricType);
        } else {
            return 0;
        }
    }

    /**
     * Clear the metric storage as whole.
     */
    public void clear() {
        metricsPerLra.clear();
    }

    /**
     * A class to hold all of the metrics gathered in the context of a single LRA. We need stats per LRA since a
     * misbehaving test may leave an LRA in need of recovery which means that the compensate/complete call will continue
     * to be called when subsequent tests run - ie it is not possible to fully tear down a failing test.
     */
    private static class LRAMetric {
        private Map<LRAMetricType, AtomicInteger> metrics = Arrays.stream(LRAMetricType.values())
                .collect(Collectors.toMap(Function.identity(), t -> new AtomicInteger(0)));

        void increment(LRAMetricType metricType) {
            if (metrics.containsKey(metricType)) {
                metrics.get(metricType).incrementAndGet();
            } else {
                throw new IllegalArgumentException("Cannot increment metric type " + metricType.name());
            }
        }

        int get(LRAMetricType metricType) {
            if (metrics.containsKey(metricType)) {
                return metrics.get(metricType).get();
            }

            return 0;
        }
    }

}
