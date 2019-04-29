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

import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.tck.TckContextTests.HttpMethod.GET;
import static org.eclipse.microprofile.lra.tck.TckContextTests.HttpMethod.PUT;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.CONTEXT_CHECK_LRA_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LEAVE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LRA_TCK_FAULT_CODE_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LRA_TCK_FAULT_TYPE_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.LRA_TCK_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.METRIC_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ContextTckResource.NESTED_LRA_PATH;
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
        String lra = invoke(NEW_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.SUCCESS, 200);
        invoke(REQUIRED_LRA_PATH, PUT, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);

        // verify that the resource was asked to complete
        String completions = invoke(METRIC_PATH + "/" + Complete.class.getName(), GET, lra);

        assertEquals(testName.getMethodName() + ": Resource was not asked to complete",
                Integer.toString(1), completions);
    }

    @Test
    public void testStatus() throws InterruptedException {
        // call a resource that begins and ends an LRA and coerces the resource to return ACCEPTED when asked to complete
        String lra = invoke(REQUIRED_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.ACCEPTED, 202);

        // verfiy that the resource was asked to complete and is in the state Completing
        String status = invoke(STATUS_PATH, HttpMethod.GET, lra, 202, ContextTckResource.EndPhase.SUCCESS, 200);
        assertEquals(testName.getMethodName() + ": participant is not completing", ParticipantStatus.Completing.name(), status);

        // clear the EndPhase override data so that the next status request returns completed or compensated
        invoke(STATUS_PATH, PUT, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);

        // trigger a replay of the end phase
        replayEndPhase(TCK_CONTEXT_RESOURCE_PATH);

        // and verify that the resoure was asked to complete
        status = invoke(STATUS_PATH, HttpMethod.GET, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);
        assertEquals(testName.getMethodName() + ": participant is not completed", ParticipantStatus.Completed.name(), status);
    }

    @Test
    public void testLeave() {
        // call a resource that begins but does not end an LRA
        String lra = invoke(NEW_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.SUCCESS, 200);
        // verfiy that the resource is active
        String status = invoke(STATUS_PATH, HttpMethod.GET, lra, 200, ContextTckResource.EndPhase.SUCCESS, 200);

        assertEquals(testName.getMethodName() + ": participant is not active", ParticipantStatus.Active.name(), status);

        // ask the resource to leave the active LRA
        invoke(LEAVE_PATH, PUT, lra);

        // end the LRA via a different resource since using the same one will re-enlist it
        lraClient.closeLRA(lra);

        // verify that the resource was not asked to complete
        String completions = invoke(METRIC_PATH + "/" + Complete.class.getName(), GET, lra);

        assertEquals(testName.getMethodName() + ": Resource left but was still asked to complete",
                "0", completions);
    }

    @Test
    public void testForget() throws InterruptedException {
        String count;
        // call a resource that begins and ends an LRA and coerces the resource to return FAILED when asked to complete
        String lra = invoke(REQUIRED_LRA_PATH, PUT, null, 200, ContextTckResource.EndPhase.FAILED, 500);

        // trigger a replay attempt
        replayEndPhase(TCK_CONTEXT_RESOURCE_PATH);

        // the implementation should have called status which will have returned 500
        count = invoke(METRIC_PATH + "/" + Status.class.getName(), GET, lra);
        assertEquals(testName.getMethodName() + " resource status should have been called", "1", count);

        // the implementation should not call forget until it knows the particpant status
        count = invoke(METRIC_PATH + "/" + Forget.class.getName(), GET, lra);
        assertEquals(testName.getMethodName() + " resource forget should not have been called", "0", count);

        // clear the fault
        invoke(STATUS_PATH, PUT, lra, 200, ContextTckResource.EndPhase.FAILED, 200);

        // trigger a replay of the end phase
        replayEndPhase(TCK_CONTEXT_RESOURCE_PATH);

        // the implementation should have called status again which will have returned 200
        count = invoke(METRIC_PATH + "/" + Status.class.getName(), GET, lra);
        assertEquals(testName.getMethodName() + " resource status should have been called again", "2", count);
        // the implementation should call forget since it knows the particpant status
        count = invoke(METRIC_PATH + "/" + Forget.class.getName(), GET, lra);
        assertEquals(testName.getMethodName() + " resource forget should have been called", "1", count);
    }

    /*
     * test that the parent context is available when:
     * - a method executes with a nested LRA
     * - when a participant callback is invoked
     */
    @Test
    public void testParentContextAvailable() {
        // start an LRA
        String topLevelLRA = invoke(NEW_LRA_PATH, PUT, null);
        // start a nested LRA
        String result = invoke(NESTED_LRA_PATH, PUT, topLevelLRA);
        // the resource method should return the nested LRA and the top level LRA separated by a comma
        assertTrue(result.contains(","));
        assertEquals(testName.getMethodName() + ": wrong parent LRA", topLevelLRA, result.split(",")[1]);

        String nestedLRA = result.split(",")[0];

        // end the top level LRA
        invoke(REQUIRED_LRA_PATH, PUT, topLevelLRA);

        // check that the resource was asked to complete twice, one in the context of the nested LRA and a
        // second time in the context of the top level LRA

        String nestedCompletions = invoke(METRIC_PATH + "/" + Complete.class.getName(), GET, nestedLRA);
        assertEquals(testName.getMethodName() + ": resource should have completed for the nested LRA",
                "1", nestedCompletions);

        String topLevelCompletions = invoke(METRIC_PATH + "/" + Complete.class.getName(), GET, topLevelLRA);
        assertEquals(testName.getMethodName() + ": resource should have completed for the top level LRA",
                "1", topLevelCompletions);

        // and validate that the parent LRA header was present when the nested LRA was asked to complete
        String endCallsWithParentContextHeaderPresent = invoke(METRIC_PATH + "/" + LRA.Type.NESTED.name(),
                GET, topLevelLRA);
        assertEquals(testName.getMethodName() +
                        ": when the resource was asked to complete a nested LRA the parent context header was missing",
                "1", endCallsWithParentContextHeaderPresent);
    }

    // invoke a method in an LRA context which performs various outgoing calls checking that the notion of active context
    // conforms with what is written in the specification
    @Test
    public void testContextAfterRemoteCalls() {
        invoke(CONTEXT_CHECK_LRA_PATH, PUT, null);
    }

    private String invoke(String where, HttpMethod method, String lraContext) {
        return invoke(where, method, lraContext, 200, ContextTckResource.EndPhase.SUCCESS, 200);
    }

    private String invoke(String where, HttpMethod method, String lraContext, int expectStatus,
                          ContextTckResource.EndPhase finishWith, int finishStatus) {
        WebTarget resourcePath = tckSuiteTarget.path(TCK_CONTEXT_RESOURCE_PATH).path(where);
        Invocation.Builder builder = resourcePath.request()
                .header(LRA_TCK_FAULT_TYPE_HEADER, finishWith)
                .header(LRA_TCK_FAULT_CODE_HEADER, finishStatus);
        Response response;

        if (where.startsWith(METRIC_PATH) || (where.startsWith(STATUS_PATH) && method == PUT)) {
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
