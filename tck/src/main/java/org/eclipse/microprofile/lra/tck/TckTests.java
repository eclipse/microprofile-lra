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

import org.eclipse.microprofile.lra.client.GenericLRAException;
import org.eclipse.microprofile.lra.client.LRAClient;
import org.eclipse.microprofile.lra.client.LRAInfo;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.eclipse.microprofile.lra.client.LRAClient.LRA_COORDINATOR_HOST_KEY;
import static org.eclipse.microprofile.lra.client.LRAClient.LRA_COORDINATOR_PORT_KEY;
import static org.eclipse.microprofile.lra.client.LRAClient.LRA_COORDINATOR_PATH_KEY;
import static org.eclipse.microprofile.lra.client.LRAClient.LRA_RECOVERY_PATH_KEY;
import static org.eclipse.microprofile.lra.tck.participant.api.ActivityController.ACCEPT_WORK;
import static org.eclipse.microprofile.lra.tck.participant.api.ActivityController.ACTIVITIES_PATH;

public class TckTests {
    private static final Long LRA_TIMEOUT_MILLIS = 50000L;
    private static URL micrserviceBaseUrl;
    private static URL rcBaseUrl;

    private static final int COORDINATOR_SWARM_PORT = 8082;
    private static final int TEST_SWARM_PORT = 8080;

    private static LRAClient lraClient;
    private static Client msClient;
    private static Client rcClient;

    private WebTarget msTarget;
    private WebTarget recoveryTarget;

    private static List<LRAInfo> oldLRAs;

    private enum CompletionType {
        complete, compensate, mixed
    }

    @BeforeClass
    public static void beforeClass(LRAClient lraClient) {
        initTck(lraClient);
    }

    public TckResult runTck(LRAClient lraClient, String testname, boolean verbose) {
        TckResult run = new TckResult();

        initTck(lraClient);

        run.add("timeLimit", TckTests::timeLimit, verbose);
        run.add("startLRA", TckTests::startLRA, verbose);
        run.add("cancelLRA", TckTests::cancelLRA, verbose);
        run.add("closeLRA", TckTests::closeLRA, verbose);
        run.add("getActiveLRAs", TckTests::getActiveLRAs, verbose);
        run.add("getAllLRAs", TckTests::getAllLRAs, verbose);
        run.add("isActiveLRA", TckTests::isActiveLRA, verbose);
        run.add("nestedActivity", TckTests::nestedActivity, verbose);
        run.add("completeMultiLevelNestedActivity", TckTests::completeMultiLevelNestedActivity, verbose);
        run.add("compensateMultiLevelNestedActivity", TckTests::compensateMultiLevelNestedActivity, verbose);
        run.add("mixedMultiLevelNestedActivity", TckTests::mixedMultiLevelNestedActivity, verbose);
        run.add("joinLRAViaHeader", TckTests::joinLRAViaHeader, verbose);
        run.add("join", TckTests::join, verbose);
        run.add("leaveLRA", TckTests::leaveLRA, verbose);
        run.add("leaveLRAViaAPI", TckTests::leaveLRAViaAPI, verbose);
        run.add("dependentLRA", TckTests::dependentLRA, verbose);
        run.add("cancelOn", TckTests::cancelOn, verbose);
        run.add("cancelOnFamily", TckTests::cancelOnFamily, verbose);
        run.add("acceptTest", TckTests::acceptTest, verbose);

        run.runTests(this, testname);

        return run;
    }

    private static void initTck(LRAClient lraClient) {
        TckTests.lraClient = lraClient;

        try {
            if (Boolean.valueOf(System.getProperty("enablePause", "true"))) {
                System.out.println("Getting ready to connect - expecting swarm lra coordinator is already up...");
                Thread.sleep(1000);
            }

            int servicePort = Integer.getInteger("service.http.port", TEST_SWARM_PORT);
            String rcHost = System.getProperty(LRA_COORDINATOR_HOST_KEY, "localhost");
            int rcPort = Integer.getInteger(LRA_COORDINATOR_PORT_KEY, COORDINATOR_SWARM_PORT);
            String coordinatorPath = System.getProperty(LRA_COORDINATOR_PATH_KEY, "lra-coordinator");

            micrserviceBaseUrl = new URL(String.format("http://localhost:%d", servicePort));
            rcBaseUrl = new URL(String.format("http://%s:%d", rcHost, rcPort));

            lraClient.setCoordinatorURI(new URI(String.format("http://%s:%d/%s", rcHost, rcPort, coordinatorPath)));
            msClient = ClientBuilder.newClient();
            rcClient = ClientBuilder.newClient();

            oldLRAs = new ArrayList<>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterClass
    public static void afterClass() {
        oldLRAs.clear();
        lraClient.close();
        msClient.close();
        rcClient.close();
    }

    @Before
    public void before() {
        try {
            msTarget = msClient.target(URI.create(new URL(micrserviceBaseUrl, "/").toExternalForm()));
            recoveryTarget = rcClient.target(URI.create(new URL(rcBaseUrl, "/").toExternalForm()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @After
    public void after() {
        List<LRAInfo> activeLRAs = lraClient.getActiveLRAs();

        if (activeLRAs.size() != 0) {
            activeLRAs.forEach(lra -> {
                try {
                    if (!oldLRAs.contains(lra)) {
                        System.out.printf("%s: WARNING: test did not close %s%n", "testName.getMethodName()", lra.getLraId());
                        oldLRAs.add(lra);
                        lraClient.closeLRA(new URL(lra.getLraId()));
                    }
                } catch (WebApplicationException | MalformedURLException e) {
                    System.out.printf("After Test: exception %s closing %s%n", e.getMessage(), lra.getLraId());
                }
            });
        }
//        Current.popAll();
    }

    @Test
    private String startLRA() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#startLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        lraClient.closeLRA(lra);

        return lra.toExternalForm();
    }

    @Test
    private String cancelLRA() throws WebApplicationException {
        URL lra = lraClient.startLRA(null,"SpecTest#cancelLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        lraClient.cancelLRA(lra);

        List<LRAInfo> lras = lraClient.getAllLRAs();

        assertNull(getLra(lras, lra.toExternalForm()), "cancelLRA via client: lra still active", null);

        return lra.toExternalForm();
    }

    @Test
    private String closeLRA() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#closelLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        lraClient.closeLRA(lra);

        List<LRAInfo> lras = lraClient.getAllLRAs();

        assertNull(getLra(lras, lra.toExternalForm()), "closeLRA via client: lra still active", null);

        return lra.toExternalForm();
    }

    @Test
    private String getActiveLRAs() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#getActiveLRAs", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        List<LRAInfo> lras = lraClient.getActiveLRAs();

        assertNotNull(getLra(lras, lra.toExternalForm()), "getActiveLRAs: getLra returned null", null);

        lraClient.closeLRA(lra);

        return lra.toExternalForm();
    }

    @Test
    private String getAllLRAs() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#getAllLRAs", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        List<LRAInfo> lras = lraClient.getAllLRAs();

        assertNotNull(getLra(lras, lra.toExternalForm()), "getAllLRAs: getLra returned null", null);

        lraClient.closeLRA(lra);

        return "passed";
    }

    //    @Test
    private void getRecoveringLRAs() throws WebApplicationException {
        // TODO
    }

    @Test
    private String isActiveLRA() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#isActiveLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        assertTrue(lraClient.isActiveLRA(lra), null, null, lra);

        lraClient.closeLRA(lra);

        return lra.toExternalForm();
    }

    // the coordinator cleans up when canceled
    @Test
    private String isCompensatedLRA() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#isCompensatedLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        lraClient.cancelLRA(lra);

        assertTrue(lraClient.isCompensatedLRA(lra), null, null, lra);

        return lra.toExternalForm();
    }

    // the coordinator cleans up when completed
    @Test
    private String isCompletedLRA() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#isCompletedLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        lraClient.closeLRA(lra);

        assertTrue(lraClient.isCompletedLRA(lra), null, null, lra);

        return lra.toExternalForm();
    }

    @Test
    private String joinLRAViaBody() throws WebApplicationException {

        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("work");
        Response response = resourcePath.request().put(Entity.text(""));

        String lra = checkStatusAndClose(response, Response.Status.OK.getStatusCode(), true, resourcePath);

        // validate that the LRA coordinator no longer knows about lraId
        List<LRAInfo> lras = lraClient.getActiveLRAs();

        // the resource /activities/work is annotated with Type.REQUIRED so the container should have ended it
        assertNull(getLra(lras, lra), "joinLRAViaBody: lra is still active", resourcePath);

        return "passed";
    }

    @Test
    private String nestedActivity() throws WebApplicationException {
        URL lra = lraClient.startLRA(null, "SpecTest#nestedActivity", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        WebTarget resourcePath = msTarget
                .path(ACTIVITIES_PATH).path("nestedActivity");

        Response response = resourcePath
                .request()
                .header(LRAClient.LRA_HTTP_HEADER, lra)
                .put(Entity.text(""));

        Object parentId = response.getHeaders().getFirst(LRAClient.LRA_HTTP_HEADER);

        assertNotNull(parentId, "nestedActivity: null parent LRA", resourcePath);
        assertEquals(lra.toExternalForm(), parentId, "nestedActivity should have returned the parent LRA", resourcePath);

        String nestedLraId = checkStatusAndClose(response, Response.Status.OK.getStatusCode(), true, resourcePath);

        List<LRAInfo> lras = lraClient.getActiveLRAs();

        // close the LRA
        lraClient.closeLRA(lra);

        // validate that the nested LRA was closed
        lras = lraClient.getActiveLRAs();

        // the resource /activities/work is annotated with Type.REQUIRED so the container should have ended it
        assertNull(getLra(lras, nestedLraId), "nestedActivity: nested LRA should not be active", resourcePath);

        return lra.toExternalForm();
    }

    @Test
    private String completeMultiLevelNestedActivity() throws WebApplicationException {
        return multiLevelNestedActivity(CompletionType.complete, 1);
    }

    @Test
    private String compensateMultiLevelNestedActivity() throws WebApplicationException {
        return multiLevelNestedActivity(CompletionType.compensate, 1);
    }

    @Test
    private String mixedMultiLevelNestedActivity() throws WebApplicationException {
        return multiLevelNestedActivity(CompletionType.mixed, 2);
    }

    @Test
    private String joinLRAViaHeader () throws WebApplicationException {
        int cnt1 = completedCount(true);

        URL lra = lraClient.startLRA(null, "SpecTest#joinLRAViaBody", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("work");
        Response response = resourcePath
                .request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        // validate that the LRA coordinator still knows about lraId
        List<LRAInfo> lras = lraClient.getActiveLRAs();
        assertNotNull(getLra(lras, lra.toExternalForm()), "joinLRAViaHeader: missing lra", resourcePath);

        // close the LRA
        lraClient.closeLRA(lra);

        // check that LRA coordinator no longer knows about lraId
        lras = lraClient.getActiveLRAs();
        assertNull(getLra(lras, lra.toExternalForm()), "joinLRAViaHeader: LRA should not be active", resourcePath);

        // check that participant was told to complete
        int cnt2 = completedCount(true);
        assertEquals(cnt1 + 1, cnt2, "joinLRAViaHeader: wrong completion count", resourcePath);

        return "passed";
    }

    @Test
    private String join () throws WebApplicationException {
        List<LRAInfo> lras = lraClient.getActiveLRAs();
        int count = lras.size();
        URL lra = lraClient.startLRA(null, "SpecTest#join", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("work");
        Response response = resourcePath
                .request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);
        lraClient.closeLRA(lra);

        lras = lraClient.getActiveLRAs();
        System.out.printf("join ok %d versus %d lras%n", count, lras.size());
        assertEquals(count, lras.size(), "join: wrong LRA count", resourcePath);

        return lra.toExternalForm();
    }

    @Test
    private String leaveLRA() throws WebApplicationException {
        int cnt1 = completedCount(true);
        URL lra = lraClient.startLRA(null, "SpecTest#leaveLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("work");
        Response response = resourcePath.request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));

        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        // perform a second request to the same method in the same LRA context to validate that multiple participants are not registered
        resourcePath = msTarget.path(ACTIVITIES_PATH).path("work");
        response = resourcePath.request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        // call a method annotated with @Leave (should remove the participant from the LRA)
        resourcePath = msTarget.path(ACTIVITIES_PATH).path("leave");
        response = resourcePath.request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        // lraClient.leaveLRA(lra, "some participant"); // ask the MS for the participant url so we can test LRAOldClient

        lraClient.closeLRA(lra);

        // check that participant was not told to complete
        int cnt2 = completedCount(true);

        assertEquals(cnt1, cnt2, "leaveLRA: wrong completion count", resourcePath);

        return lra.toExternalForm();
    }

    @Test
    private String leaveLRAViaAPI() throws WebApplicationException {
        int cnt1 = completedCount(true);
        URL lra = lraClient.startLRA(null, "SpecTest#leaveLRA", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);

        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("work");

        Response response = resourcePath.request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        // perform a second request to the same method in the same LRA context to validate that multiple participants are not registered
        resourcePath = msTarget.path(ACTIVITIES_PATH).path("work");
        response = resourcePath.request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        // call a method annotated with @Leave (should remove the participant from the LRA)
        try {
            resourcePath = msTarget.path(ACTIVITIES_PATH).path("leave");
            response = resourcePath.path(URLEncoder.encode(lra.toString(), "UTF-8"))
                    .request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));
        } catch (UnsupportedEncodingException e) {
            throw new WebApplicationException(
                    Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                            Entity.text(String.format("%s: %s", resourcePath.getUri().toString(), e.getMessage()))).build());
        }
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        // lraClient.leaveLRA(lra, "some participant"); // ask the MS for the participant url so we can test LRAOldClient

        lraClient.closeLRA(lra);

        // check that participant was not told to complete
        int cnt2 = completedCount(true);

        assertEquals(cnt1, cnt2,
                String.format("leaveLRAViaAPI: wrong count %d versus %d", cnt1, cnt2), resourcePath);

        return "passed";
    }

    @Test
    private String dependentLRA() throws WebApplicationException {
        // call a method annotated with NOT_SUPPORTED but one which programatically starts an LRA and returns it via a header
        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("startViaApi");
        Response response = resourcePath.request().put(Entity.text(""));
        // check that the method started an LRA
        Object lraHeader = response.getHeaders().getFirst(LRAClient.LRA_HTTP_HEADER);

        String id = checkStatusAndClose(response, Response.Status.OK.getStatusCode(), true, resourcePath);

        // the value returned via the header and body should be equal

        assertNotNull(lraHeader, String.format("JAX-RS response to PUT request should have returned the header %s",
                LRAClient.LRA_HTTP_HEADER), resourcePath);
        assertNotNull(id, "JAX-RS response to PUT request should have returned content", resourcePath);
        assertEquals(id, lraHeader.toString(), "dependentLRA: resource returned wrong LRA", resourcePath);

        try {
            lraClient.closeLRA(new URL(lraHeader.toString()));
        } catch (MalformedURLException e) {
            throw new WebApplicationException(e);
        }

        return "passed";
    }

    @Test
    private String cancelOn() {
        cancelCheck("cancelOn");

        return "passed";
    }

    @Test
    private String cancelOnFamily() {
        cancelCheck("cancelOnFamily");

        return "passed";
    }

    @Test
    private String timeLimit() {
        int[] cnt1 = {completedCount(true), completedCount(false)};
        Response response = null;

        try {
            WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("timeLimit");
            response = resourcePath
                    .request()
                    .get();

            checkStatusAndClose(response, -1, true, resourcePath);

            // Note that the timeout firing will cause the coordinator to compensate
            // the LRA so it may no longer exist
            // (depends upon how long the coordinator keeps a record of finished LRAs

            // check that participant was invoked
            int[] cnt2 = {completedCount(true), completedCount(false)};

            /*
             * The call to activities/timeLimit should have started an LRA whch should have timed out
             * (because the called resource method sleeps for long than the @TimeLimit annotation specifies).
             * Therefore the it should have compensated:
             */
            assertEquals(cnt1[0], cnt2[0],
                    "timeLimit: complete was called instead of compensate", resourcePath);
            assertEquals(cnt1[1] + 1, cnt2[1],
                    "timeLimit: compensate should have been called", resourcePath);
        } finally {

            if (response != null)
                response.close();
        }

        return "passed";
    }

    /*
     * Participants can pass data during enlistment and this data will be returned during
     * the complete/compensate callbacks
     */
    private void testUserData() {
        List<LRAInfo> lras = lraClient.getActiveLRAs();
        int count = lras.size();
        String testData = "test participant data";
        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("testUserData");

        Response response = resourcePath
                .request().put(Entity.text(testData));

        String activityId = response.readEntity(String.class);
        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        lras = lraClient.getActiveLRAs();

        assertEquals(count, lras.size(), "testUserData: testUserData produced the wrong LRA count",
                resourcePath);

        response = msTarget.path(ACTIVITIES_PATH).path("getActivity")
                .queryParam("activityId", activityId)
                .request()
                .get();

        String activity = response.readEntity(String.class);

        // validate that the service received the correct data during the complete call
        assertTrue(activity.contains("userData='" + testData), null, null, null);
        assertTrue(activity.contains("endData='" + testData), null, null, null);
    }

    @Test
    private String acceptTest() throws WebApplicationException {
        joinAndEnd(true, true, ACTIVITIES_PATH, ACCEPT_WORK);
        return "passed";
    }

    // TODO the spec does not specifiy recovery semantics
    @Test
    private void joinAndEnd(boolean waitForRecovery, boolean close, String path, String path2) throws WebApplicationException {
        int countBefore = lraClient.getActiveLRAs().size();
        URL lra = lraClient.startLRA(null, "SpecTest#join", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        WebTarget resourcePath = msTarget.path(path).path(path2);

        Response response = resourcePath
                .request().header(LRAClient.LRA_HTTP_HEADER, lra).put(Entity.text(""));

        checkStatusAndClose(response, Response.Status.OK.getStatusCode(), false, resourcePath);

        if (close)
            lraClient.closeLRA(lra);
        else
            lraClient.cancelLRA(lra);

        if (waitForRecovery) {
            String recoveryPath = System.getProperty(LRA_RECOVERY_PATH_KEY, "lra-recovery-coordinator");
            // trigger a recovery scan which trigger a replay attempt on any participants
            // that have responded to complete/compensate requests with Response.Status.ACCEPTED
            resourcePath = recoveryTarget.path(recoveryPath).path("recovery");
            Response response2 = resourcePath
                    .request().get();

            checkStatusAndClose(response2, Response.Status.OK.getStatusCode(), false, resourcePath);
        }

        int countAfter = lraClient.getActiveLRAs().size();

        assertEquals(countBefore, countAfter, "joinAndEnd: wrong LRA count", resourcePath);
    }

    private void renewTimeLimit() {
        int[] cnt1 = {completedCount(true), completedCount(false)};
        Response response = null;

        try {
            WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH)
                    .path("renewTimeLimit");

            response = resourcePath
                    .request()
                    .get();

            checkStatusAndClose(response, -1, true, resourcePath);

            // check that participant was invoked
            int[] cnt2 = {completedCount(true), completedCount(false)};

            /*
             * The call to activities/timeLimit should have started an LRA whch should not have timed out
             * (because the called resource method renews the timeLimit before sleeping for longer than
              * the @TimeLimit annotation specifies).
             * Therefore the it should not have compensated:
             */
            assertEquals(cnt1[0] + 1, cnt2[0],
                    resourcePath.getUri().toString() + ": compensate was called instead of complete", resourcePath);
            assertEquals(cnt1[1], cnt2[1],
                    resourcePath.getUri().toString() + ": compensate should not have been called", resourcePath);
        } finally {
            if (response != null)
                response.close();
        }
    }

    private String checkStatusAndClose(Response response, int expected, boolean readEntity, WebTarget webTarget) {
        try {
            if (expected != -1 && response.getStatus() != expected) {
                if (webTarget != null) {
                    throw new WebApplicationException(webTarget.getUri().toString(), response);
                }

                throw new WebApplicationException(response);
            }

            if (readEntity) {
                return response.readEntity(String.class);
            }
        } finally {
            response.close();
        }

        return null;
    }

    private int completedCount(boolean completed) {
        Response response = null;
        String path = completed ? "completedactivitycount" : "compensatedactivitycount";

        try {
            WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path(path);

            response = resourcePath.request().get();

            assertEquals(Response.Status.OK.getStatusCode(),
                    response.getStatus(),
                    resourcePath.getUri().toString() + ": wrong status",
                    resourcePath);

            return Integer.parseInt(response.readEntity(String.class));
        } finally {
            if (response != null) {
                response.close();
            }
        }

    }

    private String multiLevelNestedActivity(CompletionType how, int nestedCnt) throws WebApplicationException {
        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path("multiLevelNestedActivity");

        int[] cnt1 = {completedCount(true), completedCount(false)};

        if (how == CompletionType.mixed && nestedCnt <= 1)
            how = CompletionType.complete;

        URL lra = lraClient.startLRA(null, "SpecTest#multiLevelNestedActivity", LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        String lraId = lra.toString();

        Response response = resourcePath
                .queryParam("nestedCnt", nestedCnt)
                .request()
                .header(LRAClient.LRA_HTTP_HEADER, lra)
                .put(Entity.text(""));

        String lraStr = checkStatusAndClose(response, Response.Status.OK.getStatusCode(), true, resourcePath);
        assert lraStr != null;
        String[] lraArray = lraStr.split(",");
        final List<LRAInfo> lras = lraClient.getActiveLRAs();
        URL[] urls = new URL[lraArray.length];

        IntStream.range(0, urls.length).forEach(i -> {
            try {
                urls[i] = new URL(lraArray[i]);
            } catch (MalformedURLException e) {
                fail(String.format("%s (multiLevelNestedActivity): returned an invalid URL: %s",
                        resourcePath.getUri().toString(), e.getMessage()));
            }
        });
        // check that the multiLevelNestedActivity method returned the mandatory LRA followed by two nested LRAs
        assertEquals(nestedCnt + 1, lraArray.length, "multiLevelNestedActivity: step 1", resourcePath);
        assertEquals(lraId, lraArray[0], "multiLevelNestedActivity: step 2", resourcePath); // first element should be the mandatory LRA

        // check that the coordinator knows about the two nested LRAs started by the multiLevelNestedActivity method
        // NB even though they should have completed they are held in memory pending the enclosing LRA finishing
        IntStream.rangeClosed(1, nestedCnt).forEach(i -> assertNotNull(getLra(lras, lraArray[i]),
                "missing nested LRA",
                resourcePath));

        // and the mandatory lra seen by the multiLevelNestedActivity method
        assertNotNull(getLra(lras, lraArray[0]), "lra should have been found", resourcePath);

        int[] cnt2 = {completedCount(true), completedCount(false)};

        // check that both nested activities were told to complete
        assertEquals(cnt1[0] + nestedCnt, cnt2[0], "multiLevelNestedActivity: step 3", resourcePath);
        // and that neither were told to compensate
        assertEquals(cnt1[1], cnt2[1], "multiLevelNestedActivity: step 4", resourcePath);

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
             * L2 is canceled  which causes C2 to compensate
             * L1 is closed which triggers the completion of C1
             *
             * To summarise:
             *
             * - C1 is completed
             * - C2 is completed and then compensated
             * - C3, ... are completed
             */
            lraClient.cancelLRA(urls[1]); // compensate the first nested LRA
            lraClient.closeLRA(lra); // should not complete any nested LRAs (since they have already completed via the interceptor)
        }

        // validate that the top level and nested LRAs are gone
        final List<LRAInfo> lras2 = lraClient.getActiveLRAs();

        IntStream.rangeClosed(0, nestedCnt).forEach(i -> assertNull(getLra(lras2, lraArray[i]),
                        "multiLevelNestedActivity: top level or nested activity still active", resourcePath));

        int[] cnt3 = {completedCount(true), completedCount(false)};

        if (how == CompletionType.complete) {
            // make sure that all nested activities were not told to complete or cancel a second time
            assertEquals(cnt2[0] + nestedCnt, cnt3[0], "multiLevelNestedActivity: step 5", resourcePath);
            // and that neither were still not told to compensate
            assertEquals(cnt1[1], cnt3[1], "multiLevelNestedActivity: step 6", resourcePath);

        } else if (how == CompletionType.compensate) {
            /*
             * the test starts LRA1 calls a @Mandatory method multiLevelNestedActivity which enlists in LRA1
             * multiLevelNestedActivity then calls an @Nested method which starts L2 and enlists another participant
             *   when the method returns the nested participant is completed (ie completed count is incremented)
             * Canceling L1 should then compensate the L1 enlistement (ie compensate count is incrememted)
             * which will then tell L2 to compenstate (ie the compensate count is incrememted again)
             */
            // each nested participant should have completed (the +nestedCnt)
            assertEquals(cnt1[0] + nestedCnt, cnt3[0], "multiLevelNestedActivity: step 7", resourcePath);
            // each nested participant should have compensated. The top level enlistement should have compensated (the +1)
            assertEquals(cnt2[1] + 1 + nestedCnt, cnt3[1], "multiLevelNestedActivity: step 8", resourcePath);
        } else {
            /*
             * The test is calling for a mixed uutcome:
             * - the top level LRA was closed
             * - one of the nested LRAs was compensated the rest should have been completed
             */
            // there should be just 1 compensation (the first nested LRA)
            assertEquals(1, cnt3[1] - cnt1[1], "multiLevelNestedActivity: step 9", resourcePath);
            /*
             * Expect nestedCnt + 1 completions, 1 for the top level and one for each nested LRA
             * (NB the first nested LRA is completed and compensated)
             * Note that the top level complete should not call complete again on the nested LRA
             */
            assertEquals(nestedCnt + 1, cnt3[0] - cnt1[0], "multiLevelNestedActivity: step 10", resourcePath);
        }

        return "passed";
    }

    private void cancelCheck(String path) {
        int[] cnt1 = {completedCount(true), completedCount(false)};
        URL lra = lraClient.startLRA(null, "SpecTest#" + path, LRA_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        Response response = null;

        WebTarget resourcePath = msTarget.path(ACTIVITIES_PATH).path(path);

        try {
            response = resourcePath
                    .request()
                    .header(LRAClient.LRA_HTTP_HEADER, lra)
                    .get();

            checkStatusAndClose(response, Response.Status.BAD_REQUEST.getStatusCode(), true, resourcePath);

            // check that participant was invoked
            int[] cnt2 = {completedCount(true), completedCount(false)};

            // check that complete was not called and that compensate was
            assertEquals(cnt1[0], cnt2[0], "complete was called instead of compensate", resourcePath);
            assertEquals(cnt1[1] + 1, cnt2[1], "compensate should have been called", resourcePath);

            try {
                assertTrue(!lraClient.isActiveLRA(lra), "cancelCheck: LRA should have been cancelled", resourcePath, lra);
            } catch (NotFoundException ignore) {
                // means the LRA has gone
            }
        } finally {
            if (response != null)
                response.close();
        }
    }

    static private LRAInfo getLra(List<LRAInfo> lras, String lraId) {
        for (LRAInfo lraInfo : lras) {
            if (lraInfo.getLraId().equals(lraId))
                return lraInfo;
        }

        return null;
    }

    static private void assertTrue(boolean condition, String reason, WebTarget target, URL lra) {
//        assert condition;

        if (!condition) {
            throw new GenericLRAException(lra, 0, target.getUri().toString() + ": " + reason, null);
        }
    }

    static private <T> void assertEquals(T expected, T actual, String reason, WebTarget target) {
//        assert expected.equals(actual);

        if (!expected.equals(actual)) {
            throw new GenericLRAException(null, 0, target.getUri().toString() + ": " + reason, null);
        }
    }
    static private void fail(String msg) {
        System.out.printf("%s%n", msg);
        assert false;
    }

    static private <T> void assertNotNull(T value, String reason, WebTarget target) {
//        assert value != null;
        if (value == null) {
            if (target == null)
                throw new GenericLRAException(null, 0, reason, null);
            else
                throw new GenericLRAException(null, 0, target.getUri().toString() + reason, null);
        }
    }

    static private <T> void assertNull(T value, String reason, WebTarget target) {
//        assert value == null;
        if (value != null) {
            if (target == null)
                throw new GenericLRAException(null, 0, reason, null);
            else
                throw new GenericLRAException(null, 0, target.getUri().toString() + reason, null);
        }    }
}
