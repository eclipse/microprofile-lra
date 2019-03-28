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

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_HEADER;

@ApplicationScoped
@Path(LRATypeTckResource.TCK_LRA_TYPE_RESOURCE_PATH)
public class LRATypeTckResource {
    public static final String TCK_LRA_TYPE_RESOURCE_PATH = "lra-type-tck-resource";

    public static final String REQUIRED_PATH = "/required";
    public static final String REQUIRES_NEW_PATH = "/requires-new";
    public static final String SUPPORTS_PATH = "/supports";
    public static final String NOT_SUPPORTED_PATH = "/not-supported";
    public static final String MANDATORY_PATH = "/mandatory";
    public static final String NEVER_PATH = "/never";

    public static final String REQUIRED_WITH_END_PATH = "/end-required";
    public static final String REQUIRES_NEW_WITH_END_PATH = "/end-requires-new";
    public static final String SUPPORTS_WITH_END_PATH = "/end-supports";
    public static final String NOT_SUPPORTED_WITH_END_PATH = "/end-not-supported";
    public static final String MANDATORY_WITH_END_PATH = "/end-mandatory";
    public static final String NEVER_WITH_END_PATH = "/end-never";

    // resource methods for each LRA.Type attribute
    @GET
    @Path(REQUIRED_PATH)
    @LRA(value = LRA.Type.REQUIRED)
    public Response requiredLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(REQUIRES_NEW_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW)
    public Response requiresNewLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(MANDATORY_PATH)
    @LRA(value = LRA.Type.MANDATORY)
    public Response mandatoryLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(SUPPORTS_PATH)
    @LRA(value = LRA.Type.SUPPORTS)
    public Response supportsLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(NOT_SUPPORTED_PATH)
    @LRA(value = LRA.Type.NOT_SUPPORTED)
    public Response notSupportedLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(NEVER_PATH)
    @LRA(value = LRA.Type.NEVER)
    public Response neverLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }

    // similar set of resources methods but with the LRA.end attribute set to false
    @GET
    @Path(REQUIRED_WITH_END_PATH)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response requiredEndLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(REQUIRES_NEW_WITH_END_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false)
    public Response requiresNewEndLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(MANDATORY_WITH_END_PATH)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response mandatoryEndLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(SUPPORTS_WITH_END_PATH)
    @LRA(value = LRA.Type.SUPPORTS, end = false)
    public Response supportsEndLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(NOT_SUPPORTED_WITH_END_PATH)
    @LRA(value = LRA.Type.NOT_SUPPORTED, end = false)
    public Response notSupportedEndLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
    @GET
    @Path(NEVER_WITH_END_PATH)
    @LRA(value = LRA.Type.NEVER, end = false)
    public Response neverEndLRA(@HeaderParam(LRA_HTTP_HEADER) String lraId) {
        return Response.ok(lraId).build();
    }
}