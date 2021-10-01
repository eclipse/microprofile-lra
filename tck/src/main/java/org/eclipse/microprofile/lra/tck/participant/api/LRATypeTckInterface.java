/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

public interface LRATypeTckInterface {

    String REQUIRED_PATH = "/required";
    String REQUIRES_NEW_PATH = "/requires-new";
    String SUPPORTS_PATH = "/supports";
    String NOT_SUPPORTED_PATH = "/not-supported";
    String MANDATORY_PATH = "/mandatory";
    String NEVER_PATH = "/never";

    String REQUIRED_WITH_END_FALSE_PATH = "/end-required";
    String REQUIRES_NEW_WITH_END_FALSE_PATH = "/end-requires-new";
    String SUPPORTS_WITH_END_FALSE_PATH = "/end-supports";
    String NOT_SUPPORTED_WITH_END_FALSE_PATH = "/end-not-supported";
    String MANDATORY_WITH_END_FALSE_PATH = "/end-mandatory";
    String NEVER_WITH_END_FALSE_PATH = "/end-never";

    @GET
    @Path(REQUIRED_PATH)
    @LRA(value = LRA.Type.REQUIRED)
    Response requiredLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(REQUIRES_NEW_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW)
    Response requiresNewLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(MANDATORY_PATH)
    @LRA(value = LRA.Type.MANDATORY)
    Response mandatoryLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(SUPPORTS_PATH)
    @LRA(value = LRA.Type.SUPPORTS)
    Response supportsLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(NOT_SUPPORTED_PATH)
    @LRA(value = LRA.Type.NOT_SUPPORTED)
    Response notSupportedLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(NEVER_PATH)
    @LRA(value = LRA.Type.NEVER)
    Response neverLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(REQUIRED_WITH_END_FALSE_PATH)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    Response requiredEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(REQUIRES_NEW_WITH_END_FALSE_PATH)
    @LRA(value = LRA.Type.REQUIRES_NEW, end = false)
    Response requiresNewEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(MANDATORY_WITH_END_FALSE_PATH)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    Response mandatoryEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(SUPPORTS_WITH_END_FALSE_PATH)
    @LRA(value = LRA.Type.SUPPORTS, end = false)
    Response supportsEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(NOT_SUPPORTED_WITH_END_FALSE_PATH)
    @LRA(value = LRA.Type.NOT_SUPPORTED, end = false)
    Response notSupportedEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);

    @GET
    @Path(NEVER_WITH_END_FALSE_PATH)
    @LRA(value = LRA.Type.NEVER, end = false)
    Response neverEndLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId);
}
