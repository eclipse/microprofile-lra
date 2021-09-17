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

import static org.hamcrest.MatcherAssert.assertThat;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.lra.tck.participant.api.RecoveryResource;
import org.eclipse.microprofile.lra.tck.service.LRAMetricRest;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.hamcrest.Matchers;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
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

/**
 * <p>
 * Tests for the recovery of failed LRA services. Test that LRA functions properly even in case of service failures.
 * </p>
 * <p>
 * This test is meant to be run in "run as client mode controlling the behaviour not via CDI injection but via HTTP
 * calls. The @{@link Deployment} is defined as <code>managed = false</code> which means that Arquillian does not
 * automatically deploy the deployment at the start of the test and the test control this step on its own.
 * </p>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class TckRecoveryTests {
    private static final Logger LOG = Logger.getLogger(TckRecoveryTests.class.getName());

    private static final String DEPLOYMENT_NAME = "tck-recovery";
    private static final Logger LOGGER = Logger.getLogger(TckRecoveryTests.class.getName());

    @ArquillianResource
    private Deployer deployer;

    private LRATestService lraTestService;
    private Client deploymentClient;
    private WebTarget deploymentTarget;

    @Rule
    public TestName testName = new TestName();

    @Before
    public void before() {
        LOGGER.info("Running test: " + testName.getMethodName());
        // deploy the test service
        deployer.deploy(DEPLOYMENT_NAME);
    }

    @After
    public void after() {
        try {
            deployer.undeploy(DEPLOYMENT_NAME);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Cannot undeploy the deployment " + DEPLOYMENT_NAME +
                    " at the end of the test " + testName, e);
        }
        deploymentClient.close();
        lraTestService.stop();
    }

    @Deployment(name = DEPLOYMENT_NAME, managed = false)
    public static WebArchive deploy() {
        return TckTestBase.deploy(DEPLOYMENT_NAME);
    }

    /**
     * This test verifies that if the microservice application fails after it enlists with a LRA and then it is
     * restarted again the Compensate callbacks are still received correctly.
     *
     * Scenario: - start a new container with a single LRA resource - start a new LRA and enlist LRA resource - kill the
     * container/application - start the container/application = cancel the LRA and verify that the callbacks have been
     * sent
     *
     * @param deploymentURL
     *            the URL of the arquillian deployment
     */
    @Test
    public void testCancelWhenParticipantIsRestarted(@ArquillianResource URL deploymentURL) {
        clientServiceSetup(deploymentURL);

        // starting and enlisting to LRA
        Response response = deploymentTarget
                .path(RecoveryResource.RECOVERY_RESOURCE_PATH)
                .path(RecoveryResource.REQUIRED_PATH)
                .request().put(Entity.text(""));

        Assert.assertEquals(200, response.getStatus());
        URI lra = URI.create(response.readEntity(String.class));

        // kill the test service while LRA is still active
        deployer.undeploy(DEPLOYMENT_NAME);

        // start the test service again
        deployer.deploy(DEPLOYMENT_NAME);

        // Cancel the LRA and verify that the @Compensate method was called on the enlisted resource
        lraTestService.getLRAClient().cancelLRA(lra);
        lraTestService.waitForCallbacks(lra);

        assertMetricCallbackCalled(LRAMetricType.Compensated, lra);
        assertMetricCallbackCalled(LRAMetricType.Cancelled, lra);

    }

    /**
     * This test verifies that if the microservice application which enlisted with the LRA fails and the LRA is ended
     * during the time the service is still down, the Compensate callbacks are received when the microservice
     * application is started again.
     *
     * Scenario: - start a new container with a single LRA resource - start a new LRA and enlist the LRA resource with
     * it - kill the container - cancel the LRA while the container is still down - start the container again - replay
     * the end phase to get Compensate calls redelivered - verify that the Compensate callbacks have been received
     *
     * @param deploymentURL
     *            the URL of the arquillian deployment
     */
    @Test
    public void testCancelWhenParticipantIsUnavailable(@ArquillianResource URL deploymentURL) {
        clientServiceSetup(deploymentURL);

        // starting and enlisting to LRA
        Response response = deploymentTarget
                .path(RecoveryResource.RECOVERY_RESOURCE_PATH)
                .path(RecoveryResource.REQUIRED_TIMEOUT_PATH)
                .request().put(Entity.text(""));

        Assert.assertEquals(200, response.getStatus());
        URI lra = URI.create(response.readEntity(String.class));

        // kill the test service while LRA is still active
        deployer.undeploy(DEPLOYMENT_NAME);

        // Wait for the timeout cancellation of the LRA. This will put the LRA into cancel only state.
        // Then wait for the short delay to actually perform the cancellation while the service is still down.
        // Compensate should be attempted to be called while the participant service is down
        try {
            Thread.sleep(adjustTimeoutByDefaultFactor(RecoveryResource.LRA_TIMEOUT));
        } catch (InterruptedException e) {
            LOG.log(Level.SEVERE, "Sleep LRA timeout interrupted", e);
            Assert.fail("Sleeping LRA timeout " + RecoveryResource.LRA_TIMEOUT + " was interrupted: " + e.getMessage());
        }
        // wait for the Compensate call to be delivered
        lraTestService.waitForCallbacks(lra);

        // start the test service again
        deployer.deploy(DEPLOYMENT_NAME);

        // trigger recovery causing the Compensate call to be replayed
        lraTestService.waitForRecovery(lra);

        // execute checks that verify that callbacks have been called
        lraTestService.waitForCallbacks(lra);

        assertMetricCallbackCalled(LRAMetricType.Compensated, lra);
        assertMetricCallbackCalled(LRAMetricType.Cancelled, lra);
    }

    private void assertMetricCallbackCalled(LRAMetricType metricType, URI lra) {
        Response responseMetric = deploymentTarget
                .path(LRAMetricRest.LRA_TCK_METRIC_RESOURCE_PATH)
                .path(LRAMetricRest.METRIC_PATH)
                .queryParam(LRAMetricRest.METRIC_TYPE_PARAM, metricType)
                .queryParam(LRAMetricRest.LRA_ID_PARAM, lra)
                .queryParam(LRAMetricRest.PARTICIPANT_NAME_PARAM, RecoveryResource.class.getName())
                .request().get();
        Assert.assertEquals("Expect the metric REST call to " + responseMetric.getLocation()
                + " to succeed", 200, responseMetric.getStatus());
        int metricNumber = responseMetric.readEntity(Integer.class);
        assertThat("Expecting the metric " + metricType + " callback was called",
                metricNumber, Matchers.greaterThanOrEqualTo(1));
    }

    /**
     * A helper method which is capable to adjust timeout value by factor obtained from system property
     * {@code LraTckConfigBean#LRA_TCK_TIMEOUT_FACTOR_PROPETY_NAME}.
     */
    private long adjustTimeoutByDefaultFactor(long timeout) {
        String timeoutFactor = System.getProperty(LraTckConfigBean.LRA_TCK_TIMEOUT_FACTOR_PROPETY_NAME, "1.0");
        return (long) Math.ceil(timeout * Double.parseDouble(timeoutFactor));
    }

    private void clientServiceSetup(URL deploymentURL) {
        try {
            deploymentClient = ClientBuilder.newClient();
            deploymentTarget = deploymentClient.target(deploymentURL.toURI());
            lraTestService = new LRATestService();
            lraTestService.start(deploymentURL);
        } catch (URISyntaxException use) {
            throw new IllegalStateException("Cannot create URI from deployment URL " + deploymentURL, use);
        }
    }
}
