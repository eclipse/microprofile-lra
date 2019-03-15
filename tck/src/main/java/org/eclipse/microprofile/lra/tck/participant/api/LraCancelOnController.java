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

import static org.eclipse.microprofile.lra.client.LRAClient.LRA_HTTP_HEADER;

import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA.Type;
import org.eclipse.microprofile.lra.tck.LraTckConfigBean;
import org.eclipse.microprofile.lra.tck.participant.activity.ParticipantService;

@ApplicationScoped
@Path(LraCancelOnController.LRA_CANCEL_ON_CONTROLLER_PATH)
public class LraCancelOnController {
    private static final Logger LOGGER = Logger.getLogger(LraCancelOnController.class.getName());
    public static final String LRA_CANCEL_ON_CONTROLLER_PATH = "lracontroller-cancelon";
    public static final String COMPLETED_COUNT_PATH = "completed";
    public static final String COMPENSATED_COUNT_PATH = "compensated";
    public static final String CLEAR = "clear";

    @Inject
    private LraTckConfigBean config;

    @Inject
    private ParticipantService participantService;

    public static final String CANCEL_ON_FAMILY_DEFAULT_4XX = "cancelOnFamilyDefault4xx";
    /**
     * Default return status for cancelling LRA is <code>4xx</code> and <code>5xx</code>
     */
    @GET
    @Path(CANCEL_ON_FAMILY_DEFAULT_4XX)
    @LRA(value = Type.REQUIRED)
    public Response cancelOnFamilyDefault4xx() {
        return Response.status(Status.BAD_REQUEST).build();
    }

    public static final String CANCEL_ON_FAMILY_DEFAULT_5XX = "cancelOnFamilyDefault5xx";
    /**
     * Default return status for cancelling LRA is <code>4xx</code> and <code>5xx</code>
     */
    @GET
    @Path(CANCEL_ON_FAMILY_DEFAULT_5XX)
    @LRA(value = Type.REQUIRED)
    public Response cancelOnFamilyDefault5xx() {
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    public static final String CANCEL_ON_FAMILY_3XX = "cancelOnFamily3xx";
    /**
     * Cancel on family is set to <code>3xx</code>. The <code>3xx</code> return code
     * has to cancel the LRA.
     */
    @GET
    @Path(CANCEL_ON_FAMILY_3XX)
    @LRA(value = Type.REQUIRES_NEW,
            cancelOnFamily = Family.REDIRECTION)
    public Response cancelOnFamily3xx() {
        return Response.status(Status.SEE_OTHER).build();
    }

    public static final String CANCEL_ON_301 = "cancelOn301";
    /**
     * Cancel on is set to <code>301</code>. The <code>301</code> return code
     * has to cancel the LRA.
     */
    @GET
    @Path(CANCEL_ON_301)
    @LRA(value = Type.REQUIRES_NEW,
        cancelOn = {Status.MOVED_PERMANENTLY})
    public Response cancelOn301() {
        return Response.status(Status.MOVED_PERMANENTLY).build();
    }

    public static final String NOT_CANCEL_ON_FAMILY_5XX = "notCancelOnFamily5xx";
    /**
     * Cancel on family is set to <code>4xx</code>,
     * the code from other families (e.g. for <code>5xx</code>
     * should not cancel but should go with close the LRA.
     */
    @GET
    @Path(NOT_CANCEL_ON_FAMILY_5XX)
    @LRA(value = Type.REQUIRES_NEW,
            cancelOnFamily = {Family.CLIENT_ERROR})
    public Response notCancelOnFamily5xx() {
        return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }

    @PUT
    @Path("/complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        participantService.completeWork(lraId, LraCancelOnController.class.getName());
        return Response.ok().build();
    }

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_HEADER) String lraId, String userData) 
            throws NotFoundException {
        participantService.compensateWork(lraId, LraCancelOnController.class.getName());
        return Response.ok().build();
    }
    
    @GET
    @Path(CLEAR)
    @Produces(MediaType.TEXT_PLAIN)
    public Response clear() {
        participantService.clear();
        return Response.ok().build();
    }

    @GET
    @Path(COMPLETED_COUNT_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response completed() {
        return Response.ok(participantService.completed()).build();
    }

    @GET
    @Path(COMPENSATED_COUNT_PATH)
    @Produces(MediaType.APPLICATION_JSON)
    public Response compensated() {
        return Response.ok(participantService.compensated()).build();
    }
}
