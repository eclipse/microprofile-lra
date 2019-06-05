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
import org.eclipse.microprofile.lra.tck.participant.api.InvalidLRAIdException;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.ACCEPT_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.RECOVERY_PARAM;

/**
 * TCK valid LRA participant containing a combination of valid participant method signatures
 */
@ApplicationScoped
@Path(ValidLRAParticipantWithString.RESOURCE_PATH)
public class ValidLRAParticipantWithString {
    private static final Logger LOGGER = Logger.getLogger(ValidLRAParticipantWithString.class.getName());

    public static final String RESOURCE_PATH = "valid-nonjaxrs-string";
    public static final String ENLIST_WITH_COMPLETE = "nonjaxrs-string-enlist-complete";

    private int recoveryPasses;

    @Inject
    private LRAMetricService lraMetricService;

    @GET
    @Path(ENLIST_WITH_COMPLETE)
    @LRA(value = Type.REQUIRED)
    public Response enlistWithComplete(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }

    public static final String ENLIST_WITH_COMPENSATE = "nonjaxrs-string-enlist-compensate";

    @GET
    @Path(ENLIST_WITH_COMPENSATE)
    @LRA(value = Type.REQUIRED, cancelOn = Response.Status.INTERNAL_SERVER_ERROR)
    public Response enlistWithCompensate(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) String lraId) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(lraId).build();
    }


    @Complete
    public void completeWithException(String lraId, URI parentId) { // Intentionally mix of String and URI
        verifyLRAId(lraId);

        try {
            lraMetricService.incrementMetric(LRAMetricType.COMPLETE, new URI(lraId));
        } catch (URISyntaxException e) {
            throw new InvalidLRAIdException(lraId, "Not a vlaid URI", e);
        }

        LOGGER.fine(String.format("LRA id '%s' was completed", lraId));
        throw new WebApplicationException(Response.ok().build());
    }

    @Compensate
    public ParticipantStatus compensate(URI lraId) {
        verifyLRAId(lraId);

        lraMetricService.incrementMetric(LRAMetricType.COMPENSATE, lraId);

        LOGGER.fine(String.format("LRA id '%s' was compensated", lraId));
        return ParticipantStatus.Compensating;
    }

    @Status
    public Response status(URI lraId) {
        verifyLRAId(lraId);

        lraMetricService.incrementMetric(LRAMetricType.STATUS, lraId);

        LOGGER.fine(String.format("LRA id '%s' status called, return FailedToCompensate to get @Forget called", lraId));
        return Response.ok(ParticipantStatus.FailedToCompensate).build();
    }

    @Forget
    public void forget(URI lraId) {
        verifyLRAId(lraId);

        lraMetricService.incrementMetric(LRAMetricType.FORGET, lraId);

        LOGGER.fine(String.format("LRA id '%s' forget called", lraId));
    }

    private void verifyLRAId(Object lraId) {
        if (lraId == null) {
            throw new NullPointerException("lraId cannot be null");
        }
    }

    @PUT
    @Path(ACCEPT_PATH)
    @LRA(value = Type.REQUIRES_NEW)
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
