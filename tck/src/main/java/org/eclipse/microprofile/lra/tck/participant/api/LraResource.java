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
import org.eclipse.microprofile.lra.tck.LRAClientOps;
import org.eclipse.microprofile.lra.tck.LraTckConfigBean;
import org.eclipse.microprofile.lra.tck.participant.activity.Activity;
import org.eclipse.microprofile.lra.tck.participant.activity.ActivityStorage;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.Leave;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.eclipse.microprofile.lra.tck.service.LRATestService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_RECOVERY_HEADER;

@ApplicationScoped
@Path(LraResource.LRA_RESOURCE_PATH)
@LRA(value = LRA.Type.SUPPORTS, end = false)
public class LraResource {
    public static final String LRA_RESOURCE_PATH = "lraresource";
    public static final String TRANSACTIONAL_WORK_PATH = "work";
    public static final String ACCEPT_WORK = "acceptWork";
    public static final String TIME_LIMIT = "/timeLimit";
    public static final String TIME_LIMIT_HALF_SEC = "/timeLimit2";
    public static final String CANCEL_PATH = "/cancel";
    static final String MANDATORY_LRA_RESOURCE_PATH = "/mandatory";

    private static final Logger LOGGER = Logger.getLogger(LraResource.class.getName());

    private static final String MISSING_LRA_DATA = "Missing LRA data";

    @Context
    private UriInfo context;

    @Inject
    private ActivityStorage activityStore;

    @Inject
    private LRAMetricService lraMetricService;

    @Inject
    private LraTckConfigBean configBean;

    @Inject
    LRATestService lraTestService;
    /**
     * Performing a GET on the participant URL will return the current status of the
     * participant {@link ParticipantStatus}, or 410 if the participant does no longer know about this LRA.
     *
     * @param lraId the id of the LRA
     * @param recoveryId the recovery id of this enlistment
     * @return the status of the LRA
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    public Response status(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                           @HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId) {

        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER); // the TCK expects the implementation to invoke @Status methods
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER); // the TCK expects the implementation to invoke @Status methods

        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        if (activity.getStatus() == null) {
            throw new IllegalLRAStateException(lraId, "LRA is not active");
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

    @PUT
    @Path("/leave")
    @Produces(MediaType.APPLICATION_JSON)
    @Leave
    public Response leaveWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

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
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                                 @HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
                                 String userData) {
        lraMetricService.incrementMetric(LRAMetricType.Completed, lraId, LraResource.class.getName());

        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER); // the TCK expects the implementation to invoke @Complete methods
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER); // the TCK expects the implementation to invoke @Complete methods

        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(ParticipantStatus.Completing);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(),
                    LRA_RESOURCE_PATH, lraId));

            return Response.accepted().location(URI.create(activity.getStatusUrl())).build();
        }

        activity.setStatus(ParticipantStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId.toASCIIString()));

        LOGGER.info(String.format("LRA id '%s' was completed", lraId.toASCIIString()));
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                                   @HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
                                   String userData) {

        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER); // the TCK expects the implementation to invoke @Compensate methods
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER); // the TCK expects the implementation to invoke @Compensate methods

        lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId, LraResource.class.getName());

        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(ParticipantStatus.Compensating);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(),
                    LRA_RESOURCE_PATH, lraId));

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
    public Response forgetWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
                               @HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId) {
        lraMetricService.incrementMetric(LRAMetricType.Forget, lraId, LraResource.class.getName());

        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER); // the TCK expects the implementation to invoke @Forget methods
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER); // the TCK expects the implementation to invoke @Forget methods

        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        if (activity == null) {
            throw new IllegalStateException(
                String.format("Activity store does not contain LRA id '%s' while it was invoked forget method at ", context.getPath()));
        }

        activityStore.remove(activity.getLraId());
        activity.setStatus(ParticipantStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        LOGGER.info(String.format("LRA id '%s' was forgotten", lraId.toASCIIString()));
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path(LraResource.ACCEPT_WORK)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response acceptWork(
            @HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER);
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER);

        Activity activity = storeActivity(lraId, recoveryId);

        activity.setAcceptedCount(1); // later tests that it is possible to asynchronously complete
        return Response.ok(lraId).build();
    }

    @PUT
    @Path("/supports")
    @LRA(value = LRA.Type.SUPPORTS, end = false)
    public Response supportsLRACall(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER);

        storeActivity(lraId, null);

        return Response.ok(lraId).build();
    }

    @PUT
    @Path("/startViaApi")
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response subActivity(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        assertNotHeaderPresent(lraId);

        Client client = null;

        try {
            client = ClientBuilder.newClient();
            WebTarget target = client.target(context.getBaseUri());
            URI lra = new LRAClientOps(target).startLRA(null,"subActivity", 0L, ChronoUnit.SECONDS);

            lraId = lra;

            storeActivity(lraId, null);

            // invoke a method that SUPPORTS LRAs. The filters should detect the LRA we just started
            // and add it as a header before calling the method at path /supports (ie supportsLRACall()).
            // The supportsLRACall method will return LRA id in the body if it is present.
            String id = restPutInvocation(lra,"supports", "");

            // check that the invoked method saw the LRA
            if (!lraId.toASCIIString().equals(id)) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Entity.text("Unequal LRA ids")).build();
            }

            return Response.ok(id).build();
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    @PUT
    @Path(TRANSACTIONAL_WORK_PATH)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response activityWithLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
                                    @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER);
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER);

        Activity activity = storeActivity(lraId, recoveryId);

        if (activity == null) {
            return Response.status(Response.Status.EXPECTATION_FAILED).entity(MISSING_LRA_DATA).build();
        }

        return Response.ok(lraId).header(LRA_HTTP_RECOVERY_HEADER, recoveryId).build();
    }

    private String restPutInvocation(URI lraURI, String path, String bodyText) {
        String id = null;
        Response response = ClientBuilder.newClient()
                .target(context.getBaseUri())
                .path(LRA_RESOURCE_PATH)
                .path(path)
                .request()
                .header(LRA_HTTP_CONTEXT_HEADER, lraURI)
                .put(Entity.text(bodyText));

        if (response.hasEntity()) {
            id = response.readEntity(String.class);
        }

        try {
            if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                throw new WebApplicationException("Error on REST PUT for LRA '" + lraURI
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
    public Response activityWithMandatoryLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
                                             @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return activityWithLRA(recoveryId, lraId);
    }

    @PUT
    @Path("/nestedActivity")
    @LRA(value = LRA.Type.NESTED, end = true)
    public Response nestedActivity(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
                                   @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI nestedLRAId) {
        assertHeaderPresent(nestedLRAId, LRA_HTTP_CONTEXT_HEADER);
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER);

        storeActivity(nestedLRAId, recoveryId);

        return Response.ok(nestedLRAId).build();
    }

    /**
     * Used to close nested LRA in which this resource is enlisted
     *
     * @see org.eclipse.microprofile.lra.tck.TckTests#mixedMultiLevelNestedActivity
     */
    @PUT
    @Path(CANCEL_PATH)
    @LRA(value = LRA.Type.MANDATORY,
        cancelOnFamily = Response.Status.Family.SERVER_ERROR)
    public Response cancelLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.status(500).entity(lraId).build();
    }

    @PUT
    @Path("/multiLevelNestedActivity")
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response multiLevelNestedActivity(
            @HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI nestedLRAId,
            @QueryParam("nestedCnt") @DefaultValue("1") Integer nestedCnt) {
        assertHeaderPresent(nestedLRAId, LRA_HTTP_CONTEXT_HEADER);
        assertHeaderPresent(recoveryId, LRA_HTTP_RECOVERY_HEADER);

        storeActivity(nestedLRAId, recoveryId);

        // invoke resources that enlist nested LRAs
        String[] lras = new String[nestedCnt + 1];
        lras[0] = nestedLRAId.toASCIIString();
        IntStream.range(1, lras.length).forEach(i -> lras[i] = restPutInvocation(nestedLRAId,"nestedActivity", ""));

        return Response.ok(String.join(",", lras)).build();
    }

    private Activity storeActivity(URI lraId, URI recoveryId) {
        String lra = lraId != null ? lraId.toASCIIString() : null; // already asserted by the caller but check anyway
        String rid = recoveryId != null ? recoveryId.toASCIIString() : null; // not asserted by the call so check for null
        LOGGER.fine(String.format("Storing information about LRA id '%s' and recoveryId '%s'", lra, rid));

        Activity activity = new Activity(lraId)
            .setRecoveryUri(recoveryId)
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
    @Path(TIME_LIMIT)
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, timeLimit = 500, timeUnit = ChronoUnit.MILLIS)
    public Response timeLimit(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER);

        activityStore.add(new Activity(lraId));

        try {
            // sleep longer time than specified in the attribute 'timeLimit'
            Thread.sleep(configBean.adjustTimeout(1000));
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted because time limit elapsed", e);
        }
        return Response.status(Response.Status.OK).entity(lraId.toASCIIString()).build();
    }

    /**
     * @see org.eclipse.microprofile.lra.tck.TckTests#timeLimitWithPreConditionFailed
     */
    @GET
    @Path(TIME_LIMIT_HALF_SEC)
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, timeLimit = 500, timeUnit = ChronoUnit.MILLIS)
    public Response timeLimitTest2(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        assertHeaderPresent(lraId, LRA_HTTP_CONTEXT_HEADER);

        activityStore.add(new Activity(lraId));

        try {
            Thread.sleep(configBean.adjustTimeout(1000)); // sleep for longer than specified in the timeLimit annotation attribute
            // force the implementation to notice that the LRA should have timed out
            lraTestService.waitForCallbacks(lraId);
            // the next request should fail with a 412 code since the LRA should no longer be active
            restPutInvocation(lraId, MANDATORY_LRA_RESOURCE_PATH, "");
        } catch (WebApplicationException wae) {
            return Response.status(wae.getResponse().getStatus()).build();
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, "timeLimitTest2: Interrupted because time limit elapsed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.OK).entity(lraId.toASCIIString()).build();
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
     */
    @PUT
    @Path("/{LRAId}/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response compensate(@PathParam("LRAId") URI lraId) {
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
     */
    @PUT
    @Path("/{LRAId}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response complete(@PathParam("LRAId") URI lraId) {
        Activity activity = activityStore.getActivityAndAssertExistence(lraId, context);

        activity.setStatus(ParticipantStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path("/{LRAId}/forget")
    public void forget(@PathParam("LRAId") URI lraId) {
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

    private void assertHeaderPresent(URI lraId, String headerName) {
        if (lraId == null) {
            throw new WrongHeaderException(String.format("%s: missing '%s' header", context.getPath(), headerName));
        }
    }

    private void assertNotHeaderPresent(URI lraId) {
        if (lraId != null) {
            throw new WrongHeaderException(String.format("%s: unexpected '%s' header", context.getPath(), LRA_HTTP_CONTEXT_HEADER));
        }
    }
}
