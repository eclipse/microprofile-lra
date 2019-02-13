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
package org.eclipse.microprofile.lra.tck.participant.api;

import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.client.GenericLRAException;
import org.eclipse.microprofile.lra.client.InvalidLRAIdException;
import org.eclipse.microprofile.lra.client.LRAClient;
import org.eclipse.microprofile.lra.tck.participant.activity.Activity;
import org.eclipse.microprofile.lra.tck.participant.activity.ActivityStorage;
import org.eclipse.microprofile.lra.annotation.LRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Leave;
import org.eclipse.microprofile.lra.annotation.NestedLRA;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.client.IllegalLRAStateException;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.eclipse.microprofile.lra.client.LRAClient.LRA_HTTP_HEADER;
import static org.eclipse.microprofile.lra.client.LRAClient.LRA_HTTP_RECOVERY_HEADER;

@ApplicationScoped
@Path(LraController.LRA_CONTROLLER_PATH)
@LRA(value = LRA.Type.SUPPORTS, end = false)
public class LraController {
    public static final String LRA_CONTROLLER_PATH = "lracontroller";
    public static final String TRANSACTIONAL_WORK_PATH = "work";
    public static final String ACCEPT_WORK = "acceptWork";
    private static final Logger LOGGER = Logger.getLogger(LraController.class.getName());

    static final String MANDATORY_LRA_RESOURCE_PATH = "/mandatory";

    private static final String MISSING_LRA_DATA = "Missing LRA data";

    @Inject
    private LRAClient lraClient;

    private static final AtomicInteger COMPLETED_COUNT = new AtomicInteger(0);
    private static final AtomicInteger COMPENSATED_COUNT = new AtomicInteger(0);

    @Context
    private UriInfo context;

    @Inject
    private ActivityStorage activityStore;

    /**
     * Performing a GET on the participant URL will return the current status of the
     * participant {@link ParticipantStatus}, or 404 if the participant is no longer present.
     *
     * @param lraId the id of the LRA
     * @return the status of the LRA
     * @throws NotFoundException if the activity was not found
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(value = LRA.Type.NOT_SUPPORTED)
    public Response status(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        if (activity.getStatus() == null) {
            throw new IllegalLRAStateException(lraId, "getStatus", "LRA is not active");
        }

        if (activity.getAndDecrementAcceptCount() <= 0) {
            if (activity.getStatus() == ParticipantStatus.Completing) {
                activity.setStatus(ParticipantStatus.Completed);
            } else if (activity.getStatus() == ParticipantStatus.Compensating) {
                activity.setStatus(ParticipantStatus.Compensated);
            }
        }

        return Response.ok(activity.getStatus().name()).build();
    }

    /**
     * Test that participants can leave an LRA using the {@link LRAClient} programmatic API
     * @param lraUrl the id of the LRA
     * @param recoveryUrl header param defines recovery url
     * @return the url of the LRA if it was successfully removed
     * @throws NotFoundException if the activity was not found
     * @throws MalformedURLException if the LRA is malformed
     */
    @PUT
    @Path("/leave/{LraUrl}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response leaveWorkViaAPI(@PathParam("LraUrl")String lraUrl,
                                    @HeaderParam(LRA_HTTP_RECOVERY_HEADER) String recoveryUrl)
        throws NotFoundException, MalformedURLException {

        if (lraUrl != null && recoveryUrl != null) {
            lraClient.leaveLRA(new URL(recoveryUrl));

            activityStore.getActivityAndAssertExistence(lraUrl, context);

            activityStore.remove(lraUrl);

            return Response.ok(lraUrl).build();
        }

        return Response.ok("non transactional").build();
    }

    @PUT
    @Path("/leave")
    @Produces(MediaType.APPLICATION_JSON)
    @Leave
    public Response leaveWork(@HeaderParam(LRA_HTTP_HEADER) String lraId)
        throws NotFoundException {

        if (lraId != null) {
            activityStore.getActivityAndAssertExistence(lraId, context);

            activityStore.remove(lraId);

            return Response.ok(lraId).build();
        }

        return Response.ok("non transactional").build();
    }

    @PUT
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_HEADER) String lraId, String userData)
        throws NotFoundException {
        COMPLETED_COUNT.incrementAndGet();

        assertHeaderPresent(lraId); // the TCK expects the coordinator to invoke @Complete methods

        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(ParticipantStatus.Completing);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(),
                    LRA_CONTROLLER_PATH, lraId));

            return Response.accepted().location(URI.create(activity.getStatusUrl())).build();
        }

        activity.setStatus(ParticipantStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        LOGGER.info(String.format("LRA id '%s' was completed", lraId));
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate(timeLimit = 0, timeUnit = ChronoUnit.SECONDS)
    public Response compensateWork(@HeaderParam(LRA_HTTP_HEADER) String lraId, String userData)
        throws NotFoundException {

        assertHeaderPresent(lraId); // the TCK expects the coordinator to invoke @Compensate methods

        COMPENSATED_COUNT.incrementAndGet();

        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(ParticipantStatus.Compensating);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(),
                    LRA_CONTROLLER_PATH, lraId));

            return Response.accepted().location(URI.create(activity.getStatusUrl())).build();
        }

        activity.setStatus(ParticipantStatus.Compensated);
        activity.setStatusUrl(String.format("%s/%s/activity/compensated", context.getBaseUri(), lraId));

        LOGGER.info(String.format("LRA id '%s' was compensated", lraId));
        return Response.ok(activity.getStatusUrl()).build();
    }

    @DELETE
    @Path("/forget")
    @Produces(MediaType.APPLICATION_JSON)
    @Forget
    public Response forgetWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        COMPLETED_COUNT.incrementAndGet();

        assertHeaderPresent(lraId); // the TCK expects the coordinator to invoke @Forget methods

        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        if(activity == null) {
            throw new IllegalStateException(
                String.format("Activity store does not contain LRA id '%s' while it was invoked forget method at " + context.getPath()));
        }

        activityStore.remove(activity.getLraId());
        activity.setStatus(ParticipantStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        LOGGER.info(String.format("LRA id '%s' was forgotten", lraId));
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path(LraController.ACCEPT_WORK)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response acceptWork(
            @HeaderParam(LRA_HTTP_RECOVERY_HEADER) String recoveryId,
            @HeaderParam(LRA_HTTP_HEADER) String lraId) {

        assertHeaderPresent(lraId);

        Activity activity = storeActivity(lraId, recoveryId);

        activity.setAcceptedCount(1); // later tests that it is possible to asynchronously complete
        return Response.ok(lraId).build();
    }

    @PUT
    @Path("/supports")
    @LRA(value = LRA.Type.SUPPORTS, end = false)
    public Response supportsLRACall(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        storeActivity(lraId, null);

        return Response.ok(lraId).build();
    }

    @PUT
    @Path("/startViaApi")
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response subActivity(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertNotHeaderPresent(lraId);

        // manually start an LRA via the injection LRAClient api
        URL lra = lraClient.startLRA(null,"subActivity", 0L, ChronoUnit.SECONDS);

        lraId = lra.toString();

        storeActivity(lraId, null);

        // invoke a method that SUPPORTS LRAs. The filters should detect the LRA we just
        // started via the injected client
        // and add it as a header before calling the method at path /supports (ie supportsLRACall()).
        // The supportsLRACall method will return LRA id in the body if it is present.
        String id = restPutInvocation(lra,"supports", "");

        // check that the invoked method saw the LRA
        if (!lraId.equals(id)) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Entity.text("Unequal LRA ids")).build();
        }

        return Response.ok(id).build();
    }

    @PUT
    @Path(TRANSACTIONAL_WORK_PATH)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response activityWithLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String recoveryId,
                                    @HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        Activity activity = storeActivity(lraId, recoveryId);

        if (activity == null) {
            return Response.status(Response.Status.EXPECTATION_FAILED).entity(MISSING_LRA_DATA).build();
        }

        return Response.ok(lraId).header(LRA_HTTP_RECOVERY_HEADER, recoveryId).build();
    }

    private String restPutInvocation(URL lraURL, String path, String bodyText) {
        String id = null;
        Response response = ClientBuilder.newClient()
                .target(context.getBaseUri())
                .path(LRA_CONTROLLER_PATH)
                .path(path)
                .request()
                .header(LRAClient.LRA_HTTP_HEADER, lraURL)
                .put(Entity.text(bodyText));

        if (response.hasEntity()) {
            id = response.readEntity(String.class);
        }

        try {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new WebApplicationException("Error on REST PUT for LRA '" + lraURL
                        + "' at path '" + path + "' and body '" + bodyText + "'", response);
            }
        } finally {
            response.close();
        }

        return id;
    }

    @PUT
    @Path(MANDATORY_LRA_RESOURCE_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response activityWithMandatoryLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String recoveryId,
                                             @HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return activityWithLRA(recoveryId, lraId);
    }

    @PUT
    @Path("/nestedActivity")
    @LRA(value = LRA.Type.MANDATORY, end = true)
    @NestedLRA
    public Response nestedActivity(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String recoveryId,
                                   @HeaderParam(LRA_HTTP_HEADER) String nestedLRAId) {
        assertHeaderPresent(nestedLRAId);

        storeActivity(nestedLRAId, recoveryId);

        return Response.ok(nestedLRAId).build();
    }

    @PUT
    @Path("/multiLevelNestedActivity")
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response multiLevelNestedActivity(
            @HeaderParam(LRA_HTTP_RECOVERY_HEADER) String recoveryId,
            @HeaderParam(LRA_HTTP_HEADER) String nestedLRAId,
            @QueryParam("nestedCnt") @DefaultValue("1") Integer nestedCnt) {
        assertHeaderPresent(nestedLRAId);

        storeActivity(nestedLRAId, recoveryId);

        URL lraURL;

        try {
            lraURL = new URL(URLDecoder.decode(nestedLRAId, "UTF-8"));
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new InvalidLRAIdException(nestedLRAId, e.getMessage(), e);
        }

        // invoke resources that enlist nested LRAs
        String[] lras = new String[nestedCnt + 1];
        lras[0] = nestedLRAId;
        IntStream.range(1, lras.length).forEach(i -> lras[i] = restPutInvocation(lraURL,"nestedActivity", ""));

        return Response.ok(String.join(",", lras)).build();
    }

    private Activity storeActivity(String lraId, String recoveryId) {
        LOGGER.fine(String.format("Storing information about LRA id '%s' and recoveryId '%s'", lraId, recoveryId));

        Activity activity = new Activity(lraId)
            .setRecoveryUrl(recoveryId)
            .setStatus(null);

        return activityStore.add(activity);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response findAll() {
        List<Activity> results = activityStore.findAll();

        return Response.ok(results.size()).build();
    }

    @GET
    @Path("/completedactivitycount")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response getCompletedCount() {
        return Response.ok(COMPLETED_COUNT.get()).build();
    }

    @GET
    @Path("/compensatedactivitycount")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response getCompensatedCount() {
        return Response.ok(COMPENSATED_COUNT.get()).build();
    }

    @GET
    @Path("/cancelOn")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, cancelOn = {Response.Status.NOT_FOUND, Response.Status.BAD_REQUEST})
    public Response cancelOn(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        activityStore.add(new Activity(lraId));

        return Response.status(Response.Status.BAD_REQUEST).entity(Entity.text("Simulate buisiness logic failure")).build();
    }

    @GET
    @Path("/cancelOnFamily")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, cancelOnFamily = {Response.Status.Family.CLIENT_ERROR})
    public Response cancelOnFamily(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        activityStore.add(new Activity(lraId));

        return Response.status(Response.Status.BAD_REQUEST).entity(Entity.text("Simulate buisiness logic failure")).build();
    }

    @GET
    @Path("/timeLimit")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, timeLimit = 100, timeUnit = ChronoUnit.MILLIS)
    public Response timeLimit(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        activityStore.add(new Activity(lraId));

        try {
            Thread.sleep(300); // sleep for longer than specified in the timeLimit annotation attribute
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted becaused time limit elapsed", e);
        }
        return Response.status(Response.Status.OK).entity(Entity.text("Simulate buisiness logic timeoout")).build();
    }

    @GET
    @Path("/renewTimeLimit")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, end = false, timeLimit = 100, timeUnit = ChronoUnit.MILLIS)
    public Response extendTimeLimit(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        activityStore.add(new Activity(lraId));

        try {
            /*
             * the incoming LRA was created with a timeLimit of 100 ms via the timeLimit annotation
             * attribute update the timeLimit to 300 sleep for 200 return from the method so the LRA will
             * have been running for 200 ms so it should not be cancelled
             */
            lraClient.renewTimeLimit(lraToURL(lraId, "Invalid LRA id"), 300, ChronoUnit.MILLIS);
            // sleep for 200000 micro seconds (should be longer than specified in the timeLimit annotation attribute)
            Thread.sleep(200);
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted because the renewed time limit elapsed", e);
        }
        return Response.status(Response.Status.OK).entity(Entity.text("Simulate buisiness logic timeoout")).build();
    }

    /**
     * Performing a PUT on "participant URL"/compensate will cause the participant to compensate
     * the work that was done within the scope of the transaction.
     *
     * The participant will either return a 200 OK code and a "status URL" which indicates
     * the outcome and which can be probed (via GET) and will simply return the same
     * (implicit) information:
     *
     * "URL"/cannot-compensate
     * "URL"/cannot-complete
     *
     * @param lraId the id of the LRA
     * @return the final status of the activity
     * @throws NotFoundException if the activity does not exist
     */
    @PUT
    @Path("/{LRAId}/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response compensate(@PathParam("LRAId") String lraId) throws NotFoundException {
        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activity.setStatus(ParticipantStatus.Compensated);
        activity.setStatusUrl(String.format("%s/%s/activity/compensated", context.getBaseUri(), lraId));

        return Response.ok(activity.getStatusUrl()).build();
    }

    /**
     * Performing a PUT on "participant URL"/complete will cause the participant to tidy up
     * and it can forget this transaction.
     *
     * The participant will either return a 200 OK code and a "status URL" which indicates
     * the outcome and which can be probed (via GET)
     * and will simply return the same (implicit) information:
     * "URL"/cannot-compensate
     * "URL"/cannot-complete
     *
     * @param lraId the id of the LRA
     * @return the final status of the activity
     * @throws NotFoundException if the activity does not exist
     */
    @PUT
    @Path("/{LRAId}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response complete(@PathParam("LRAId") String lraId) throws NotFoundException {
        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activity.setStatus(ParticipantStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path("/{LRAId}/forget")
    public void forget(@PathParam("LRAId")String lraId) throws NotFoundException {
        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activityStore.remove(activity.getLraId());
    }

    @GET
    @Path("/{LRAId}/completed")
    @Produces(MediaType.APPLICATION_JSON)
    public String completedStatus(@PathParam("LRAId") String lraId) {
        return ParticipantStatus.Completed.name();
    }

    @GET
    @Path("/{LRAId}/compensated")
    @Produces(MediaType.APPLICATION_JSON)
    public String compensatedStatus(@PathParam("LRAId") String lraId) {
        return ParticipantStatus.Compensated.name();
    }

    private static URL lraToURL(String lraId, String errorMessage) {
        try {
            return new URL(lraId);
        } catch (MalformedURLException e) {
            LOGGER.log(Level.WARNING, "Can't construct URL from LRA id '" + lraId + "'", e);
            throw new GenericLRAException(null, Response.Status.BAD_REQUEST.getStatusCode(),
                    errorMessage + ": LRA id '" + lraId + "'", e);
        }
    }

    private void assertHeaderPresent(String lraId) {
        if (lraId == null) {
            throw new InvalidLRAIdException(null,
                    String.format("%s: missing '%s' header", context.getPath(), LRA_HTTP_HEADER));
        }
    }

    private void assertNotHeaderPresent(String lraId) {
        if (lraId != null) {
            throw new InvalidLRAIdException(null,
                    String.format("%s: unexpected '%s' header", context.getPath(), LRA_HTTP_HEADER));
        }
    }
}
