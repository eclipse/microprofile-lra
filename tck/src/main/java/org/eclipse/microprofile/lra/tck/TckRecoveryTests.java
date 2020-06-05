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

import org.eclipse.microprofile.lra.tck.participant.api.RecoveryResource;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.eclipse.microprofile.lra.tck.service.spi.LRACallbackException;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Logger;

/**
 * Tests for the recovery of failed LRA services. Test that LRA functions properly even in case of service failures.
 *
 * As this test is not running a managed Arquillian deployment, CDI is not working inside of this class. This is
 * why the test execution is moved into {@link RecoveryResource} in two phases (see {@link RecoveryResource#PHASE_1} and
 * {@link RecoveryResource#PHASE_2}) which also verify the results and pass the results in HTTP response.
 *
 * Note that this test relies on Arquillian to supply the deployment URL via the {@link ArquillianResource} injection.
 */
@RunWith(Arquillian.class)
public class TckRecoveryTests {
    
    public static final String LRA_TCK_DEPLOYMENT_URL = "LRA-TCK-Deployment-URL";

    private static final String DEPLOYMENT_NAME = "tck-recovery";
    private static final Logger LOGGER = Logger.getLogger(TckRecoveryTests.class.getName());

    @ArquillianResource
    private Deployer deployer;

    @ArquillianResource
    private URL deploymentURL;

    private LRARecoveryService lraRecoveryService;

    private Client deploymentClient;
    private WebTarget deploymentTarget;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() throws URISyntaxException {
        LOGGER.info("Running test: " + testName.getMethodName());

        deploymentClient = ClientBuilder.newClient();
        deploymentTarget = deploymentClient.target(deploymentURL.toURI());
        lraRecoveryService = LRATestService.loadService(LRARecoveryService.class);
    }

    @After
    public void after() {
        deploymentClient.close();
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    public static WebArchive deploy() {
        return TckTestBase.deploy(DEPLOYMENT_NAME);
    }

    /**
     * This test verifies that if the microservice application fails after
     * it enlists with a LRA and then it is restarted again the Compensate
     * callbacks are still received correctly.
     *
     * Scenario:
     * - start a new container with a single LRA resource
     * - start a new LRA and enlist LRA resource
     * - kill the container/application
     * - start the container/application
     * = cancel the LRA and verify that the callbacks have been sent
     */
    @Test
    public void testCancelWhenParticipantIsRestarted() {
        // deploy the test service
        deployer.deploy(DEPLOYMENT_NAME);

        // invoke phase 1 inside the container (start new LRA and enlist resource)
        Response response1 = deploymentTarget
            .path(RecoveryResource.RECOVERY_RESOURCE_PATH)
            .path(RecoveryResource.PHASE_1)
            .request()
            .header(LRA_TCK_DEPLOYMENT_URL, deploymentURL.toExternalForm())
            .get();
        
        Assert.assertEquals(200, response1.getStatus());
        URI lra = URI.create(response1.readEntity(String.class));

        // kill the test service while LRA is still active
        deployer.undeploy(DEPLOYMENT_NAME);

        // start the test service again
        deployer.deploy(DEPLOYMENT_NAME);

        // invoke phase 2 inside the container (cancel the LRA and verify that the
        // @Compensate method was called on the enlisted resource)
        Response response2 = deploymentTarget.path(RecoveryResource.RECOVERY_RESOURCE_PATH)
                .path(RecoveryResource.PHASE_2)
                .request()
                .header(RecoveryResource.LRA_HEADER, lra)
                .header(LRA_TCK_DEPLOYMENT_URL, deploymentURL.toExternalForm())
                .get();

        Assert.assertEquals(response2.readEntity(String.class), 200, response2.getStatus());

        deployer.undeploy(DEPLOYMENT_NAME);
    }

    /**
     * This test verifies that if the microservice application which
     * enlisted with the LRA fails and the LRA is ended during the time
     * the service is still down, the Compensate callbacks are received
     * when the microservice application is started again.
     *
     * Scenario:
     * - start a new container with a single LRA resource
     * - start a new LRA and enlist the LRA resource with it
     * - kill the container
     * - cancel the LRA while the container is still down
     * - start the container again
     * - replay the end phase to get Compensate calls redelivered
     * - verify that the Compensate callbacks have been received
     */
    @Test
    public void testCancelWhenParticipantIsUnavailable() {
        // deploy the test service
        deployer.deploy(DEPLOYMENT_NAME);

        // invoke phase 1 inside the container (start new LRA and enlist resource) which cancels after 500 millis
        Response response1 = deploymentTarget.path(RecoveryResource.RECOVERY_RESOURCE_PATH)
            .path(RecoveryResource.PHASE_1)
            .queryParam("timeout", true)
            .request()
            .header(LRA_TCK_DEPLOYMENT_URL, deploymentURL.toExternalForm())
            .get();

        Assert.assertEquals(200, response1.getStatus());
        URI lra = URI.create(response1.readEntity(String.class));

        // kill the test service while LRA is still active
        deployer.undeploy(DEPLOYMENT_NAME);

        // Wait for the timeout cancellation of the LRA. This will put the LRA into cancel only state.
        // Then wait for the short delay to actually perform the cancellation while the service is still down.
        // Compensate should be attempted to be called while the participant service is down
        try {
            Thread.sleep(RecoveryResource.LRA_TIMEOUT);
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }

        // wait for the Compensate call to be delivered
        try {
            lraRecoveryService.waitForCallbacks(lra);
        } catch (LRACallbackException e) {
            Assert.fail(e.getMessage());
        }

        // start the test service again
        deployer.deploy(DEPLOYMENT_NAME);

        // trigger recovery causing the Compensate call to be replayed
        deploymentTarget.path(RecoveryResource.RECOVERY_RESOURCE_PATH)
                .path(RecoveryResource.TRIGGER_RECOVERY)
                .request()
                .header(RecoveryResource.LRA_HEADER, lra)
                .header(LRA_TCK_DEPLOYMENT_URL, deploymentURL.toExternalForm())
                .get();

        // invoke phase 2 inside the container (execute checks that verify that callbacks have been called)
        Response response2 = deploymentTarget.path(RecoveryResource.RECOVERY_RESOURCE_PATH)
                .path(RecoveryResource.PHASE_2)
                .request()
                .header(RecoveryResource.LRA_HEADER, lra)
                .header(LRA_TCK_DEPLOYMENT_URL, deploymentURL.toExternalForm())
                .get();

        Assert.assertEquals(response2.readEntity(String.class), 200, response2.getStatus());

        deployer.undeploy(DEPLOYMENT_NAME);
    }
}
