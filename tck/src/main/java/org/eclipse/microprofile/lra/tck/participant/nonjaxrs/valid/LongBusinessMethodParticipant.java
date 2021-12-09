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
package org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path(LongBusinessMethodParticipant.ROOT_PATH)
public class LongBusinessMethodParticipant {

    public static final String ROOT_PATH = "long-business-participant";
    public static final String BUSINESS_METHOD = "business-method";
    public static final String SYNC_METHOD = "sync-method";

    private static final Logger LOGGER = Logger.getLogger(LongBusinessMethodParticipant.class.getName());

    private CountDownLatch businessLatch = new CountDownLatch(1);
    private CountDownLatch syncLatch = new CountDownLatch(1);

    @Inject
    private LRAMetricService lraMetricService;

    @Compensate
    public void compensate(URI lraId) {
        assert lraId != null;
        if (businessLatch.getCount() > 0) {
            businessLatch.countDown();
        }
        lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId, LongBusinessMethodParticipant.class);
    }

    @PUT
    @Path(BUSINESS_METHOD)
    @LRA(value = LRA.Type.MANDATORY, end = false)
    public Response enlistWithLongLatency(@HeaderParam(LRA.LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        LOGGER.info("call of enlistWithLongLatency");
        try {
            syncLatch.countDown();
            // await for compensation
            businessLatch.await();
            return Response.ok(lraId).build();
        } catch (InterruptedException ex) {
            return Response.serverError().build();
        }
    }

    @PUT
    @Path(SYNC_METHOD)
    public Response sync() {
        LOGGER.info("call of sync method");
        try {
            syncLatch.await();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Expecting the latch will be succesfully released on long latency LRA is in progress");
        }
        return Response.ok().build();
    }
}
