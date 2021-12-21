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

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.LRAClientOps;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@ApplicationScoped
@Path(NonParticipatingTckResource.TCK_NON_PARTICIPANT_RESOURCE_PATH)
@LRA(value = LRA.Type.SUPPORTS, end = false)
public class NonParticipatingTckResource extends ResourceParent {
    public static final String TCK_NON_PARTICIPANT_RESOURCE_PATH = "non-participating-tck-resource";

    public static final String START_AND_END_PATH = "/start-and-end";
    public static final String START_BUT_DONT_END_PATH = "/start-dont-end";
    public static final String START_AND_END_NESTED_PATH = "/start-nested-and-end";

    public static final String START_BUT_DONT_END_NESTED_PATH = "/start-nested-and-dont-end";
    public static final String NEVER_PATH = "/never";
    public static final String END_PATH = "/end";
    public static final String SUPPORTS_PATH = "/supports";

    public static final String STATUS_CODE_QUERY_NAME = "Coerce-Status";
    public static final String START_LRA_VIA_REMOTE_INVOCATION = "/start-via-remote-invocation";

    @Context
    private UriInfo context;

    @PUT
    @Path(NonParticipatingTckResource.NEVER_PATH)
    @LRA(value = LRA.Type.NEVER)
    public Response neverRunWithLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(SUPPORTS_PATH)
    @LRA(value = LRA.Type.SUPPORTS, end = false)
    public Response supports(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_AND_END_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW, cancelOnFamily = Response.Status.Family.SERVER_ERROR) // default is to end the
                                                                                              // LRA
    public Response startAndEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @DefaultValue("200") @QueryParam(STATUS_CODE_QUERY_NAME) int coerceStatus) {
        return Response.status(coerceStatus).entity(checkLRANotNull(lraId)).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_BUT_DONT_END_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false, cancelOnFamily = Response.Status.Family.SERVER_ERROR)
    public Response startDontEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @DefaultValue("200") @QueryParam(STATUS_CODE_QUERY_NAME) int coerceStatus) {
        return Response.status(coerceStatus).entity(checkLRANotNull(lraId)).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.END_PATH)
    @LRA(value = LRA.Type.MANDATORY, cancelOnFamily = Response.Status.Family.SERVER_ERROR) // default is to end the LRA
    public Response endLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @DefaultValue("200") @QueryParam(STATUS_CODE_QUERY_NAME) int coerceStatus) {
        return Response.status(coerceStatus).entity(checkLRANotNull(lraId)).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_AND_END_NESTED_PATH)
    @LRA(value = LRA.Type.NESTED, cancelOnFamily = Response.Status.Family.SERVER_ERROR) // default is to end the LRA
    public Response startAndEndNestedLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_BUT_DONT_END_NESTED_PATH)
    @LRA(value = LRA.Type.NESTED, end = false, cancelOnFamily = Response.Status.Family.SERVER_ERROR)
    public Response startAndDontEndNestedLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_LRA_VIA_REMOTE_INVOCATION)
    @LRA(value = LRA.Type.SUPPORTS, end = false, cancelOnFamily = Response.Status.Family.SERVER_ERROR)
    public Response notSupportedButCallServiceWhichStartsButDoesntEndAnLRA(
            @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(invokeRestEndpoint(lraId, TCK_NON_PARTICIPANT_RESOURCE_PATH, START_BUT_DONT_END_PATH,
                200)).build();
    }

    private String invokeRestEndpoint(URI lra, String basePath, String path, int coerceResponse) {
        Client client = ClientBuilder.newClient();

        try {
            WebTarget target = client.target(context.getBaseUri());
            return new LRAClientOps(target).invokeRestEndpointAndReturnLRA(lra, basePath, path, coerceResponse);
        } finally {
            client.close();
        }
    }

    private URI checkLRANotNull(URI lraId) {
        if (lraId == null) {
            throw new WrongHeaderException(
                    String.format("%s: missing '%s' header", context.getPath(), LRA_HTTP_CONTEXT_HEADER));
        }

        return lraId;
    }
}
