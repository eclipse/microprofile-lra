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

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.tck.participant.api.LraResource.LRA_RESOURCE_PATH;

import java.net.URI;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@ApplicationScoped
@Path(NoLRAResource.NO_LRA_RESOURCE_PATH)
public class NoLRAResource {
    public static final String NO_LRA_RESOURCE_PATH = "nolraresource";
    public static final String NON_TRANSACTIONAL_WORK_PATH = "work";

    @Context
    private UriInfo context;

    @PUT
    @Path(NON_TRANSACTIONAL_WORK_PATH)
    public Response work2(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {

        if (lraId != null) {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity("Unexpected LRA context").build();
        }

        WebTarget resourcePath = ClientBuilder.newClient().target(context.getBaseUri())
                .path(LRA_RESOURCE_PATH)
                .path(LraResource.MANDATORY_LRA_RESOURCE_PATH);

        Response response = resourcePath.request().put(Entity.text(""));

        String id = checkStatusAndClose(response, Response.Status.OK.getStatusCode(), true, resourcePath);

        if (id == null) {
            return Response.status(Response.Status.PRECONDITION_FAILED).entity("LRA context was not propagated")
                    .build();
        }

        return Response.ok(id).build();
    }

    private String checkStatusAndClose(Response response, int expected, boolean readEntity, WebTarget webTarget) {
        try {
            if (expected != -1 && response.getStatus() != expected) {
                if (webTarget != null) {
                    throw new WebApplicationException(
                            String.format("%s: expected status %d got %d",
                                    webTarget.getUri().toString(), expected, response.getStatus()),
                            response);
                }

                throw new WebApplicationException(response);
            }

            if (readEntity) {
                return response.readEntity(String.class);
            }
        } finally {
            response.close();
        }

        return null;
    }
}
