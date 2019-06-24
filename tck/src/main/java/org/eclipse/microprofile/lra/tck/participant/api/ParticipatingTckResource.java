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
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@ApplicationScoped
@Path(ParticipatingTckResource.TCK_PARTICIPANT_RESOURCE_PATH)
public class ParticipatingTckResource {
    private static final Logger LOGGER = Logger.getLogger(ParticipatingTckResource.class.getName());

    public static final String TCK_PARTICIPANT_RESOURCE_PATH = "participating-tck-resource";
    public static final String JOIN_WITH_EXISTING_LRA_PATH = "/join-with-existing-lra";
    public static final String JOIN_WITH_EXISTING_LRA_PATH2 = "/join-with-existing-lra2";
    public static final String JOIN_WITH_NEW_LRA_PATH = "/join-with-new-LRA";
    public static final String LEAVE_PATH = "/leave";
    public static final String ACCEPT_PATH = "/accept";

    public static final String RECOVERY_PARAM = "recoveryCount";

    private int recoveryPasses = 0;

    @Inject
    private LRAMetricService lraMetricService;

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId, String userData)
            throws NotFoundException {
        if(lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        LOGGER.info(String.format("LRA id '%s' was told to compensate", lraId));

        return getEndPhaseResponse(false, lraId);
    }

    @PUT
    @Path("/complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId, String userData)
            throws NotFoundException {
        if(lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        LOGGER.info(String.format("LRA id '%s' was told to complete", lraId));

        return getEndPhaseResponse(true, lraId);
    }

    private Response getEndPhaseResponse(boolean complete, URI lraId) {
        if (--recoveryPasses > 0) {
            return Response.accepted().build();
        }

        if (complete) {
            lraMetricService.incrementMetric(LRAMetricType.Completed, lraId, ParticipatingTckResource.class.getName());
        } else {
            lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId, ParticipatingTckResource.class.getName());
        }

        return Response.ok().build();
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

    @PUT
    @Path(LEAVE_PATH)
    @LRA(value = LRA.Type.SUPPORTS, end = false)
    @Leave
    public Response leaveLRA() {
        return Response.ok().build();
    }

    // if this resource path is invoked outside of a LRA then, since this JAX-RS resoruce is annotated
    // as a participant, the implementation MUST report a PRECONDITION_FAILED status
    @PUT
    @Path(ParticipatingTckResource.JOIN_WITH_EXISTING_LRA_PATH)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response joinWithExistingLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok().build();
    }

    @PUT
    @Path(ParticipatingTckResource.JOIN_WITH_EXISTING_LRA_PATH2)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response joinWithExistingLRA2(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok().build();
    }

    @PUT
    @Path(ParticipatingTckResource.JOIN_WITH_NEW_LRA_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false)
    public Response joinWithNewLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok().build();
    }
}
