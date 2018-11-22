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
import org.eclipse.microprofile.lra.tck.participant.service.ActivityService;
import org.eclipse.microprofile.lra.annotation.LRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Leave;
import org.eclipse.microprofile.lra.annotation.NestedLRA;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.TimeLimit;
import org.eclipse.microprofile.lra.client.IllegalLRAStateException;
import org.eclipse.microprofile.lra.tck.participant.model.Activity;
import org.eclipse.microprofile.lra.annotation.CompensatorStatus;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static org.eclipse.microprofile.lra.client.LRAClient.LRA_HTTP_HEADER;
import static org.eclipse.microprofile.lra.client.LRAClient.LRA_HTTP_RECOVERY_HEADER;

@ApplicationScoped
@Path(ActivityController.ACTIVITIES_PATH)
@LRA(LRA.Type.SUPPORTS)
public class ActivityController {
    public static final String ACTIVITIES_PATH = "activities";
    public static final String ACCEPT_WORK = "acceptWork";
    private static final Logger LOGGER = Logger.getLogger(ActivityController.class.getName());

    @Inject
    private LRAClient lraClient;

    private static final AtomicInteger COMPLETED_COUNT = new AtomicInteger(0);
    private static final AtomicInteger COMPENSATED_COUNT = new AtomicInteger(0);

    @Context
    private UriInfo context;

    @Inject
    private ActivityService activityService;

    /**
     * Performing a GET on the participant URL will return the current status of the
     * participant {@link CompensatorStatus}, or 404 if the participant is no longer present.
     *
     * @param lraId the id of the LRA
     * @return the status of the LRA
     * @throws NotFoundException if the activity was not found
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response status(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        Activity activity = activityService.getActivity(lraId);

        if (activity.getStatus() == null) {
            throw new IllegalLRAStateException(lraId, "getStatus", "LRA is not active");
        }

        if (activity.getAndDecrementAcceptCount() <= 0) {
            if (activity.getStatus() == CompensatorStatus.Completing) {
                activity.setStatus(CompensatorStatus.Completed);
            }
            else if (activity.getStatus() == CompensatorStatus.Compensating) {
                activity.setStatus(CompensatorStatus.Compensated);
            }
        }

        return Response.ok(activity.getStatus().name()).build();
    }

    /**
     * Test that participants can leave an LRA using the {@link LRAClient} programmatic API
     * @param lraUrl the id of the LRA
     * @return the url of the LRA if it was successfully removed
     * @throws NotFoundException if the activity was not found
     * @throws MalformedURLException if the LRA is malformed
     */
    @PUT
    @Path("/leave/{LraUrl}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response leaveWorkViaAPI(@PathParam("LraUrl")String lraUrl)
        throws NotFoundException, MalformedURLException {

        if (lraUrl != null) {
            // TODO this encoding of LRA URIs will be Narayana specific
            Map<String, String> terminateURIs =
                Util.getTerminationUris(this.getClass(), context.getBaseUri());
            lraClient.leaveLRA(new URL(lraUrl), terminateURIs.get("Link"));

            activityService.getActivity(lraUrl);

            activityService.remove(lraUrl);

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
            activityService.getActivity(lraId);

            activityService.remove(lraId);

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

        Activity activity = activityService.getActivity(lraId);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(CompensatorStatus.Completing);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(),
                    ACTIVITIES_PATH, lraId));

            return Response.accepted().location(URI.create(activity.getStatusUrl())).build();
        }

        activity.setStatus(CompensatorStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        System.out.printf("ActivityController completing %s%n", lraId);
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_HEADER) String lraId, String userData)
        throws NotFoundException {

        assertHeaderPresent(lraId); // the TCK expects the coordinator to invoke @Compensate methods

        COMPENSATED_COUNT.incrementAndGet();

        Activity activity = activityService.getActivity(lraId);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(CompensatorStatus.Compensating);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(),
                    ACTIVITIES_PATH, lraId));

            return Response.accepted().location(URI.create(activity.getStatusUrl())).build();
        }

        activity.setStatus(CompensatorStatus.Compensated);
        activity.setStatusUrl(String.format("%s/%s/activity/compensated", context.getBaseUri(), lraId));

        System.out.printf("ActivityController compensating %s%n", lraId);
        return Response.ok(activity.getStatusUrl()).build();
    }

    @DELETE
    @Path("/forget")
    @Produces(MediaType.APPLICATION_JSON)
    @Forget
    public Response forgetWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        COMPLETED_COUNT.incrementAndGet();

        assertHeaderPresent(lraId); // the TCK expects the coordinator to invoke @Forget methods

        Activity activity = activityService.getActivity(lraId);

        activityService.remove(activity.getId());
        activity.setStatus(CompensatorStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        System.out.printf("ActivityController forgetting %s%n", lraId);
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path(ActivityController.ACCEPT_WORK)
    @LRA(LRA.Type.REQUIRED)
    public Response acceptWork(
            @HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
            @HeaderParam(LRA_HTTP_HEADER) String lraId) {

        assertHeaderPresent(lraId);

        Activity activity = addWork(lraId, rcvId);

        if (activity == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        activity.setAcceptedCount(1); // tests that it is possible to asynchronously complete
        return Response.ok(lraId).build();
    }

    @PUT
    @Path("/supports")
    @LRA(LRA.Type.SUPPORTS)
    public Response supportsLRACall(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        addWork(lraId, null);

        return Response.ok(lraId).build();
    }

    @PUT
    @Path("/startViaApi")
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response subActivity(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertNotHeaderPresent(lraId);

        // manually start an LRA via the injection LRAClient api
        URL lra = lraClient.startLRA(null,"subActivity", 0L, TimeUnit.SECONDS);

        lraId = lra.toString();

        addWork(lraId, null);

        // invoke a method that SUPPORTS LRAs. The filters should detect the LRA we just
        // started via the injected client
        // and add it as a header before calling the method at path /supports (ie supportsLRACall()).
        // The supportsLRACall method will return LRA id in the body if it is present.
        String id = restPutInvocation(lra,"supports", "");

        // check that the invoked method saw the LRA
        if (id == null || !lraId.equals(id))
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Entity.text("Unequal LRA ids")).build();

        return Response.ok(id).build();
    }

    @PUT
    @Path("/work")
    @LRA(LRA.Type.REQUIRED)
    public Response activityWithLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
                                    @HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        Activity activity = addWork(lraId, rcvId);

        if (activity == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        return Response.ok(lraId).build();
    }

    private String restPutInvocation(URL lraURL, String path, String bodyText) {
        String id = null;
        Response response = ClientBuilder.newClient()
                .target(context.getBaseUri())
                .path("activities")
                .path(path)
                .request()
                .header(LRAClient.LRA_HTTP_HEADER, lraURL)
                .put(Entity.text(bodyText));

        if (response.hasEntity())
            id = response.readEntity(String.class);

        checkStatusAndClose(response, Response.Status.OK.getStatusCode());

        return id;
    }

    @PUT
    @Path("/nestedActivity")
    @LRA(LRA.Type.MANDATORY)
    @NestedLRA
    public Response nestedActivity(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
                                   @HeaderParam(LRA_HTTP_HEADER) String nestedLRAId) {
        assertHeaderPresent(nestedLRAId);

        Activity activity = addWork(nestedLRAId, rcvId);

        if (activity == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        return Response.ok(nestedLRAId).build();
    }

    @PUT
    @Path("/multiLevelNestedActivity")
    @LRA(LRA.Type.MANDATORY)
    public Response multiLevelNestedActivity(
            @HeaderParam(LRA_HTTP_RECOVERY_HEADER) String rcvId,
            @HeaderParam(LRA_HTTP_HEADER) String nestedLRAId,
            @QueryParam("nestedCnt") @DefaultValue("1") Integer nestedCnt) {
        assertHeaderPresent(nestedLRAId);

        Activity activity = addWork(nestedLRAId, rcvId);

        if (activity == null)
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();

        URL lraURL;

        try {
            lraURL = new URL(URLDecoder.decode(nestedLRAId, "UTF-8"));
        }
        catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new InvalidLRAIdException(nestedLRAId, e.getMessage(), e);
        }

        // invoke resources that enlist nested LRAs
        String[] lras = new String[nestedCnt + 1];
        lras[0] = nestedLRAId;
        IntStream.range(1, lras.length).forEach(i -> lras[i] = restPutInvocation(lraURL,"nestedActivity", ""));

        return Response.ok(String.join(",", lras)).build();
    }

    private Activity addWork(String lraId, String rcvId) {
        System.out.printf("ActivityController: work id %s and rcvId %s %n", lraId, rcvId);

        try {
            return activityService.getActivity(lraId);
        }
        catch (NotFoundException e) {
            Activity activity = new Activity(lraId);

            activity.setRcvUrl(rcvId);
            activity.setStatus(null);

            activityService.add(activity);

            return activity;
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response findAll() {
        List<Activity> results = activityService.findAll();

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

        activityService.add(new Activity(lraId));

        return Response.status(Response.Status.BAD_REQUEST).entity(Entity.text("Simulate buisiness logic failure")).build();
    }

    @GET
    @Path("/cancelOnFamily")
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.REQUIRED, cancelOnFamily = {Response.Status.Family.CLIENT_ERROR})
    public Response cancelOnFamily(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        activityService.add(new Activity(lraId));

        return Response.status(Response.Status.BAD_REQUEST).entity(Entity.text("Simulate buisiness logic failure")).build();
    }

    @GET
    @Path("/timeLimit")
    @Produces(MediaType.APPLICATION_JSON)
    @TimeLimit(limit = 100, unit = TimeUnit.MILLISECONDS)
    @LRA(value = LRA.Type.REQUIRED)
    public Response timeLimit(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        activityService.add(new Activity(lraId));

        try {
            Thread.sleep(300); // sleep for 200 miliseconds (should be longer than specified in the @TimeLimit annotation)
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Response.status(Response.Status.OK).entity(Entity.text("Simulate buisiness logic timeoout")).build();
    }

    @GET
    @Path("/renewTimeLimit")
    @Produces(MediaType.APPLICATION_JSON)
    @TimeLimit(limit = 100, unit = TimeUnit.MILLISECONDS)
    @LRA(value = LRA.Type.REQUIRED)
    public Response extendTimeLimit(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        assertHeaderPresent(lraId);

        activityService.add(new Activity(lraId));

        try {
            /*
             * the incomming LRA was created with a timeLimit of 100 ms via the @TimeLimit annotation
             * update the timeLimit to 300 sleep for 200 return from the method so the LRA will
             * have been running for 200 ms so it should not be cancelled
             */
            lraClient.renewTimeLimit(lraToURL(lraId, "Invalid LRA id"), 300, TimeUnit.MILLISECONDS);
            // sleep for 200000 micro seconds (should be longer than specified in the @TimeLimit annotation)
            Thread.sleep(200);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
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
     * @param txId the id of the LRA
     * @return the final status of the activity
     * @throws NotFoundException if the activity does not exist
     */
    @PUT
    @Path("/{TxId}/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    public Response compensate(@PathParam("TxId")String txId) throws NotFoundException {
        Activity activity = activityService.getActivity(txId);

        activity.setStatus(CompensatorStatus.Compensated);
        activity.setStatusUrl(String.format("%s/%s/activity/compensated", context.getBaseUri(), txId));

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
     * @param txId the id of the LRA
     * @return the final status of the activity
     * @throws NotFoundException if the activity does not exist
     */
    @PUT
    @Path("/{TxId}/complete")
    @Produces(MediaType.APPLICATION_JSON)
    public Response complete(@PathParam("TxId")String txId) throws NotFoundException {
        Activity activity = activityService.getActivity(txId);

        activity.setStatus(CompensatorStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), txId));

        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path("/{TxId}/forget")
    public void forget(@PathParam("TxId")String txId) throws NotFoundException {
        Activity activity = activityService.getActivity(txId);

        activityService.remove(activity.getId());
    }

    @GET
    @Path("/{TxId}/completed")
    @Produces(MediaType.APPLICATION_JSON)
    public String completedStatus(@PathParam("TxId")String txId) {
        return CompensatorStatus.Completed.name();
    }

    @GET
    @Path("/{TxId}/compensated")
    @Produces(MediaType.APPLICATION_JSON)
    public String compensatedStatus(@PathParam("TxId")String txId) {
        return CompensatorStatus.Compensated.name();
    }

    private void checkStatusAndClose(Response response, int expected) {
        try {
            if (response.getStatus() != expected)
                throw new WebApplicationException(response);
        }
        finally {
            response.close();
        }
    }

    private static URL lraToURL(String lraId, String errorMessage) {
        try {
            return new URL(lraId);
        }
        catch (MalformedURLException e) {
            LOGGER.log(Level.WARNING, "Can't construct URL from LRA id " + lraId, e);
            throw new GenericLRAException(null, Response.Status.BAD_REQUEST.getStatusCode(),
                    errorMessage + ": " + lraId, e);
        }
    }

    private void assertHeaderPresent(String lraId) {
        // assert (lraId != null) : context.getPath() + ": missing " + LRA_HTTP_HEADER + " header";

        if (lraId == null) {
            throw new InvalidLRAIdException(null,
                    String.format("%s: missing %s header", context.getPath(), LRA_HTTP_HEADER), null);
        }
    }


    private void assertNotHeaderPresent(String lraId) {
        // assert (lraId == null) : context.getPath() + ": unexpected " + LRA_HTTP_HEADER + " header";

        if (lraId != null) {
            throw new InvalidLRAIdException(null,
                    String.format("%s: unexpected %s header", context.getPath(), LRA_HTTP_HEADER), null);
        }
    }
}
