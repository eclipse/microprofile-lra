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

import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid.ValidLRACSParticipant;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.valid.ValidLRAParticipant;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;

/**
 * TCK to verify that valid non-JAX-RS participant method signatures are respected
 */
@RunWith(Arquillian.class)
public class TckParticipantTests extends TckTestBase {
    
    private static final String VALID_DEPLOYMENT = "valid-deploy";

    private int beforeCompletedCount;
    private int beforeCompensatedCount;
    private int beforeStatusCount;
    private int beforeForgetCount;
    
    @Deployment(name = VALID_DEPLOYMENT)
    public static WebArchive deployValidParticipant() {
        return TckTestBase.deploy(VALID_DEPLOYMENT)
            .addPackage(ValidLRAParticipant.class.getPackage());
    }
    
    @Before
    public void before() {
        super.before();

        beforeCompensatedCount = getCompensateCount();
        beforeCompletedCount = getCompleteCount();
        beforeStatusCount = getStatusCount();
        beforeForgetCount = getForgetCount();
    }

    /**
     * Test verifies that non-JAX-RS @Complete method is invoked according to the 
     * LRA protocol and that if {@link javax.ws.rs.WebApplicationException} is 
     * thrown inside of a non-JAX-RS participant method than {@link Response} it 
     * is carrying is extracted and acted upon accoding to LRA response handling
     */
    @Test
    public void validWebApplicationExceptionReturnedTest() {
        Response response = tckSuiteTarget.path(ValidLRAParticipant.RESOURCE_PATH)
            .path(ValidLRAParticipant.ENLIST_WITH_COMPLETE)
            .request()
            .get();

        assertEquals("The 200 status response is expected", 
            Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Non JAX-RS @Complete method throwing WebApplicationException shoud have been called", 
            beforeCompletedCount + 1, getCompleteCount());
        assertEquals("@Compensate method should not have been called as LRA completed succesfully",
            beforeCompensatedCount, getCompensateCount());

    }

    /**
     * This test verifies chained call of non-JAX-RS participant methods. First 
     * it starts and cancels a new LRA. @Compensate non-JAX-RS method then returns 
     * {@link org.eclipse.microprofile.lra.annotation.ParticipantStatus#Compensating} 
     * (see {@link ValidLRAParticipant}) indicating that non-JAX-RS @Status method should 
     * be invoked. The test then waits for recovery and then verifies that @Status method is 
     * called. This method finishes compensation with return of the {@link Response} object 
     * indicating failure and so the test then verifies that non-JAX-RS @Forget method has 
     * also been called.
     */
    @Test
    public void validSignaturesChainTest() throws InterruptedException {
        Response response = tckSuiteTarget.path(ValidLRAParticipant.RESOURCE_PATH)
            .path(ValidLRAParticipant.ENLIST_WITH_COMPENSATE)
            .request()
            .get();

        assertEquals("The 500 status response is expected", 
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("Non JAX-RS @Compensate method should have been called",
            beforeCompensatedCount + 1, getCompensateCount());
        assertEquals("@Complete method should not have been called as LRA compensated",
            beforeCompletedCount, getCompleteCount());
        
        replayEndPhase(ValidLRAParticipant.RESOURCE_PATH);
        
        assertEquals("Non JAX-RS @Status method should have been called", 
            beforeStatusCount + 1, getStatusCount());
        assertEquals("Non JAX-RS @Forget method should have been called",
            beforeForgetCount + 1, getForgetCount());

    }

    /**
     * Test verifies {@link java.util.concurrent.CompletionStage} parametrized with 
     * {@link Void} as valid non-JAX-RS participant method return type
     */
    @Test
    public void testNonJaxRsCompletionStageVoid() {
        int beforeCompletedCount = getCompleteCount(ValidLRACSParticipant.ROOT_PATH);
        int beforeCompensateCount = getCompensateCount(ValidLRACSParticipant.ROOT_PATH);
        
        Response response = tckSuiteTarget.path(ValidLRACSParticipant.ROOT_PATH)
            .path(ValidLRACSParticipant.ENLIST_WITH_COMPENSATE)
            .request()
            .get();

        assertEquals("The 500 status response is expected",
            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("Non JAX-RS @Compensate method with CompletionStage<Void> should have been called",
            beforeCompensateCount + 1, getCompensateCount(ValidLRACSParticipant.ROOT_PATH));
        assertEquals("Non JAX-RS @Complete method should have not been called",
            beforeCompletedCount, getCompleteCount(ValidLRACSParticipant.ROOT_PATH));
    }

    /**
     * Test verifyies {@link java.util.concurrent.CompletionStage} parametrized by 
     * {@link Response} and {@link org.eclipse.microprofile.lra.annotation.ParticipantStatus} as valid
     * non-JAX-RS participant methods return types
     */
    @Test
    public void testNonJaxRsCompletionStageResponseAndParticipantStatus() throws InterruptedException {
        int beforeCompletedCount = getCompleteCount(ValidLRACSParticipant.ROOT_PATH);
        int beforeCompensateCount = getCompensateCount(ValidLRACSParticipant.ROOT_PATH);
        int beforeStatusCount = getStatusCount(ValidLRACSParticipant.ROOT_PATH);

        Response response = tckSuiteTarget.path(ValidLRACSParticipant.ROOT_PATH)
            .path(ValidLRACSParticipant.ENLIST_WITH_COMPLETE)
            .request()
            .get();

        assertEquals("The 200 status response is expected",
            Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Non JAX-RS @Complete method with CompletionStage<Response> should have been called",
            beforeCompletedCount + 1, getCompleteCount(ValidLRACSParticipant.ROOT_PATH));
        assertEquals("Non JAX-RS @Compensate method should have not been called",
            beforeCompensateCount, getCompensateCount(ValidLRACSParticipant.ROOT_PATH));
        
        replayEndPhase(ValidLRACSParticipant.ROOT_PATH);
        
        assertEquals("Non JAX-RS @Status method with CompletionStage<ParticipantStatus> should have been called",
            beforeStatusCount + 1, getStatusCount(ValidLRACSParticipant.ROOT_PATH));
    }

    private int getCompleteCount() {
        return getCompleteCount(ValidLRAParticipant.RESOURCE_PATH);
    }

    private int getCompensateCount() {
        return getCompensateCount(ValidLRAParticipant.RESOURCE_PATH);
    }

    private int getStatusCount() {
        return getStatusCount(ValidLRAParticipant.RESOURCE_PATH);
    }

    private int getForgetCount() {
        Response response = tckSuiteTarget.path(ValidLRAParticipant.RESOURCE_PATH)
            .path(ValidLRAParticipant.FORGET_COUNT_PATH).request().get();
        return response.readEntity(Integer.class);
    }

    private int getCompleteCount(String path) {
        Response response = tckSuiteTarget.path(path)
            .path(ValidLRAParticipant.COMPLETED_COUNT_PATH).request().get();
        return response.readEntity(Integer.class);
    }

    private int getCompensateCount(String path) {
        Response response = tckSuiteTarget.path(path)
            .path(ValidLRAParticipant.COMPENSATED_COUNT_PATH).request().get();
        return response.readEntity(Integer.class);
    }

    private int getStatusCount(String path) {
        Response response = tckSuiteTarget.path(path)
            .path(ValidLRAParticipant.STATUS_COUNT_PATH).request().get();
        return response.readEntity(Integer.class);
    }
}
