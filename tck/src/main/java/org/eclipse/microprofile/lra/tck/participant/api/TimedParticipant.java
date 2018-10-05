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

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.CompensatorStatus;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.LRA;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.TimeLimit;
import org.eclipse.microprofile.lra.client.IllegalLRAStateException;
import org.eclipse.microprofile.lra.client.LRAClient;
import org.eclipse.microprofile.lra.tck.participant.model.Activity;
import org.eclipse.microprofile.lra.tck.participant.service.ActivityService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@ApplicationScoped
@Path(TimedParticipant.ACTIVITIES_PATH2)
@LRA(LRA.Type.SUPPORTS)
public class TimedParticipant {
    public static final String ACTIVITIES_PATH2 = "timedactivities";
    public static final String ACCEPT_WORK = "/acceptWork";
    public static final String TIME_LIMIT_SUPPORTS_LRA = "/timeLimitSupportsLRA";

    private static final AtomicInteger COMPLETED_COUNT = new AtomicInteger(0);
    private static final AtomicInteger COMPENSATED_COUNT = new AtomicInteger(0);

    @Inject
    private LRAClient lraClient;

    @Context
    private UriInfo context;

    @Inject
    private ActivityService activityService;

    /*
     Performing a GET on the participant URL will return the current status of the participant
     {@link CompensatorStatus}, or 404 if the participant is no longer present.
     */
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    @Status
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response status(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        Activity activity = activityService.getActivity(lraId);

        if (activity.getStatus() == null) {
            throw new IllegalLRAStateException(lraId, "LRA is not active", "getStatus");
        }

        if (activity.getAndDecrementAcceptCount() <= 0) {
            if (activity.getStatus() == CompensatorStatus.Completing) {
                activity.setStatus(CompensatorStatus.Completed);
            } else if (activity.getStatus() == CompensatorStatus.Compensating) {
                activity.setStatus(CompensatorStatus.Compensated);
            }
        }

        return Response.ok(activity.getStatus().name()).build();
    }

    @PUT
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    @TimeLimit(limit = 100, unit = TimeUnit.MILLISECONDS)
    public Response completeWork(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId, String userData) throws NotFoundException {
        COMPLETED_COUNT.incrementAndGet();

        assert lraId != null;

        Activity activity = activityService.getActivity(lraId);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(CompensatorStatus.Completing);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(), ACTIVITIES_PATH2, lraId));

            return Response.accepted().location(URI.create(activity.getStatusUrl())).build();
        }

        activity.setStatus(CompensatorStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        endCheck(activity);

        System.out.printf("ActivityController completing %s%n", lraId);
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    @TimeLimit(limit = 100, unit = TimeUnit.MILLISECONDS)
    public Response compensateWork(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId, String userData) throws NotFoundException {
        COMPENSATED_COUNT.incrementAndGet();

        assert lraId != null;

        Activity activity = activityService.getActivity(lraId);

        activity.setEndData(userData);

        if (activity.getAndDecrementAcceptCount() > 0) {
            activity.setStatus(CompensatorStatus.Compensating);
            activity.setStatusUrl(String.format("%s/%s/%s/status", context.getBaseUri(), ACTIVITIES_PATH2, lraId));

            return Response.accepted().location(URI.create(activity.getStatusUrl())).build();
        }

        activity.setStatus(CompensatorStatus.Compensated);
        activity.setStatusUrl(String.format("%s/%s/activity/compensated", context.getBaseUri(), lraId));

        endCheck(activity);

        System.out.printf("ActivityController compensating %s%n", lraId);
        return Response.ok(activity.getStatusUrl()).build();
    }

    @DELETE
    @Path("/forget")
    @Produces(MediaType.APPLICATION_JSON)
    @Forget
    public Response forgetWork(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) {
        COMPLETED_COUNT.incrementAndGet();

        assert lraId != null;

        Activity activity = activityService.getActivity(lraId);

        activityService.remove(activity.getId());
        activity.setStatus(CompensatorStatus.Completed);
        activity.setStatusUrl(String.format("%s/%s/activity/completed", context.getBaseUri(), lraId));

        System.out.printf("ActivityController forgetting %s%n", lraId);
        return Response.ok(activity.getStatusUrl()).build();
    }

    @PUT
    @Path(ACCEPT_WORK)
    @LRA(LRA.Type.REQUIRED)
    public Response acceptWork(
            @HeaderParam(LRAClient.LRA_HTTP_RECOVERY_HEADER) String rcvId,
            @HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) {
        assert lraId != null;
        Activity activity = addWork(lraId, rcvId);

        if (activity == null) {
            return Response.status(Response.Status.EXPECTATION_FAILED).entity("Missing lra data").build();
        }

        activity.setAcceptedCount(1); // tests that it is possible to asynchronously complete
        return Response.ok(lraId).build();
    }

    @GET
    @Path(ActivityController.COMPLETED_COUNT_RESOURCE_METHOD)
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response getCompleteCount() {
        return Response.ok(COMPLETED_COUNT.get()).build();
    }
    @GET
    @Path(ActivityController.COMPENSATED_COUNT_RESOURCE_METHOD)
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response getCompensatedCount() {
        return Response.ok(COMPENSATED_COUNT.get()).build();
    }

    @GET
    @Path("/timeLimitRequiredLRA")
    @Produces(MediaType.APPLICATION_JSON)
    @TimeLimit(limit = 100, unit = TimeUnit.MILLISECONDS)
    @LRA(value = LRA.Type.REQUIRED)
    public Response timeLimitRequiredLRA(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) {
        activityService.add(new Activity(lraId));

        try {
            Thread.sleep(300); // sleep for 200 miliseconds (should be longer than specified in the @TimeLimit annotation)
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return Response.status(Response.Status.OK).entity(Entity.text("Simulate business logic timeoout")).build();
    }

    @GET
    @Path(TIME_LIMIT_SUPPORTS_LRA)
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(value = LRA.Type.SUPPORTS)
    public Response timeLimitSupportsLRA(@HeaderParam(LRAClient.LRA_HTTP_HEADER) String lraId) {
        activityService.add(new Activity(lraId));

        return Response.status(Response.Status.OK).entity(Entity.text("Simulate business logic timeoout")).build();
    }

    private Activity addWork(String lraId, String rcvId) {
        assert lraId != null;

        System.out.printf("ActivityController: work id %s and rcvId %s %n", lraId, rcvId);

        try {
            return activityService.getActivity(lraId);
        } catch (NotFoundException e) {
            Activity activity = new Activity(lraId);

            activity.setRcvUrl(rcvId);
            activity.setStatus(null);

            activityService.add(activity);

            return activity;
        }
    }

    private void endCheck(Activity activity) {
        String how = activity.getHow();
        String arg = activity.getArg();

        activity.setHow(null);
        activity.setArg(null);

        if ("wait".equals(how) && arg != null && "recovery".equals(arg)) {
            lraClient.getRecoveringLRAs();
        }
    }
}
