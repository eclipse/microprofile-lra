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
package org.eclipse.microprofile.lra.tck;

import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid.ValidLRAParticipantWithString;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * TCK to verify that valid non-JAX-RS participant method signatures are respected when using String as parameter type.
 */
@RunWith(Arquillian.class)
public class TckParticipantStringTests extends TckTestBase {

    private static final String VALID_DEPLOYMENT = "valid-deploy";

    @Inject
    private LRAMetricService lraMetricService;

    @Deployment(name = VALID_DEPLOYMENT)
    public static WebArchive deployValidParticipant() {
        return TckTestBase.deploy(VALID_DEPLOYMENT)
                .addPackage(ValidLRAParticipantWithString.class.getPackage());
    }

    /**
     * Test verifies that non-JAX-RS @Complete method is invoked according to the
     * LRA protocol and that if {@link javax.ws.rs.WebApplicationException} is
     * thrown inside of a non-JAX-RS participant method than {@link Response} it
     * is carrying is extracted and acted upon accoding to LRA response handling
     */
    @Test
    public void validWebApplicationExceptionReturnedTest() {
        WebTarget resourcePath = tckSuiteTarget.path(ValidLRAParticipantWithString.RESOURCE_PATH)
                .path(ValidLRAParticipantWithString.ENLIST_WITH_COMPLETE);

        Response response = resourcePath.request().get();
        URI lraId = URI.create(checkStatusReadAndCloseResponse(Response.Status.OK, response, resourcePath));

        assertEquals("Non JAX-RS @Complete method throwing WebApplicationException shoud have been called",
                1, lraMetricService.getMetric(LRAMetricType.COMPLETE, lraId));
        assertEquals("@Compensate method should not have been called as LRA completed succesfully",
                0, lraMetricService.getMetric(LRAMetricType.COMPENSATE, lraId));

    }

    /**
     * This test verifies chained call of non-JAX-RS participant methods. First
     * it starts and cancels a new LRA. @Compensate non-JAX-RS method then returns
     * {@link org.eclipse.microprofile.lra.annotation.ParticipantStatus#Compensating}
     * (see {@link ValidLRAParticipantWithString}) indicating that non-JAX-RS @Status method should
     * be invoked. The test then waits for recovery and then verifies that @Status method is
     * called. This method finishes compensation with return of the {@link Response} object
     * indicating failure and so the test then verifies that non-JAX-RS @Forget method has
     * also been called.
     */
    @Test
    public void validSignaturesChainTest() throws InterruptedException {
        WebTarget resourcePath = tckSuiteTarget.path(ValidLRAParticipantWithString.RESOURCE_PATH)
                .path(ValidLRAParticipantWithString.ENLIST_WITH_COMPENSATE);

        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Response.Status.INTERNAL_SERVER_ERROR, response, resourcePath));

        assertEquals("Non JAX-RS @Compensate method should have been called",
                1, lraMetricService.getMetric(LRAMetricType.COMPENSATE, lraId));
        assertEquals("@Complete method should not have been called as LRA compensated",
                0, lraMetricService.getMetric(LRAMetricType.COMPLETE, lraId));

        replayEndPhase(ValidLRAParticipantWithString.RESOURCE_PATH);

        assertEquals("Non JAX-RS @Status method should have been called",
                1, lraMetricService.getMetric(LRAMetricType.STATUS, lraId));
        assertEquals("Non JAX-RS @Forget method should have been called",
                1, lraMetricService.getMetric(LRAMetricType.FORGET, lraId));

    }

}
