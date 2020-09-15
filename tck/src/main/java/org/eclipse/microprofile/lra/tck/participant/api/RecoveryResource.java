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

import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.temporal.ChronoUnit;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Path(RecoveryResource.RECOVERY_RESOURCE_PATH)
@ApplicationScoped
public class RecoveryResource {

    public static final String RECOVERY_RESOURCE_PATH = "recovery-resource";
    public static final String REQUIRED_PATH = "required";
    public static final String REQUIRED_TIMEOUT_PATH = "required-timeout";
    public static final long LRA_TIMEOUT = 500;

    @Inject
    LRAMetricService lraMetricService;

    @PUT
    @Path(REQUIRED_PATH)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response requiredLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path(REQUIRED_TIMEOUT_PATH)
    @LRA(value = LRA.Type.REQUIRED, end = false, timeLimit = LRA_TIMEOUT, timeUnit = ChronoUnit.MILLIS)
    public Response requiredTimeoutLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok(lraId).build();
    }

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensate(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId, RecoveryResource.class);

        return Response.ok().build();
    }

    @PUT
    @Path("/after")
    @AfterLRA
    public Response afterLRA(@HeaderParam(LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId, LRAStatus lraStatus) {
        lraMetricService.incrementMetric(LRAMetricType.valueOf(lraStatus.name()), lraId, RecoveryResource.class);

        return Response.ok().build();
    }
}
