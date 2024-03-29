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
package org.eclipse.microprofile.lra.tck.participant.nonjaxrs;

import java.net.URI;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.TckInvalidSignaturesTests;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

/**
 * TCK invalid LRA participant containing too many arguments in the participant method signature used for verification
 * of deployment time invalid signature detection and error report in {@link TckInvalidSignaturesTests}.
 */
@Path("too-many-args-nonjaxrs")
public class TooManyArgsParticipant {

    @GET
    @Path("enlist")
    @LRA(LRA.Type.REQUIRED)
    public Response doInLRA() {
        return Response.ok().build();
    }

    @Compensate
    public ParticipantStatus compensate(URI lraId) {
        return ParticipantStatus.Compensated;
    }

    @Complete
    public ParticipantStatus complete(URI lraId, URI parentId, Object additional) {
        return ParticipantStatus.Completed;
    }
}
