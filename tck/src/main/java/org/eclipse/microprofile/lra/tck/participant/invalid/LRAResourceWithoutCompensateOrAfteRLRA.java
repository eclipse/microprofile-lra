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
package org.eclipse.microprofile.lra.tck.participant.invalid;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

@Path(LRAResourceWithoutCompensateOrAfteRLRA.INVALID_LRA_RESOURCE_PATH)
public class LRAResourceWithoutCompensateOrAfteRLRA {

    public static final String INVALID_LRA_RESOURCE_PATH = "/invalid-lra-resource-path";

    @GET
    @Path("/lra")
    @LRA(value = LRA.Type.REQUIRES_NEW)
    public Response doInLRA(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId).build();
    }

    // intentionally don't include any of Compensate or AfterLRA methods which should fail the deployment

    @PUT
    @Path("/complete")
    @Complete
    public Response complete() {
        throw new WebApplicationException(500);
    }

}
