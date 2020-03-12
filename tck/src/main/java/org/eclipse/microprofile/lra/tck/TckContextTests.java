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
package org.eclipse.microprofile.lra.tck;

import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.tck.participant.api.AfterLRAListener;
import org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.net.URI;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.tck.TckContextTests.HttpMethod.PUT;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.ASYNC_LRA_PATH1;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.ASYNC_LRA_PATH2;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.ASYNC_LRA_PATH3;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.CLEAR_STATUS_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.CONTEXT_CHECK_LRA_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LEAVE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LRA_TCK_FAULT_CODE_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LRA_TCK_FAULT_TYPE_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LRA_TCK_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.METRIC_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.NESTED_LRA_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.NESTED_LRA_PATH_WITH_CLOSE;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.NEW_LRA_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.REQUIRED_LRA_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.RESET_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.STATUS_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.TCK_CONTEXT_RESOURCE_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * test that Compensate, Complete, Status, Forget and Leave annotations work without an LRA annotation
 * test that resource methods that make outgoing requests do not modify the current context when they return
 */
@RunWith(Arquillian.class)
public class TckContextTests extends TckTestBase {

    enum HttpMethod {GET, PUT, POST}

    @Inject
    private LRAMetricService lraMetricService;

    @Inject
    private LRATestService lraTestService;

    @Deployment(name = "TckContextTests")
    public static WebArchive deploy() {
        return TckTestBase.deploy(TckContextTests.class.getSimpleName().toLowerCase());
    }

    @Before
    public void before() {
        super.before();
        invoke(RESET_PATH, HttpMethod.PUT, null, 200, null, 200);
    }

    @Test
    public void testBasicContextPropagation() {
        URI lra = URI.create(invoke(NEW_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.SUCCESS, 200));
        invoke(REQUIRED_LRA_PATH, PUT, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);

        // verify that the resource was asked to complete
        int completions = lraMetricService.getMetric(LRAMetricType.Completed, lra);

        assertEquals(testName.getMethodName() + ": Resource was not asked to complete",
                1, completions);
    }

    @Test
    public void testStatus() {
        // call a resource that begins and ends an LRA and coerces the resource to return ACCEPTED when asked to complete
        URI lra = URI.create(invoke(REQUIRED_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.ACCEPTED, 202));

        // verify that the resource was asked to complete and is in the state Completing
        String status = invoke(STATUS_PATH, HttpMethod.GET, lra, 202, ContextTckResource.EndPhase.SUCCESS, 200);
        lraTestService.waitForCallbacks(lra);
        assertEquals(testName.getMethodName() + ": participant is not completing", ParticipantStatus.Completing.name(), status);

        // clear the EndPhase override data so that the next status request returns completed or compensated
        invoke(CLEAR_STATUS_PATH, HttpMethod.POST, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);

        // trigger a replay of the end phase
        lraTestService.waitForRecovery(lra);

        // and verify that the resource was asked to complete
        status = invoke(STATUS_PATH, HttpMethod.GET, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);
        assertEquals(testName.getMethodName() + ": participant is not completed", ParticipantStatus.Completed.name(), status);
    }

    @Test
    public void testLeave() {
        // call a resource that begins but does not end an LRA
        URI lra = URI.create(invoke(NEW_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.SUCCESS, 200));
        // verify that the resource is active
        String status = invoke(STATUS_PATH, HttpMethod.GET, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);

        assertEquals(testName.getMethodName() + ": participant is not active", ParticipantStatus.Active.name(), status);

        // ask the resource to leave the active LRA
        invoke(LEAVE_PATH, PUT, lra);

        // end the LRA via a different resource since using the same one will re-enlist it
        lraClient.closeLRA(lra);

        // verify that the resource was not asked to complete
        int completions = lraMetricService.getMetric(LRAMetricType.Completed, lra);

        assertEquals(testName.getMethodName() + ": Resource left but was still asked to complete",
                0, completions);
    }

    @Test
    public void testForget() {
        int count;
        // call a resource that begins and ends an LRA and coerces the resource to return an invalid HTTP code (503) when asked to complete
        URI lra = URI.create(invoke(REQUIRED_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.FAILED, 503));

        // trigger a replay attempt
        lraTestService.waitForEndPhaseReplay(lra);

        // the implementation should have called status which will have returned 500
        count = lraMetricService.getMetric(LRAMetricType.Status, lra);
        assertTrue(testName.getMethodName() + " resource status should have been called", count >= 1);

        // the implementation should not call forget until it knows the participant status
        count = lraMetricService.getMetric(LRAMetricType.Forget, lra);
        assertEquals(testName.getMethodName() + " resource forget should not have been called", 0, count);

        // clear the fault
        invoke(CLEAR_STATUS_PATH, HttpMethod.POST, lra, 200, ContextTckResource.EndPhase.FAILED, 200);

        // trigger a replay of the end phase
        lraTestService.waitForRecovery(lra);

        // the implementation should have called status again which will have returned 200
        count = lraMetricService.getMetric(LRAMetricType.Status, lra);
        // the implementation should have called status at least once. Since we have alread called status in this test
        // check that the status count is at least 2
        assertTrue(testName.getMethodName() + " resource status should have been called again", count >= 2);
        // the implementation should call forget since it knows the participant status
        count = lraMetricService.getMetric(LRAMetricType.Forget, lra);
        assertTrue(testName.getMethodName() + " resource forget should have been called", count >= 1);
    }

    /*
     * test that the parent context is available when:
     * - a method executes with a nested LRA
     * - when a participant callback is invoked
     */
    @Test
    public void testParentContextAvailable() {
        // start an LRA
        URI topLevelLRA = URI.create(invoke(NEW_LRA_PATH, PUT, null));
        // start a nested LRA
        String result = invoke(NESTED_LRA_PATH, PUT, topLevelLRA);
        // the resource method should return the nested LRA and the top level LRA separated by a comma
        assertTrue(result.contains(","));
        assertEquals(testName.getMethodName() + ": wrong parent LRA", topLevelLRA, URI.create(result.split(",")[1]));

        URI nestedLRA = URI.create(result.split(",")[0]);

        // end the top level LRA
        invoke(REQUIRED_LRA_PATH, PUT, topLevelLRA);

        // check that the resource was asked to complete twice, one in the context of the nested LRA and a
        // second time in the context of the top level LRA

        int nestedCompletions = lraMetricService.getMetric(LRAMetricType.Completed, nestedLRA);
        assertEquals(testName.getMethodName() + ": resource should have completed for the nested LRA",
                1, nestedCompletions);

        int topLevelCompletions = lraMetricService.getMetric(LRAMetricType.Completed, topLevelLRA);
        assertEquals(testName.getMethodName() + ": resource should have completed for the top level LRA",
                1, topLevelCompletions);

        // and validate that the parent LRA header was present when the nested LRA was asked to complete
        int endCallsWithParentContextHeaderPresent = lraMetricService.getMetric(LRAMetricType.Nested, topLevelLRA);
        assertEquals(testName.getMethodName() +
                        ": when the resource was asked to complete a nested LRA the parent context header was missing",
                1, endCallsWithParentContextHeaderPresent);
    }

    @Test
    public void testForgetCalledForNestedParticipantsWhenParentIsClosed() {
        // start an LRA
        URI topLevelLRA = URI.create(invoke(NEW_LRA_PATH, PUT, null));
        // start a nested LRA
        String result = invoke(NESTED_LRA_PATH_WITH_CLOSE, PUT, topLevelLRA);
        // the resource method should return the nested LRA and the top level LRA separated by a comma
        assertTrue(result.contains(","));
        assertEquals(testName.getMethodName() + ": wrong parent LRA", topLevelLRA, URI.create(result.split(",")[1]));

        URI nestedLRA = URI.create(result.split(",")[0]);
        lraTestService.waitForCallbacks(nestedLRA);

        // nested LRA should be closed
        assertEquals(testName.getMethodName() + ": resource should have completed for the nested LRA",
            1, lraMetricService.getMetric(LRAMetricType.Completed, nestedLRA));

        // end the top level LRA
        invoke(REQUIRED_LRA_PATH, PUT, topLevelLRA);
        lraTestService.waitForCallbacks(topLevelLRA);

        assertEquals(testName.getMethodName() + ": resource should have completed for the top level LRA",
            1, lraMetricService.getMetric(LRAMetricType.Completed, topLevelLRA));
        // nested LRA Complete method should not be replayed when parent is closed
        assertEquals(testName.getMethodName() + ": resource should not have completed for the nested LRA again",
            1, lraMetricService.getMetric(LRAMetricType.Completed, nestedLRA));
        // nested LRA Forget should be called when parent is closed
        assertEquals(testName.getMethodName() + ": resource should have called forget for the nested LRA",
            1, lraMetricService.getMetric(LRAMetricType.Forget, nestedLRA));
        // nested is incremented twice: once for repeated Complete and once for Forget
        assertEquals(testName.getMethodName() + ": parent context not included in complete or forget callbacks",
            2, lraMetricService.getMetric(LRAMetricType.Nested, topLevelLRA));

    }

    // invoke a method in an LRA context which performs various outgoing calls checking that the notion of active context
    // conforms with what is written in the specification
    @Test
    public void testContextAfterRemoteCalls() {
        invoke(CONTEXT_CHECK_LRA_PATH, PUT, null);
    }

    @Test
    public void testAsync1Support() {
        URI lra = URI.create(invoke(ASYNC_LRA_PATH1, PUT, null));

        // verify that the resource was asked to complete
        int completions = lraMetricService.getMetric(LRAMetricType.Completed, lra);

        assertEquals(testName.getMethodName() + ": Resource was not asked to complete",
                1, completions);
    }

    @Test
    public void testAsync2Support() {
        URI lra = URI.create(invoke(ASYNC_LRA_PATH2, PUT, null));

        // verify that the resource was asked to complete
        int completions = lraMetricService.getMetric(LRAMetricType.Completed, lra);

        assertEquals(testName.getMethodName() + ": Resource was not asked to complete",
                1, completions);
    }

    @Test
    public void testAsync3Support() {
        // invoke an async resource that throws an exception which cancels the LRA
        URI lra = URI.create(invoke(ASYNC_LRA_PATH3, PUT, null, 404,
            ContextTckResource.EndPhase.SUCCESS, 200));
        lraTestService.waitForCallbacks(lra);

        // verify that the resource was asked to compensate
        int completions = lraMetricService.getMetric(LRAMetricType.Completed, lra);
        int compensations = lraMetricService.getMetric(LRAMetricType.Compensated, lra);

        assertEquals(testName.getMethodName() + ": Resource was asked to complete",
                0, completions);
        assertEquals(testName.getMethodName() + ": Resource was not asked to compensate",
                1, compensations);
    }

    @Test
    public void testAfterLRAEnlistmentDuringClosingPhase() {
        // call a resource that begins and ends an LRA and coerces the resource to return ACCEPTED when asked to complete
        URI lra = URI.create(invoke(REQUIRED_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.ACCEPTED, 202));

        // verify that the resource was asked to complete and is in the state Completing
        String status = invoke(STATUS_PATH, HttpMethod.GET, lra, 202, ContextTckResource.EndPhase.SUCCESS, 200);
        lraTestService.waitForCallbacks(lra);
        assertEquals(testName.getMethodName() + ": participant is not completing", ParticipantStatus.Completing.name(), status);

        // enlist an LRA listener
        Response enlistResponse = tckSuiteTarget.path(AfterLRAListener.AFTER_LRA_LISTENER_PATH)
            .path(AfterLRAListener.AFTER_LRA_LISTENER_WORK)
            .request()
            .header(LRA_HTTP_CONTEXT_HEADER, lra)
            .put(null);

        assertEquals("AfterLRA listener hasn't been enlisted for the notification",
            200, enlistResponse.getStatus());
        enlistResponse.close();

        // clear the EndPhase override data so that the next status request returns completed or compensated
        invoke(CLEAR_STATUS_PATH, HttpMethod.POST, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);

        // trigger a replay of the end phase
        lraTestService.waitForRecovery(lra);

        // verify that the resource was asked to complete
        assertEquals(testName.getMethodName() + ": participant is not completed", 1,
            lraMetricService.getMetric(LRAMetricType.Completed, lra));

        // and that afterLRA listener was notified
        assertTrue("AfterLRA listener registered during the Closing phase was not notified about the LRA close",
            lraMetricService.getMetric(LRAMetricType.Closed, lra, AfterLRAListener.class.getName()) >= 1);
    }

    private String invoke(String where, HttpMethod method, URI lraContext) {
        return invoke(where, method, lraContext, 200, ContextTckResource.EndPhase.SUCCESS, 200);
    }

    private String invoke(String where, HttpMethod method, URI lraContext, int expectStatus,
                          ContextTckResource.EndPhase finishWith, int finishStatus) {
        WebTarget resourcePath = tckSuiteTarget.path(TCK_CONTEXT_RESOURCE_PATH).path(where);
        Invocation.Builder builder = resourcePath.request()
                .header(LRA_TCK_FAULT_TYPE_HEADER, finishWith)
                .header(LRA_TCK_FAULT_CODE_HEADER, finishStatus);
        Response response;

        if (where.startsWith(METRIC_PATH) || (where.startsWith(CLEAR_STATUS_PATH))) {
            builder.header(LRA_TCK_HTTP_CONTEXT_HEADER, lraContext);
        } else {
            builder.header(LRA_HTTP_CONTEXT_HEADER, lraContext);
        }

        switch (method) {
            case GET:
                response =  builder.get();
                break;
            case PUT:
                response =  builder.put(Entity.text(""));
                break;
            case POST:
                response =  builder.post(Entity.text(""));
                break;
            default:
                throw new IllegalArgumentException("HTTP method not supported: " + method);
        }

        return checkStatusReadAndCloseResponse(Response.Status.fromStatusCode(expectStatus), response, resourcePath);
    }
}
