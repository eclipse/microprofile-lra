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
import org.eclipse.microprofile.lra.tck.LraTckConfigBean;
import org.eclipse.microprofile.lra.tck.participant.activity.ParticipantService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.client.LRAClient.LRA_HTTP_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.LraCancelOnController.LRA_CANCEL_ON_CONTROLLER_PATH;

@ApplicationScoped
@Path(LraCancelOnRemoteCallController.LRA_CANCEL_ON_REMOTE_CALL_CONTROLLER_PATH)
public class LraCancelOnRemoteCallController {

    public static final String LRA_CANCEL_ON_REMOTE_CALL_CONTROLLER_PATH = "lracontroller-cancelon-remote";

    private static final Logger LOGGER = Logger.getLogger(LraCancelOnRemoteCallController.class.getName());

    @Inject
    private LraTckConfigBean config;
    
    @Inject
    private ParticipantService participantService;

    public static final String CANCEL_FROM_REMOTE_CALL = "cancelFromRemoteCall";
    /**
     * Returning <code>200</code> thus the LRA should be closed but
     * it calls the remote method which ends with 5xx which is the
     * default for the cancelling and so the whole LRA should be cancelled.
     */
    @GET
    @Path(CANCEL_FROM_REMOTE_CALL)
    @LRA(value = LRA.Type.REQUIRES_NEW)
    public Response cancelFromRemoteCall() {
        Client client = ClientBuilder.newClient();
        try {
            Response response = client
                    .target(URI.create(new URL(config.tckSuiteBaseUrl()).toExternalForm()))
                    .path(LRA_CANCEL_ON_CONTROLLER_PATH)
                    .path(LraCancelOnController.CANCEL_ON_FAMILY_DEFAULT_5XX)
                    .request().get();
            assert response.getStatus() == 500;
        } catch (MalformedURLException murle) {
            LOGGER.log(Level.SEVERE, "Cannot create url from string '"
                    + config.tckSuiteBaseUrl() + "'", murle);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            client.close();
        }
        return Response.ok().build();
    }

    @PUT
    @Path("/complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_HEADER) String lraId) throws NotFoundException {
        participantService.completeWork(lraId);
        return Response.ok().build();
    }

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_HEADER) String lraId, String userData)
            throws NotFoundException {
        participantService.compensateWork(lraId);
        return Response.ok().build();
    }
}
