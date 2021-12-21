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

import java.net.URI;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@Path(LRATypeTckInterfaceResource.TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH)
public class LRATypeTckInterfaceResource extends ResourceParent implements LRATypeTckInterface {

    public static final String TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH = "lra-type-tck-interface-resource";

    public Response requiredLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response requiresNewLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response mandatoryLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response supportsLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response notSupportedLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response neverLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response requiredEndLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response requiresNewEndLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response mandatoryEndLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response supportsEndLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response notSupportedEndLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

    public Response neverEndLRA(URI lraId) {
        return Response.ok(lraId).build();
    }

}
