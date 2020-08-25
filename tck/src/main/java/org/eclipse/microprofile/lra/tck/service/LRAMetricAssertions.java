/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

import org.hamcrest.Matchers;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Assertion methods usable with metrics.
 */
@Dependent
public final class LRAMetricAssertions {

    @Inject
    private LRAMetricService lraMetricService;

    @Inject
    private LRATestService lraTestService;

    // ----------------------------- COMPENSATED -----------------------------------
    /**
     * Asserts that <b>compensated</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertCompensated(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.Compensated, lraId, participantClazz);
    }

    /**
     * Asserts that <b>compensated</b> was not called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotCompensated(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.Compensated, lraId, participantClazz);
    }

    /**
     * Asserts that <b>compensated</b> was called <code>expectedNumber</code> times for given LRA
     * and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param expectedNumber expected count for the compensated calls to be found in the metric data
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertCompensatedEquals(String message, int expectedNumber, URI lraId, Class<?> participantClazz) {
        assertEquals(message, expectedNumber, lraMetricService.getMetric(LRAMetricType.Compensated, lraId, participantClazz));
    }

    /**
     * Asserts that <b>compensated</b> was called <code>expectedNumber</code> times for all LRAs and resources,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param expectedNumber expected count for all the compensated calls to be found in the metric data
     */
    public void assertCompensatedAllEquals(String message, int expectedNumber) {
        assertEquals(message, expectedNumber, lraMetricService.getMetricAll(LRAMetricType.Compensated));
    }

    // ----------------------------- COMPLETED -------------------------------------
    /**
     * Asserts that <b>completed</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertCompleted(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.Completed, lraId, participantClazz);
    }

    /**
     * Asserts that <b>completed</b> was not called for given LRA and resource participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotCompleted(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.Completed, lraId, participantClazz);
    }

    /**
     * Asserts that <b>completed</b> was called <code>expectedNumber</code> times for given LRA
     * and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param expectedNumber expected count for the completed calls to be found in the metric data
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertCompletedEquals(String message, int expectedNumber, URI lraId, Class<?> participantClazz) {
        assertEquals(message, expectedNumber, lraMetricService.getMetric(LRAMetricType.Completed, lraId, participantClazz));
    }

    /**
     * Asserts that <b>completed</b> was called <code>expectedNumber</code> times for all LRAs and resources,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param expectedNumber expected count for all the compensated calls to be found in the metric data
     */
    public void assertCompletedAllEquals(String message, int expectedNumber) {
        assertEquals(message, expectedNumber, lraMetricService.getMetricAll(LRAMetricType.Completed));
    }

    // ----------------------------- CLOSED -----------------------------------
    /**
     * Asserts that <b>closed</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertClosed(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.Closed, lraId, participantClazz);
    }

    /**
     * Asserts that <b>closed</b> was not called for given LRA and resource participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotClosed(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.Closed, lraId, participantClazz);
    }

    // ----------------------------- CANCELLED -----------------------------------
    /**
     * Asserts that <b>cancelled</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertCancelled(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.Cancelled, lraId, participantClazz);
    }

    /**
     * Asserts that <b>cancelled</b> was not called for given LRA and resource participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotCancelled(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.Cancelled, lraId, participantClazz);
    }

    // ----------------------------- AFTERLRA -----------------------------------
    /**
     * Asserts that <b>afterLRA</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertAfterLRA(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.AfterLRA, lraId, participantClazz);
    }

    /**
     * Asserts that <b>afterLRA</b> was not called for given LRA and resource participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotAfterLRA(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.AfterLRA, lraId, participantClazz);
    }

    // ----------------------------- FORGET -----------------------------------
    /**
     * Asserts that <b>forget</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertForget(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.Forget, lraId, participantClazz);
    }

    /**
     * Asserts that <b>forget</b> was not called for given LRA and resource participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotForget(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.Forget, lraId, participantClazz);
    }

    /**
     * Asserts that <b>forget</b> was called <code>expectedNumber</code> times for given LRA
     * and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param expectedNumber expected count for the forget calls to be found in the metric data
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertForgetEquals(String message, int expectedNumber, URI lraId, Class<?> participantClazz) {
        assertEquals(message, expectedNumber, lraMetricService.getMetric(LRAMetricType.Forget, lraId, participantClazz));
    }

    // ----------------------------- STATUS -----------------------------------
    /**
     * Asserts that <b>status</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertStatus(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.Status, lraId, participantClazz);
    }

    /**
     * Asserts that <b>status</b> was not called for given LRA and resource participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotStatus(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.Status, lraId, participantClazz);
    }

    // ----------------------------- NESTED -----------------------------------
    /**
     * Asserts that <b>nested</b> was called for given LRA and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNested(String message, URI lraId, Class<?> participantClazz) {
        assertYes(message, LRAMetricType.Nested, lraId, participantClazz);
    }

    /**
     * Asserts that <b>nested</b> was not called for given LRA and resource participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNotNested(String message, URI lraId, Class<?> participantClazz) {
        assertNot(message, LRAMetricType.Nested, lraId, participantClazz);
    }

    /**
     * Asserts that <b>nested</b> was called <code>expectedNumber</code> times for given LRA
     * and participant class translated to fully qualified classname as String,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param expectedNumber expected count for the nested calls to be found in the metric data
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertNestedEquals(String message, int expectedNumber, URI lraId, Class<?> participantClazz) {
        assertEquals(message, expectedNumber, lraMetricService.getMetric(LRAMetricType.Nested, lraId, participantClazz));
    }

    // ----------------------------- FINISH ---------------------------------------
    /**
     * Asserts that given LRA within the resource name (taken as fully qualified class name) was finished,
     * if not the {@link AssertionError} with the given message is thrown.
     *
     * @param message assertion message when the check fails
     * @param lraId LRA id which the assertion check will be taken against
     * @param participantClazz  the participant class used as resource name in the map
     */
    public void assertFinished(String message, URI lraId, Class<?> participantClazz) {
        assertTrue(message, lraTestService.isLRAFinished(lraId, participantClazz.getName()));
    }

    private void assertYes(String message, LRAMetricType metricType, URI lraId, Class<?> participantClazz) {
        assertThat(message, lraMetricService.getMetric(metricType, lraId, participantClazz), Matchers.greaterThanOrEqualTo(1));
    }

    /**
     * Asserts that {@link LRAMetricType} was <b>not</b> called for given LRA and resource name
     * (taken as fully qualified class name), if not the {@link AssertionError} with the given message is thrown.
     */
    private void assertNot(String message, LRAMetricType metricType, URI lraId, Class<?> participantClazz) {
        assertEquals(message, 0, lraMetricService.getMetric(metricType, lraId, participantClazz));
    }
}
