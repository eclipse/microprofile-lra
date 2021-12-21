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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * TCK Tests related to the 410 status code handling. Version without a Status method.
 */
@ApplicationScoped
@Path(LRAUnknownResource.LRA_CONTROLLER_PATH)
public class LRAUnknownResource extends ResourceParent {
    public static final String LRA_CONTROLLER_PATH = "lraUnknownController";
    public static final String TRANSACTIONAL_WORK_PATH = "work";

    private static final Logger LOGGER = Logger.getLogger(LRAUnknownResource.class.getName());
    private static final String AFTER_LRA = "/after";

    private Map<String, Scenario> scenarioMap = new HashMap<>();

    @Inject
    private LRAMetricService lraMetricService;

    @PUT
    @Path(TRANSACTIONAL_WORK_PATH)
    @LRA
    public Response activityWithLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId,
            @QueryParam("scenario") Scenario scenario) {

        scenarioMap.put(lraId.toASCIIString(), scenario);
        // scenario.pathResponseCode determines if /complete or /compensate will be called.
        return Response.status(scenario.getPathResponseCode()).entity(lraId).build();
    }

    @PUT
    @Path("/complete")
    @Produces(MediaType.APPLICATION_JSON)
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId)
            throws NotFoundException {
        lraMetricService.incrementMetric(LRAMetricType.Completed, lraId, LRAUnknownResource.class);

        // flow for the following cases
        // Scenario.COMPLETE_RETRY
        // -> /complete -> 202
        // -> /complete -> 410 (recalled to find final status by implementation)

        // Scenario.COMPLETE_IMMEDIATE
        // -> /complete -> 410

        int responseCode = 410;
        Scenario scenario = scenarioMap.get(lraId.toASCIIString());
        if (scenario == Scenario.COMPLETE_RETRY) {
            responseCode = 202; // The 'action' is in progress
            scenarioMap.remove(lraId.toASCIIString()); // so that by next call the return status is 410.
        }

        LOGGER.info(String.format("LRA id '%s' was completed", lraId.toASCIIString()));
        return Response.status(responseCode).build();
    }

    @PUT
    @Path("/compensate")
    @Produces(MediaType.APPLICATION_JSON)
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId)
            throws NotFoundException {

        lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId, LRAUnknownResource.class);

        // flow for the following cases
        // Scenario.COMPENSATE_RETRY
        // -> /compensate -> 202
        // -> /compensate -> 410 (recalled to find final status by implementation)

        // Scenario.COMPENSATE_IMMEDIATE
        // -> /compensate -> 410

        int responseCode = 410;
        Scenario scenario = scenarioMap.get(lraId.toASCIIString());
        if (scenario == Scenario.COMPENSATE_RETRY) {
            responseCode = 202; // The 'action' is in progress
            scenarioMap.remove(lraId.toASCIIString()); // so that by next call the return status is 410.
        }

        LOGGER.info(String.format("LRA id '%s' was compensated", lraId));
        return Response.status(responseCode).build();
    }
}
