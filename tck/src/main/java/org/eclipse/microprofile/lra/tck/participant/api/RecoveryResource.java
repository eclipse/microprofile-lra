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
import org.eclipse.microprofile.lra.tck.TckRecoveryTests;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.eclipse.microprofile.lra.tck.service.LRATestService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.time.temporal.ChronoUnit;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@Path(RecoveryResource.RECOVERY_RESOURCE_PATH)
@ApplicationScoped
public class RecoveryResource {

    public static final String RECOVERY_RESOURCE_PATH = "recovery-resource";
    public static final String PHASE_1 = "phase-1";
    public static final String PHASE_2 = "phase-2";
    public static final String LRA_HEADER = "LRA";
    public static final String TRIGGER_RECOVERY = "triggerRecovery";
    public static final long LRA_TIMEOUT = 500;
    private static final String REQUIRED_PATH = "required";
    private static final String REQUIRED_TIMEOUT_PATH = "required-timeout";
    
    @Inject
    LRAMetricService lraMetricService;

    @Inject
    LRATestService lraTestService;

    /**
     * Starts a new LRA and enlists an instance of this class with it as a participant
     *
     * @param timeout whether or not the LRA should time out after 500 milliseconds
     * @return response containing the LRA id of the newly started LRA
     */
    @GET
    @Path(PHASE_1)
    public Response phase1(@QueryParam("timeout") @DefaultValue("false") boolean timeout,
                           @HeaderParam(TckRecoveryTests.LRA_TCK_DEPLOYMENT_URL) URL deploymentURL) {
        
        lraTestService.start(deploymentURL);
        
        // start a new LRA and join it with this resource
        URI lra;
        Response response;
        

        response = lraTestService.getTCKSuiteTarget().path(RecoveryResource.RECOVERY_RESOURCE_PATH)
                .path(timeout ? RecoveryResource.REQUIRED_TIMEOUT_PATH : RecoveryResource.REQUIRED_PATH)
                .request().put(Entity.text(""));

        lra = URI.create(response.readEntity(String.class));

        Response.ResponseBuilder responseBuilder = Response.ok(lra);

        return responseBuilder.build();
    }

    /**
     * Cancels the supplied LRA and verifies that all required actions have been performed
     *
     * @param lraId lra id of the LRA to be cancelled
     * @return a {@link Response} object containing the optional error message
     */
    @GET
    @Path(PHASE_2)
    public Response phase2(@HeaderParam(LRA_HEADER) URI lraId,
                           @HeaderParam(TckRecoveryTests.LRA_TCK_DEPLOYMENT_URL) URL deploymentURL) {
        lraTestService.start(deploymentURL);
        lraTestService.getLRAClient().cancelLRA(lraId);
        lraTestService.waitForCallbacks(lraId);

        // assert compensate has been called
        int compensations = lraMetricService.getMetric(LRAMetricType.Compensated, lraId, RecoveryResource.class.getName());
        if (compensations < 1) {
            return assertFailedResponse("Compensate on restarted service should have been called. Was " + compensations);
        }

        // assert after LRA has been called
        int afterLRACalls = lraMetricService.getMetric(LRAMetricType.Cancelled, lraId, RecoveryResource.class.getName());
        if (afterLRACalls < 1) {
            return assertFailedResponse("After LRA with Cancelled status should have been called. Was " + afterLRACalls);
        }

        return Response.ok().build();
    }

    private Response assertFailedResponse(String message) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(message).build();
    }

    @GET
    @Path(TRIGGER_RECOVERY)
    public Response triggerRecovery(@HeaderParam(LRA_HEADER) URI lraId,
                                    @HeaderParam(TckRecoveryTests.LRA_TCK_DEPLOYMENT_URL) URL deploymentURL) {
        lraTestService.start(deploymentURL);
        lraTestService.waitForRecovery(lraId);
        return Response.ok().build();
    }

    // LRA participant methods

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
        lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId, RecoveryResource.class.getName());

        return Response.ok().build();
    }

    @PUT
    @Path("/after")
    @AfterLRA
    public Response afterLRA(@HeaderParam(LRA.LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId, LRAStatus lraStatus) {
        lraMetricService.incrementMetric(LRAMetricType.valueOf(lraStatus.name()), lraId,
                RecoveryResource.class.getName());

        return Response.ok().build();
    }
}
