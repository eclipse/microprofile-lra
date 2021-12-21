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

import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid.LongBusinessMethodParticipant;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid.ValidLRACSParticipant;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid.ValidLRAParticipant;
import org.eclipse.microprofile.lra.tck.service.LRAMetricAssertions;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

/**
 * TCK to verify that valid non-JAX-RS participant method signatures are respected
 */
@RunWith(Arquillian.class)
public class TckParticipantTests extends TckTestBase {

    private static final String VALID_DEPLOYMENT = "valid-deploy";
    private static final Logger LOGGER = Logger.getLogger(TckParticipantTests.class.getName());

    @Inject
    private LRAMetricAssertions lraMetric;

    @Inject
    private LRATestService lraTestService;

    @Deployment
    public static WebArchive deployValidParticipant() {
        return TckTestBase.deploy(VALID_DEPLOYMENT)
                .addPackage(ValidLRAParticipant.class.getPackage());
    }

    /**
     * Test verifies that non-JAX-RS @Complete method is invoked according to the LRA protocol and that if
     * {@link WebApplicationException} is thrown inside of a non-JAX-RS participant method than {@link Response} it is
     * carrying is extracted and acted upon according to LRA response handling
     */
    @Test
    public void validWebApplicationExceptionReturnedTest() {
        WebTarget resourcePath = tckSuiteTarget.path(ValidLRAParticipant.RESOURCE_PATH)
                .path(ValidLRAParticipant.ENLIST_WITH_COMPLETE);

        Response response = resourcePath.request().get();
        URI lraId = URI.create(checkStatusReadAndCloseResponse(Response.Status.OK, response, resourcePath));

        lraMetric.assertCompletedEquals(
                "Non JAX-RS @Complete method throwing WebApplicationException shoud have been called",
                1, lraId, ValidLRAParticipant.class);
        lraMetric.assertNotCompensated("@Compensate method should not have been called as LRA completed succesfully",
                lraId, ValidLRAParticipant.class);

        /*
         * Validate that a resource can receive notifications of the final outcome of an LRA using the {@link
         * org.eclipse.microprofile.lra.annotation.AfterLRA} annotation on a non JAX-RS method
         */
        lraMetric.assertAfterLRA("@AfterLRA method should have been called", lraId, ValidLRAParticipant.class);
    }

    /**
     * This test verifies chained call of non-JAX-RS participant methods. First it starts and cancels a new
     * LRA. @Compensate non-JAX-RS method then returns
     * {@link org.eclipse.microprofile.lra.annotation.ParticipantStatus#Compensating} (see {@link ValidLRAParticipant})
     * indicating that non-JAX-RS @Status method should be invoked. The test then waits for recovery and then verifies
     * that @Status method is called. This method finishes compensation with return of the {@link Response} object
     * indicating failure and so the test then verifies that non-JAX-RS @Forget method has also been called.
     *
     * @throws InterruptedException
     *             When Test is interrupted during sleep.
     */
    @Test
    public void validSignaturesChainTest() throws InterruptedException {
        WebTarget resourcePath = tckSuiteTarget.path(ValidLRAParticipant.RESOURCE_PATH)
                .path(ValidLRAParticipant.ENLIST_WITH_COMPENSATE);

        Response response = resourcePath.request().get();

        URI lraId = URI
                .create(checkStatusReadAndCloseResponse(Response.Status.INTERNAL_SERVER_ERROR, response, resourcePath));

        lraMetric.assertCompensatedEquals("Non JAX-RS @Compensate method should have been called",
                1, lraId, ValidLRAParticipant.class);
        lraMetric.assertNotCompleted("@Complete method should not have been called as LRA compensated",
                lraId, ValidLRAParticipant.class);

        lraTestService.waitForRecovery(lraId);

        lraMetric.assertStatus("Non JAX-RS @Status method should have been called", lraId, ValidLRAParticipant.class);
        lraMetric.assertForget("Non JAX-RS @Forget method should have been called", lraId, ValidLRAParticipant.class);
    }

    /**
     * Test verifies {@link java.util.concurrent.CompletionStage} parametrized with {@link Void} as valid non-JAX-RS
     * participant method return type
     *
     * @throws InterruptedException
     *             when waiting for the finishing the completion is interrupted
     */
    @Test
    public void testNonJaxRsCompletionStageVoid() throws InterruptedException {
        WebTarget resourcePath = tckSuiteTarget.path(ValidLRACSParticipant.ROOT_PATH)
                .path(ValidLRACSParticipant.ENLIST_WITH_COMPENSATE);

        Response response = resourcePath.request().get();

        URI lraId = URI
                .create(checkStatusReadAndCloseResponse(Response.Status.INTERNAL_SERVER_ERROR, response, resourcePath));

        lraMetric.assertCompensatedEquals(
                "Non JAX-RS @Compensate method with CompletionStage<Void> should have been called",
                1, lraId, ValidLRACSParticipant.class);
        lraMetric.assertNotCompleted("Non JAX-RS @Complete method should have not been called",
                lraId, ValidLRACSParticipant.class);

        lraTestService.waitForRecovery(lraId);
    }

    /**
     * Test verifyies {@link java.util.concurrent.CompletionStage} parametrized by {@link Response} and
     * {@link org.eclipse.microprofile.lra.annotation.ParticipantStatus} as valid non-JAX-RS participant methods return
     * types
     *
     * @throws InterruptedException
     *             When Test is interrupted during sleep.
     */
    @Test
    public void testNonJaxRsCompletionStageResponseAndParticipantStatus() throws InterruptedException {
        WebTarget resourcePath = tckSuiteTarget.path(ValidLRACSParticipant.ROOT_PATH)
                .path(ValidLRACSParticipant.ENLIST_WITH_COMPLETE);

        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Response.Status.OK, response, resourcePath));

        lraMetric.assertCompletedEquals(
                "Non JAX-RS @Complete method with CompletionStage<Response> should have been called",
                1, lraId, ValidLRACSParticipant.class);
        lraMetric.assertNotCompensated("Non JAX-RS @Compensate method should have not been called",
                lraId, ValidLRACSParticipant.class);

        lraTestService.waitForRecovery(lraId);

        lraMetric.assertStatus(
                "Non JAX-RS @Status method with CompletionStage<ParticipantStatus> should have been called",
                lraId, ValidLRACSParticipant.class);

        lraTestService.waitForRecovery(lraId);
    }

    @Test
    public void cancelLraDuringBusinessMethod() throws InterruptedException, ExecutionException, TimeoutException {
        LRAClientOps lraOps = lraTestService.getLRAClient();
        URI lraId = lraOps.startLRA(null, lraClientId(), 0L, ChronoUnit.MILLIS);
        LOGGER.info(String.format("Started LRA with URI %s", lraId));
        // Start business method asynchronously, i.e. return immediately.
        Future<Response> lraFuture = tckSuiteTarget.path(LongBusinessMethodParticipant.ROOT_PATH)
                .path(LongBusinessMethodParticipant.BUSINESS_METHOD)
                .request()
                .header(LRA.LRA_HTTP_CONTEXT_HEADER, lraId)
                .async()
                .put(Entity.text(""));

        // Make sure that when we cancel the LRA, the participant is waiting in the business method.
        Response syncMethodResponse = tckSuiteTarget.path(LongBusinessMethodParticipant.ROOT_PATH)
                .path(LongBusinessMethodParticipant.SYNC_METHOD)
                .request()
                .put(Entity.text(""));
        Assert.assertEquals("Endpoint " + LongBusinessMethodParticipant.SYNC_METHOD + " failed execution",
                200, syncMethodResponse.getStatus());
        // -1 indicates that the LRAMetricType.Compensated key is not yet present in the metrics Map.
        // This in turn means that @Compensate could not have been called yet.
        lraMetric.assertNotCompleted("Business method is in progress and @Complete has not been expected",
                lraId, LongBusinessMethodParticipant.class);
        lraMetric.assertNotCompensated("Business method is in progress and @Compensate can't be called " +
                "as cancelation has not been invoked yet", lraId, LongBusinessMethodParticipant.class);
        LOGGER.info(String.format("Cancelled LRA with URI %s", lraId));
        lraOps.cancelLRA(lraId);
        // waiting for the LRA to be finished
        lraTestService.waitForRecovery(lraId);
        // participant has to be compensated
        lraMetric.assertCompensated("@Compensate method should have been called at least once " +
                "as cancel was invoked", lraId, LongBusinessMethodParticipant.class);

        Response response = lraFuture.get(lraTimeout(), TimeUnit.MILLISECONDS);
        Assert.assertEquals(LongBusinessMethodParticipant.class.getSimpleName() + "'s business method is expected " +
                "to finish successfully despite the delay.", 200, response.getStatus());
    }
}
