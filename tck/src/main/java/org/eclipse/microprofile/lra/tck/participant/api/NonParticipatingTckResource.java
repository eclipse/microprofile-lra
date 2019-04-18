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

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.annotation.ws.rs.NestedLRA;
import org.eclipse.microprofile.lra.tck.LRAClientOps;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@ApplicationScoped
@Path(NonParticipatingTckResource.TCK_NON_PARTICIPANT_RESOURCE_PATH)
@LRA(value = LRA.Type.SUPPORTS, end = false)
public class NonParticipatingTckResource {
    public static final String TCK_NON_PARTICIPANT_RESOURCE_PATH = "non-participating-tck-resource";

    public static final String START_AND_END_PATH = "/start-and-end";
    public static final String START_BUT_DONT_END_PATH = "/start-dont-end";
    public static final String START_AND_END_NESTED_PATH = "/start-nested-and-end";

    public static final String START_BUT_DONT_END_NESTED_PATH = "/start-nested-and-dont-end";
    public static final String NEVER_PATH = "/never";
    public static final String END_PATH = "/end";
    public static final String SUPPORTS_PATH = "/supports";


    public static final String OK_TEXT = "OK";

    private static final String MISSING_LRA_DATA = "Missing LRA data";
    public static final String TRANSACTIONAL_WORK_PATH = "work";
    public static final String ACCEPT_WORK = "acceptWork";
    private static final Logger LOGGER = Logger.getLogger(NonParticipatingTckResource.class.getName());

    static final String MANDATORY_LRA_RESOURCE_PATH = "/mandatory";

    private static final AtomicInteger COMPLETED_COUNT = new AtomicInteger(0);
    private static final AtomicInteger COMPENSATED_COUNT = new AtomicInteger(0);
    public static final String STATUS_CODE_QUERY_NAME = "Coerce-Status";
    public static final String START_LRA_VIA_REMOTE_INVOCATIOM = "/start-via-remote-invocation";

    @Context
    private UriInfo context;

    @PUT
    @Path(NonParticipatingTckResource.NEVER_PATH)
    @LRA(value = LRA.Type.NEVER)
    public Response neverRunWithLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(SUPPORTS_PATH)
    @LRA(value = LRA.Type.SUPPORTS)
    public Response supports(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_AND_END_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW,
            cancelOnFamily = Response.Status.Family.SERVER_ERROR) // default is to end the LRA
    public Response startAndEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId,
                                   @DefaultValue("200") @QueryParam(STATUS_CODE_QUERY_NAME) int coerceStatus) {
        return Response.status(coerceStatus).entity(checkLRANotNull(lraId)).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_BUT_DONT_END_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false,
            cancelOnFamily = Response.Status.Family.SERVER_ERROR)
    public Response startDontEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId,
                                    @DefaultValue("200") @QueryParam(STATUS_CODE_QUERY_NAME) int coerceStatus) {
        return Response.status(coerceStatus).entity(checkLRANotNull(lraId)).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.END_PATH)
    @LRA(value = LRA.Type.MANDATORY,
            cancelOnFamily = Response.Status.Family.SERVER_ERROR) // default is to end the LRA
    public Response endLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId,
                           @DefaultValue("200") @QueryParam(STATUS_CODE_QUERY_NAME) int coerceStatus) {
        return Response.status(coerceStatus).entity(checkLRANotNull(lraId)).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_AND_END_NESTED_PATH)
    @LRA(value = LRA.Type.MANDATORY,
            cancelOnFamily = Response.Status.Family.SERVER_ERROR) // default is to end the LRA
    @NestedLRA
    public Response startAndEndNestedLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_BUT_DONT_END_NESTED_PATH)
    @LRA(value = LRA.Type.MANDATORY, end = false,
            cancelOnFamily = Response.Status.Family.SERVER_ERROR)
    @NestedLRA
    public Response startAndDontEndNestedLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(NonParticipatingTckResource.START_LRA_VIA_REMOTE_INVOCATIOM)
    @LRA(value = LRA.Type.SUPPORTS, end = false,
            cancelOnFamily = Response.Status.Family.SERVER_ERROR)
    public Response notSupportedButCallServiceWhichStartsButDoesntEndAnLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        return Response.ok(invokeRestEndpoint(lraId, TCK_NON_PARTICIPANT_RESOURCE_PATH, START_BUT_DONT_END_PATH,
                200)).build();
    }

    private String invokeRestEndpoint(String lra, String basePath, String path, int coerceResponse) {
        Client client = ClientBuilder.newClient();

        try {
            WebTarget target = client.target(context.getBaseUri());
            return new LRAClientOps(target).invokeRestEndpointAndReturnLRA(lra, basePath, path, coerceResponse);
        } finally {
            client.close();
        }
    }

    private String checkLRANotNull(String lraId) {
        if (lraId == null) {
            throw new InvalidLRAIdException(null,
                    String.format("%s: missing '%s' header", context.getPath(), LRA_HTTP_CONTEXT_HEADER));
        }

        return lraId;
    }
}
