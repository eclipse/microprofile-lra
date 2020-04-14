/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.AfterLRAListener.AFTER_LRA_LISTENER_WORK;
import static org.eclipse.microprofile.lra.tck.participant.api.LraResource.ACCEPT_WORK;
import static org.eclipse.microprofile.lra.tck.participant.api.LraResource.LRA_RESOURCE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LraResource.TIME_LIMIT;
import static org.eclipse.microprofile.lra.tck.participant.api.LraResource.TIME_LIMIT_HALF_SEC;
import static org.eclipse.microprofile.lra.tck.participant.api.LraResource.TRANSACTIONAL_WORK_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.JOIN_WITH_EXISTING_LRA_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.JOIN_WITH_EXISTING_LRA_PATH2;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.TCK_PARTICIPANT_RESOURCE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.AfterLRAListener.AFTER_LRA_LISTENER_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.AfterLRAParticipant.AFTER_LRA_PARTICIPANT_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.AfterLRAParticipant.AFTER_LRA_PARTICIPANT_WORK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.temporal.ChronoUnit;
import java.util.stream.IntStream;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.tck.participant.api.GenericLRAException;
import org.eclipse.microprofile.lra.tck.participant.api.LraResource;
import org.eclipse.microprofile.lra.tck.participant.api.NoLRAResource;
import org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource;
import org.eclipse.microprofile.lra.tck.participant.api.AfterLRAListener;
import org.eclipse.microprofile.lra.tck.participant.api.AfterLRAParticipant;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TckTests extends TckTestBase {

    @Inject
    private LRAMetricService lraMetricService;

    @Inject
    private LRATestService lraTestService;
    
    private enum CompletionType {
        complete, compensate, mixed
    }

    @Deployment(name = "tcktests")
    public static WebArchive deploy() {
        return TckTestBase.deploy(TckTests.class.getSimpleName().toLowerCase());
    }

    @Before
    public void before() {
        super.before();
    }

    /**
     * <p>
     * Starting LRA and cancelling it.
     * <p>
     * It's expected the LRA won't be listed in active LRAs when cancelled.
     */
    @Test
    public void cancelLRA() throws WebApplicationException {
        try {
            URI lra = lraClient.startLRA(null,lraClientId(), lraTimeout(), ChronoUnit.MILLIS);

            lraClient.cancelLRA(lra);

            assertTrue("LRA '" + lra + "' should not be active ",
                    lraClient.isLRAFinished(lra));
        } catch (GenericLRAException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * <p>
     * Starting LRA and closing it.
     * <p>
     * It's expected the LRA won't be listed in active LRAs when closed.
     */
    @Test
    public void closeLRA() throws WebApplicationException {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);

        lraClient.closeLRA(lra);

        assertTrue("LRA '" + lra + "' should not be active anymore",
                lraClient.isLRAFinished(lra));
    }

    @Test
    public void nestedActivity() throws WebApplicationException {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);
        WebTarget resourcePath = tckSuiteTarget
                .path(LRA_RESOURCE_PATH).path("nestedActivity");

        Response response = null;
        try {
            response = resourcePath
                    .request()
                    .header(LRA_HTTP_CONTEXT_HEADER, lra)
                    .put(Entity.text(""));
    
            assertEquals("Response status to ' " + resourcePath.getUri() + "' does not match.",
                    Response.Status.OK.getStatusCode(), response.getStatus());
    
            Object parentId = response.getHeaders().getFirst(LRA_HTTP_CONTEXT_HEADER);
    
            assertNotNull("Expecting to get parent LRA id as response from " + resourcePath.getUri(), parentId);
            assertEquals("The nested activity should return the parent LRA id. The call to " + resourcePath.getUri(),
                    parentId, lra.toString());
    
            URI nestedLraId = URI.create(response.readEntity(String.class)); // We can keep String.class here as it is in TCK
    
            // close the LRA
            lraClient.closeLRA(lra);
    
            // validate that the nested LRA was closed

            // the resource /activities/work is annotated with Type.REQUIRED so the container should have ended it
            assertTrue("Nested LRA id '" + lra + "' should be listed in the list of the active LRAs (from call to "
                    + resourcePath.getUri() + ")", lraClient.isLRAFinished(nestedLraId));
        } finally {
            if(response != null) {
                response.close();
            }
        } 
    }

    @Test
    public void completeMultiLevelNestedActivity() throws WebApplicationException {
        multiLevelNestedActivity(CompletionType.complete, 1);
    }

    @Test
    public void compensateMultiLevelNestedActivity() throws WebApplicationException {
        multiLevelNestedActivity(CompletionType.compensate, 1);
    }

    @Test
    @Ignore("https://github.com/eclipse/microprofile-lra/issues/274")
    public void mixedMultiLevelNestedActivity() throws WebApplicationException {
        multiLevelNestedActivity(CompletionType.mixed, 2);
    }

    @Test
    public void joinLRAViaHeader() throws WebApplicationException {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);

        WebTarget resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path(TRANSACTIONAL_WORK_PATH);
        Response response = resourcePath
                .request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));

        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);

        // validate that the implementation still knows about lraId
        assertFalse("LRA '" + lra + "' should be active as it is not closed yet.",
                lraClient.isLRAFinished(lra));

        // close the LRA
        lraClient.closeLRA(lra);
        lraTestService.waitForCallbacks(lra);
        
        // check that participant was told to complete
        assertEquals("Wrong completion count for call " + resourcePath.getUri() + ". Expecting the method LRA was completed "
                + "after joining the existing LRA " + lra, 
                1, lraMetricService.getMetric(LRAMetricType.Completed, lra, LraResource.class.getName()));
        
        // check that implementation no longer knows about lraId
        assertTrue("LRA '" + lra + "' should not be active anymore as it was closed yet.",
                lraClient.isLRAFinished(lra));
    }

    @Test
    public void join() throws WebApplicationException {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);
        WebTarget resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path(TRANSACTIONAL_WORK_PATH);
        Response response = resourcePath
                .request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);
        lraClient.closeLRA(lra);
        assertTrue("LRA '" + lra + "' should be active as it is not closed yet.",
                lraClient.isLRAFinished(lra));
    }

    /**
     * TCK test to verify that methods annotated with {@link AfterLRA}
     * are notified correctly when an LRA terminates
     */
    @Test
    public void testAfterLRAParticipant() throws WebApplicationException, InterruptedException {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);
        WebTarget resourcePath = tckSuiteTarget.path(AFTER_LRA_PARTICIPANT_PATH).path(AFTER_LRA_PARTICIPANT_WORK);
        Response response = resourcePath
                .request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);
        lraClient.closeLRA(lra);
        lraTestService.waitForCallbacks(lra);

        // verify that the LRA is now in one of the terminal states
        assertTrue("testAfterLRAParticipant: LRA did not finish",
                lraClient.isLRAFinished(lra, lraMetricService, AfterLRAParticipant.class.getName()));

        // verify that the resource was notified of the final state of the LRA
        assertTrue("testAfterLRAParticipant: end synchronization was not invoked on resource " + resourcePath.getUri(),
                lraMetricService.getMetric(LRAMetricType.Closed, lra, AfterLRAParticipant.class.getName()) >= 1);
    }

    /**
     * TCK test to verify that methods annotated with {@link AfterLRA}
     * are notified correctly when an LRA terminates
     */
    @Test
    public void testAfterLRAListener() {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);
        WebTarget resourcePath = tckSuiteTarget.path(AFTER_LRA_LISTENER_PATH).path(AFTER_LRA_LISTENER_WORK);
        Response response = resourcePath
                .request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);
        lraClient.closeLRA(lra);
        lraTestService.waitForCallbacks(lra);

        // verify that the LRA is now in one of the terminal states
        assertTrue("testAfterLRAListener: LRA did not finish",
                lraClient.isLRAFinished(lra, lraMetricService, AfterLRAListener.class.getName()));

        // verify that the resource was notified of the final state of the LRA
        assertTrue("testAfterLRAListener: end synchronization was not invoked on resource " + resourcePath.getUri(),
                lraMetricService.getMetric(LRAMetricType.Closed, lra, AfterLRAListener.class.getName()) >= 1);
    }

    @Test
    public void leaveLRA() throws WebApplicationException {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);
        WebTarget resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path(TRANSACTIONAL_WORK_PATH);
        Response response = resourcePath.request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));

        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);

        // perform a second request to the same method in the same LRA context to validate that multiple participants are not registered
        resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path(TRANSACTIONAL_WORK_PATH);
        response = resourcePath.request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);

        // call a method annotated with @Leave (should remove the participant from the LRA)
        resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path("leave");
        response = resourcePath.request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);

        lraClient.closeLRA(lra);
        lraTestService.waitForCallbacks(lra);

        // check that participant was not told to complete
        assertEquals("Wrong completion count when participant left the LRA. "
                + "Expecting the completed count hasn't change between start and end of the test. "
                + "The test call went to LRA resource at " + resourcePath.getUri(),
                -1, lraMetricService.getMetric(LRAMetricType.Completed, lra, LraResource.class.getName()));
    }

    @Test
    public void dependentLRA() throws WebApplicationException {
        // call a method annotated with NOT_SUPPORTED but one which programmatically starts an LRA and returns it via a header
        WebTarget resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path("startViaApi");
        Response response = resourcePath.request().put(Entity.text(""));
        Object lraHeader = response.getHeaders().getFirst(LRA_HTTP_CONTEXT_HEADER);
        String lraId = checkStatusReadAndCloseResponse(Response.Status.OK, response, resourcePath);

        // LRAs started within the invoked remote method should not be available to the caller via the context header
        assertNull("JAX-RS response to PUT request should not have returned the header " + LRA_HTTP_CONTEXT_HEADER
                + ". The test call went to " + resourcePath.getUri(), lraHeader);

        // check that the remote method returned an active LRA (ie check it's not null and then close it)
        assertNotNull("JAX-RS response to PUT request should have returned content of LRA id. The test call went to "
                + resourcePath.getUri(), lraId);

        lraClient.closeLRA(URI.create(lraId));
    }

    @Test
    public void timeLimit() {
        WebTarget resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path(TIME_LIMIT);
        Response response = resourcePath
                .request()
                .get();

        URI lraId = URI.create(response.readEntity(String.class));
        response.close();

        // Note that the timeout firing will cause the implementation to compensate
        // the LRA so it may no longer exist
        // (depends upon how long the implementation keeps a record of finished LRAs

        // check that participant was invoked
        lraTestService.waitForCallbacks(lraId);
        
        /*
         * The call to activities/timeLimit should have started an LRA which should have timed out
         * (because the invoked resource method sleeps for longer than the timeLimit annotation
         * attribute specifies). Therefore the participant should have compensated:
         */
        assertEquals("The LRA should have timed out but complete was called instead of compensate. "
                + "Expecting the number of complete call before test matches the ones after LRA timed out. "
                + "The test call went to " + resourcePath.getUri(), 
                0, lraMetricService.getMetric(LRAMetricType.Completed, lraId, LraResource.class.getName()));
        assertEquals("The LRA should have timed out and compensate should be called. "
                + "Expecting the number of compensate call before test is one less lower than the ones after LRA timed out. "
                + "The test call went to " + resourcePath.getUri(), 
                1, lraMetricService.getMetric(LRAMetricType.Compensated, lraId, LraResource.class.getName()));
    }

    /**
     * Service A - Timeout 500 ms
     * Service B - Type.MANDATORY
     *
     * Service A calls Service B after it has waited 1 sec.
     * Service A returns http Status from the call to Service B.
     *
     * test calls A and verifies if return is status 412 (precondition failed)
     * or 410 (gone) since LRA is not active when Service B endpoint is called.
     */
    @Test
    public void timeLimitWithPreConditionFailed() {
        WebTarget resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path(TIME_LIMIT_HALF_SEC);
        Response response = resourcePath
                .request()
                .get();

        // verify that a 412 response code was generated
        assertTrue("Expected 412 or 410 response",
                response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode() ||
                        response.getStatus() == Response.Status.GONE.getStatusCode()
                );

        response.close();
    }

    @Test
    public void acceptCloseTest() throws WebApplicationException, InterruptedException {
        joinAndEnd(true, LRA_RESOURCE_PATH, ACCEPT_WORK);
    }

    @Test
    public void acceptCancelTest() throws WebApplicationException, InterruptedException {
        joinAndEnd(false, LRA_RESOURCE_PATH, ACCEPT_WORK);
    }

    private void joinAndEnd(boolean close, String path, String path2)
            throws WebApplicationException, InterruptedException {
        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);
        WebTarget resourcePath = tckSuiteTarget.path(path).path(path2);

        Response response = resourcePath
                .request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));

        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);

        if (close) {
            lraClient.closeLRA(lra);
        } else {
            lraClient.cancelLRA(lra);
        }

        lraTestService.waitForRecovery(lra);

        int completionCount = lraMetricService.getMetric(LRAMetricType.Completed, lra, LraResource.class.getName());
        int compensationCount = lraMetricService.getMetric(LRAMetricType.Compensated, lra, LraResource.class.getName());

        // Complete or Compensate methods MUST not be repeated as the resource contains a Status method
        boolean wasCalled = (close ? completionCount == 1 : compensationCount == 1);
        boolean wasNotCalled = (close ? compensationCount == 0 : completionCount == 0);
        String lraMode = (close ? "close" : "cancel");
        String participantMode = (close ? "complete" : "compensate");

        assertTrue(String.format("acceptTest with %s: participant (%s) was not asked to %s (expecting only one call)",
                lraMode, resourcePath.getUri(), participantMode),
                wasCalled);
        assertTrue(String.format("acceptTest with %s: participant (%s) was asked to %s",
                lraMode, resourcePath.getUri(), participantMode),
                wasNotCalled);
        assertTrue("acceptTest: LRA did not finish", lraClient.isLRAFinished(lra));
    }

    @Test
    public void noLRATest() throws WebApplicationException {
        WebTarget resourcePath = tckSuiteTarget
                .path(NoLRAResource.NO_LRA_RESOURCE_PATH)
                .path(NoLRAResource.NON_TRANSACTIONAL_WORK_PATH);

        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);

        Response response = resourcePath.request().header(LRA_HTTP_CONTEXT_HEADER, lra)
                .put(Entity.text(""));

        String lraId = checkStatusReadAndCloseResponse(Response.Status.OK, response, resourcePath);

        assertEquals("While calling non-LRA method the resource returns not expected LRA id",
                lraId, lra.toString());

        lraClient.cancelLRA(lra);
        lraTestService.waitForCallbacks(lra);

        // check that second service (the LRA aware one), namely
        // {@link org.eclipse.microprofile.lra.tck.participant.api.LraResource#activityWithMandatoryLRA(String, String)}
        // was told to compensate
        int completedCount = lraMetricService.getMetric(LRAMetricType.Completed, lra, LraResource.class.getName());
        int compensatedCount = lraMetricService.getMetric(LRAMetricType.Compensated, lra, LraResource.class.getName());

        assertEquals("Completed should not be called on the LRA aware service. "
                + "The number of completed count for before and after test does not match. "
                + "The test call went to " + resourcePath.getUri(), 0, completedCount);
        assertEquals("Compensated service should be called on LRA aware service. The number of compensated count after test is bigger for one. "
                + "The test call went to " + resourcePath.getUri(), 1, compensatedCount);
    }

    /**
     * client invokes the same participant method twice in the same LRA context
     * cancel the LRA
     * check that the participant was asked to compensate only once
     */
    @Test
    public void joinWithOneResourceSameMethodTwiceWithCancel() throws WebApplicationException {
        joinWithOneResource("joinWithOneResourceSameMethodTwiceWithCancel",
                false, JOIN_WITH_EXISTING_LRA_PATH, JOIN_WITH_EXISTING_LRA_PATH);
    }

    /**
     * client invokes the same participant twice but using different methods in the same LRA context
     * cancel the LRA
     * check that the participant was asked to compensate only once
     */
    @Test
    public void joinWithOneResourceDifferentMethodTwiceWithCancel() throws WebApplicationException {
        joinWithOneResource("joinWithOneResourceDifferentMethodTwiceWithCancel",
                false, JOIN_WITH_EXISTING_LRA_PATH, JOIN_WITH_EXISTING_LRA_PATH2);
    }

    /**
     * client invokes the same participant method twice in the same LRA context
     * close the LRA
     * check that the participant was asked to complete only once
     */
    @Test
    public void joinWithOneResourceSameMethodTwiceWithClose() throws WebApplicationException {
        joinWithOneResource("joinWithOneResourceSameMethodTwiceWithClose",
                true, JOIN_WITH_EXISTING_LRA_PATH, JOIN_WITH_EXISTING_LRA_PATH);
    }

    /**
     * client invokes the same participant twice but using different methods in the same LRA context
     * close the LRA
     * check that the participant was asked to complete only once
     */
    @Test
    public void joinWithOneResourceDifferentMethodTwiceWithClose() throws WebApplicationException {
        joinWithOneResource("joinWithOneResourceDifferentMethodTwiceWithClose",
                true, JOIN_WITH_EXISTING_LRA_PATH, JOIN_WITH_EXISTING_LRA_PATH2);
    }

    /**
     * client invokes different participant in the same LRA context
     * close the LRA
     * check that both participants were asked to complete
     */
    @Test
    public void joinWithTwoResourcesWithClose() throws WebApplicationException {
        joinWithTwoResources(true);
    }

    /**
     * client invokes different participants in the same LRA context
     * cancel the LRA
     * check that both participants were asked to compensate
     */
    @Test
    public void joinWithTwoResourcesWithCancel() throws WebApplicationException {
        joinWithTwoResources(false);
    }

    private void joinWithOneResource(String methodName, boolean close, String resource1Method, String resource2Method)
            throws WebApplicationException {
        // set up the web target for the test
        WebTarget resource1Path = tckSuiteTarget.path(TCK_PARTICIPANT_RESOURCE_PATH).path(resource1Method);
        WebTarget resource2Path = tckSuiteTarget.path(TCK_PARTICIPANT_RESOURCE_PATH).path(resource2Method);

        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);

        // invoke the same JAX-RS resources twice in the context of the lra which should enlist the resource only once:
        Response response1 = resource1Path.request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response1, resource1Path);
        Response response2 = resource2Path.request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response2, resource2Path);

        if (close) {
            lraClient.closeLRA(lra);
            lraTestService.waitForCallbacks(lra);
            
            assertTrue(methodName + ": resource should have completed with no compensations",
                    1 <= lraMetricService.getMetric(LRAMetricType.Completed, lra, ParticipatingTckResource.class.getName()) &&
                    0 == lraMetricService.getMetric(LRAMetricType.Compensated, lra, ParticipatingTckResource.class.getName()));
        } else {
            lraClient.cancelLRA(lra);
            lraTestService.waitForCallbacks(lra);

            assertTrue(methodName + ":: resource should have compensated with no completions",
                    0 == lraMetricService.getMetric(LRAMetricType.Completed, lra, ParticipatingTckResource.class.getName()) &&
                    1 <= lraMetricService.getMetric(LRAMetricType.Compensated, lra, ParticipatingTckResource.class.getName()));
        }
    }

    private void joinWithTwoResources(boolean close) throws WebApplicationException {
        // set up the web target for the test
        WebTarget resource1Path = tckSuiteTarget.path(LRA_RESOURCE_PATH).path(TRANSACTIONAL_WORK_PATH);
        WebTarget resource2Path = tckSuiteTarget.path(TCK_PARTICIPANT_RESOURCE_PATH).path(JOIN_WITH_EXISTING_LRA_PATH);

        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);

        // invoke two JAX-RS resources in the context of the lra which should enlist them both:
        Response response1 = resource1Path.request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response1, resource1Path);
        Response response2 = resource2Path.request().header(LRA_HTTP_CONTEXT_HEADER, lra).put(Entity.text(""));
        checkStatusAndCloseResponse(Response.Status.OK, response2, resource2Path);

        if (close) {
            lraClient.closeLRA(lra);
            lraTestService.waitForCallbacks(lra);

            assertTrue("joinWithTwoResourcesWithClose: both resources should have completed",
                    lraMetricService.getMetric(LRAMetricType.Completed, lra, LraResource.class.getName()) == 1 &&
                    lraMetricService.getMetric(LRAMetricType.Completed, lra, ParticipatingTckResource.class.getName()) >= 1);
        } else {
            lraClient.cancelLRA(lra);
            lraTestService.waitForCallbacks(lra);

            assertTrue("joinWithTwoResourcesWithCancel: both resources should have compensated",
                    lraMetricService.getMetric(LRAMetricType.Compensated, lra, LraResource.class.getName()) == 1 &&
                    lraMetricService.getMetric(LRAMetricType.Compensated, lra, ParticipatingTckResource.class.getName()) >= 1);
        }
    }

    private void multiLevelNestedActivity(CompletionType how, int nestedCnt) throws WebApplicationException {
        WebTarget resourcePath = tckSuiteTarget.path(LRA_RESOURCE_PATH).path("multiLevelNestedActivity");

        if (how == CompletionType.mixed && nestedCnt <= 1) {
            how = CompletionType.complete;
        }

        URI lra = lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS);
        String lraId = lra.toString();

        Response response = resourcePath
                .queryParam("nestedCnt", nestedCnt)
                .request()
                .header(LRA_HTTP_CONTEXT_HEADER, lra)
                .put(Entity.text(""));

        String lraStr = checkStatusReadAndCloseResponse(Response.Status.OK, response, resourcePath);
        assertNotNull("expecting a LRA string returned from " + resourcePath.getUri(), lraStr);
        String[] lraArray = lraStr.split(","); // We keep here type String (and not URI) because of the easy String.split
        URI[] uris = new URI[lraArray.length];

        IntStream.range(0, uris.length).forEach(i -> {
            try {
                uris[i] = new URI(lraArray[i]);
            } catch (URISyntaxException e) {
                fail(String.format("%s (multiLevelNestedActivity): returned an invalid URI: %s",
                        resourcePath.getUri().toString(), e.getMessage()));
            }
        });
        // check that the multiLevelNestedActivity method returned the mandatory LRA followed by any nested LRAs
        assertEquals("multiLevelNestedActivity: step 1 (the test call went to " + resourcePath.getUri() + ")",
                nestedCnt + 1, lraArray.length);
        // first element should be the mandatory LRA
        assertEquals("multiLevelNestedActivity: step 2 (the test call went to " + resourcePath.getUri() + ")",
                lraId, lraArray[0]);

        // and the mandatory lra seen by the multiLevelNestedActivity method
        assertFalse("multiLevelNestedActivity: top level LRA should be active (path called " + resourcePath.getUri() + ")",
                lraClient.isLRAFinished(URI.create( lraArray[0])));

        lraTestService.waitForCallbacks(lra);
        int inMiddleCompletedCount = lraMetricService.getMetric(LRAMetricType.Completed);
        int inMiddleCompensatedCount = lraMetricService.getMetric(LRAMetricType.Compensated);

        // check that all nested activities were told to complete
        assertEquals("multiLevelNestedActivity: step 3 (called test path " + resourcePath.getUri() + ")",
                nestedCnt, inMiddleCompletedCount);
        // and that neither were told to compensate
        assertEquals("multiLevelNestedActivity: step 4 (called test path " + resourcePath.getUri() + ")",
                0, inMiddleCompensatedCount);

        // close the LRA
        if (how == CompletionType.compensate) {
            lraClient.cancelLRA(lra);
        } else if (how == CompletionType.complete) {
            lraClient.closeLRA(lra);
        } else {
            /*
             * The test is calling for a mixed outcome (a top level LRA L! and nestedCnt nested LRAs (L2, L3, ...)::
             * L1 the mandatory call (PUT "activities/multiLevelNestedActivity") registers participant C1
             *   the resource makes nestedCnt calls to "activities/nestedActivity" each of which create nested LRAs
             * L2, L3, ... each of which enlists a participant (C2, C3, ...) which are completed when the call returns
             * L2 is cancelled  which causes C2 to compensate
             * L1 is closed which triggers the completion of C1
             *
             * To summarise:
             *
             * - C1 is completed
             * - C2 is completed and then compensated
             * - C3, ... are completed
             */
            lraClient.cancelLRA(uris[1]); // compensate the first nested LRA
            lraClient.closeLRA(lra); // should not complete any nested LRAs (since they have already completed via the interceptor)
        }

        // validate that the top level and nested LRAs are gone
        IntStream.rangeClosed(0, nestedCnt).forEach(i -> assertTrue(
                String.format("multiLevelNestedActivity: %s LRA still active (resource path was %s)",
                        (i == 0 ? "top level" : "nested"), resourcePath.getUri()),
                lraClient.isLRAFinished(URI.create(lraArray[i]))));

        lraTestService.waitForCallbacks(lra);
        int afterCompletedCount = lraMetricService.getMetric(LRAMetricType.Completed);
        int afterCompensatedCount = lraMetricService.getMetric(LRAMetricType.Compensated);

        if (how == CompletionType.complete) {
            // make sure that all nested activities were not told to complete or cancel a second time
            assertEquals("multiLevelNestedActivity: step 5 (called test path " + resourcePath.getUri() + ")",
                    inMiddleCompletedCount + nestedCnt, afterCompletedCount);
            // and that neither were still not told to compensate
            assertEquals("multiLevelNestedActivity: step 6 (called test path " + resourcePath.getUri() + ")",
                    0, afterCompensatedCount);

        } else if (how == CompletionType.compensate) {
            /*
             * the test starts LRA1 calls a @Mandatory method multiLevelNestedActivity which enlists in LRA1
             * multiLevelNestedActivity then calls an @Nested method which starts L2 and enlists another participant
             *   when the method returns the nested participant is completed (ie completed count is incremented)
             * Canceling L1 should then compensate the L1 enlistment (ie compensate count is incremented)
             * which will then tell L2 to compensate (ie the compensate count is incremented again)
             */
            // each nested participant should have completed (the +nestedCnt)
            assertEquals("multiLevelNestedActivity: step 7 (called test path " + resourcePath.getUri() + ")",
                    nestedCnt, afterCompletedCount);
            // each nested participant should have compensated. The top level enlistment should have compensated (the +1)
            assertEquals("multiLevelNestedActivity: step 8 (called test path " + resourcePath.getUri() + ")",
                    inMiddleCompensatedCount + 1 + nestedCnt, afterCompensatedCount);
        } else {
            /*
             * The test is calling for a mixed outcome:
             * - the top level LRA was closed
             * - one of the nested LRAs was compensated the rest should have been completed
             */
            // there should be just 1 compensation (the first nested LRA)
            assertEquals("multiLevelNestedActivity: step 9 (called test path " + resourcePath.getUri() + ")",
                    1, afterCompensatedCount);
            /*
             * Expect nestedCnt + 1 completions, 1 for the top level and one for each nested LRA
             * (NB the first nested LRA is completed and compensated)
             * Note that the top level complete should not call complete again on the nested LRA
             */
            assertEquals("multiLevelNestedActivity: step 10 (called test path " + resourcePath.getUri() + ")",
                    nestedCnt + 1, afterCompletedCount);
        }
    }
}
