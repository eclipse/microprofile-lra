/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.lra.annotation.ParticipantStatus;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.participant.LRAParticipant;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

@ApplicationScoped
@Path("participant-resource")
public class ParticipantResource implements LRAParticipant {
    private static AtomicInteger count = new AtomicInteger(0);

    @LRA(value = LRA.Type.REQUIRED)
    @PUT
    @Path("required")
    public Response requiredLRA(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) String lraId) {
        count.incrementAndGet();
        return Response.ok().build();
    }

    @Override
    public ParticipantStatus compensate(URI lra, URI parentLlra) throws NotFoundException {
        count.decrementAndGet();
        return ParticipantStatus.Compensated;
    }

    @Override
    public ParticipantStatus complete(URI lra, URI parentLlra) throws NotFoundException {
        return ParticipantStatus.Completed;
    }

    @Override
    public void forget(URI lra) {
        count.set(0);
    }
}
