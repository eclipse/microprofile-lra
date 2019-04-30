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
package org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA.Type;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.ACCEPT_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.RECOVERY_PARAM;

/**
 * TCK valid LRA participant containing a combination of valid participant method signatures
 */
@ApplicationScoped
@Path(ValidLRAParticipant.RESOURCE_PATH)
public class ValidLRAParticipant {
    private static final Logger LOGGER = Logger.getLogger(ValidLRAParticipant.class.getName());
    
    public static final String RESOURCE_PATH = "valid-nonjaxrs";
    public static final String COMPLETED_COUNT_PATH = "completed";
    public static final String COMPENSATED_COUNT_PATH = "compensated";
    public static final String STATUS_COUNT_PATH = "status";
    public static final String FORGET_COUNT_PATH = "forget";

    private final AtomicInteger completedCount = new AtomicInteger(0);
    private final AtomicInteger compensatedCount = new AtomicInteger(0);
    private final AtomicInteger statusCount = new AtomicInteger(0);
    private final AtomicInteger forgetCount = new AtomicInteger(0);

    public static final String ENLIST_WITH_COMPLETE = "nonjaxrs-enlist-complete";
    private int recoveryPasses;

    @GET
    @Path(ENLIST_WITH_COMPLETE)
    @LRA(value = Type.REQUIRED)
    public Response enlistWithComplete() {
        return Response.ok().build();
    }

    public static final String ENLIST_WITH_COMPENSATE = "nonjaxrs-enlist-compensate";

    @GET
    @Path(ENLIST_WITH_COMPENSATE)
    @LRA(value = Type.REQUIRED, cancelOn = Response.Status.INTERNAL_SERVER_ERROR)
    public Response enlistWithCompensate() {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }


    @Complete
    public void completeWithException(URI lraId, URI parentId) {
        verifyLRAId(lraId);

        completedCount.incrementAndGet();

        LOGGER.fine(String.format("LRA id '%s' was completed", lraId));
        throw new WebApplicationException(Response.ok().build());
    }

    @Compensate
    public ParticipantStatus compensate(URI lraId) {
        verifyLRAId(lraId);

        compensatedCount.incrementAndGet();

        LOGGER.fine(String.format("LRA id '%s' was compensated", lraId));
        return ParticipantStatus.Compensating;
    }

    @Status
    public Response status(URI lraId) {
        verifyLRAId(lraId);

        statusCount.incrementAndGet();

        LOGGER.fine(String.format("LRA id '%s' status called, return FailedToCompensate to get @Forget called", lraId));
        return Response.ok(ParticipantStatus.FailedToCompensate).build();
    }

    @Forget
    public void forget(URI lraId) {
        verifyLRAId(lraId);

        forgetCount.incrementAndGet();

        LOGGER.fine(String.format("LRA id '%s' forget called", lraId));
    }

    private void verifyLRAId(URI lraId) {
        if (lraId == null) {
            throw new NullPointerException("lraId cannot be null");
        }
    }

    @GET
    @Path(COMPLETED_COUNT_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public Response completed() {
        return Response.ok(completedCount.toString()).build();
    }

    @GET
    @Path(COMPENSATED_COUNT_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public Response compensated() {
        return Response.ok(compensatedCount.toString()).build();
    }

    @GET
    @Path(STATUS_COUNT_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public Response statusCount() {
        return Response.ok(statusCount.toString()).build();
    }

    @GET
    @Path(FORGET_COUNT_PATH)
    @Produces(MediaType.TEXT_PLAIN)
    public Response forgetCount() {
        return Response.ok(forgetCount.toString()).build();
    }

    @PUT
    @Path(ACCEPT_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW)
    public Response acceptLRA(@QueryParam(RECOVERY_PARAM) @DefaultValue("0") Integer recoveryPasses) {
        this.recoveryPasses = recoveryPasses;

        return Response.ok().build();
    }

    @GET
    @Path(ACCEPT_PATH)
    public Response getAcceptLRA() {
        return Response.ok(this.recoveryPasses).build();
    }
}
