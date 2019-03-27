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
package org.eclipse.microprofile.lra.tck.participant.api;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.annotation.ws.rs.Leave;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_HEADER;

@ApplicationScoped
@Path(ParticipatingTckResource.TCK_PARTICIPANT_RESOURCE_PATH)
public class ParticipatingTckResource {
    private static final Logger LOGGER = Logger.getLogger(ParticipatingTckResource.class.getName());

    public static final String TCK_PARTICIPANT_RESOURCE_PATH = "participating-tck-resource";
    public static final String JOIN_WITH_EXISTNG_LRA_PATH = "/join-with-existing-lra";
    public static final String JOIN_WITH_EXISTNG_LRA_PATH2 = "/join-with-existing-lra2";
    public static final String JOIN_WITH_NEW_LRA_PATH = "/join-with-new-LRA";
    public static final String COMPLETED_CNT_PATH = "/completed-count";
    public static final String COMPENSATED_CNT_PATH = "/compensated-count";
    public static final String LEAVE_PATH = "/leave";

    private static final AtomicInteger COMPLETED_COUNT = new AtomicInteger(0);
    private static final AtomicInteger COMPENSATED_COUNT = new AtomicInteger(0);

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_HEADER) String lraId, String userData)
            throws NotFoundException {
        if(lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        COMPENSATED_COUNT.incrementAndGet();

        LOGGER.info(String.format("LRA id '%s' was compensated", lraId));

        return Response.ok().build();
    }

    @PUT
    @Path("/complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_HEADER) String lraId, String userData)
            throws NotFoundException {
        if(lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        COMPLETED_COUNT.incrementAndGet();

        LOGGER.info(String.format("LRA id '%s' was completed", lraId));

        return Response.ok().build();
    }

    @PUT
    @Path(LEAVE_PATH)
    @LRA(value = LRA.Type.SUPPORTS, end = false)
    @Leave
    public Response leaveLRA() {
        return Response.ok().build();
    }

    @GET
    @Path(COMPLETED_CNT_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response getCompletedCount() {
        return Response.ok(COMPLETED_COUNT.get()).build();
    }

    @GET
    @Path(COMPENSATED_CNT_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    @LRA(LRA.Type.NOT_SUPPORTED)
    public Response getCompensatedCount() {
        return Response.ok(COMPENSATED_COUNT.get()).build();
    }

    // if this resource path is invoked outside of a LRA then, since this JAX-RS resoruce is annotated
    // as a participant, the implementation MUST report a PRECONDITION_FAILED status
    @PUT
    @Path(ParticipatingTckResource.JOIN_WITH_EXISTNG_LRA_PATH)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response joinWithExistingLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok().build();
    }

    @PUT
    @Path(ParticipatingTckResource.JOIN_WITH_EXISTNG_LRA_PATH2)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response joinWithExistingLRA2(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok().build();
    }

    @PUT
    @Path(ParticipatingTckResource.JOIN_WITH_NEW_LRA_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false)
    public Response joinWithNewLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok().build();
    }
}
