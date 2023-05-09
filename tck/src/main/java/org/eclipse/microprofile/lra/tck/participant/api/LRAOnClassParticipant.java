/**********************************************************************
* Copyright (c) 2022 Contributors to the Eclipse Foundation
 *
 * See the NOTICES file(s) distributed with this work for additional
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
*
* SPDX-License-Identifier: Apache-2.0
**********************************************************************/package org.eclipse.microprofile.lra.tck.participant.api;
package org.eclipse.microprofile.lra.tck.participant.api;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_ENDED_CONTEXT_HEADER;
import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_RECOVERY_HEADER;

import java.net.URI;

import org.eclipse.microprofile.lra.LRAResponse;
import org.eclipse.microprofile.lra.annotation.AfterLRA;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.eclipse.microprofile.lra.tck.service.LRATestService;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * resource for testing that methods annotated with {@link AfterLRA} are notified correctly when an LRA terminates
 */
@ApplicationScoped
@LRA(value = LRA.Type.REQUIRED)
@Path(LRAOnClassParticipant.LRA_PARTICIPANT_PATH)
public class LRAOnClassParticipant {
    public static final String LRA_PARTICIPANT_PATH = "lra-participant-class-annotation";
    public static final String LRA_PARTICIPANT_WORK = "work";

    private static final String AFTER_LRA = "/after";

    @Inject
    private LRAMetricService lraMetricService;

    @Inject
    private LRATestService lraTestService;

    @PUT
    @Path("/complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        lraMetricService.incrementMetric(LRAMetricType.Completed, lraId, LRAOnClassParticipant.class);
        return LRAResponse.completed();
    }

    @PUT
    @Path(LRA_PARTICIPANT_WORK)
    @LRA(value = LRA.Type.REQUIRED, end = false)
    public Response activityWithLRA(@HeaderParam(LRA_HTTP_RECOVERY_HEADER) URI recoveryId,
            @HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.ok().build();
    }

    @PUT
    @Path(AFTER_LRA)
    @AfterLRA // this method will be called when the LRA associated with the method activityWithLRA finishes
    public Response afterEnd(@HeaderParam(LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId, LRAStatus status) {
        return lraTestService.processAfterLRAInfo(lraId, status, LRAOnClassParticipant.class,
                LRA_PARTICIPANT_PATH + AFTER_LRA);
    }
}
