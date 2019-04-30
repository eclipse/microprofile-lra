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

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.tck.participant.activity.Activity;
import org.eclipse.microprofile.lra.tck.participant.api.LraController;
import org.eclipse.microprofile.lra.tck.participant.api.Util;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.ACCEPT_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.RECOVERY_PARAM;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.TCK_PARTICIPANT_RESOURCE_PATH;
import static org.junit.Assert.assertEquals;

public class TckTestBase {
    private static final Logger LOGGER = Logger.getLogger(TckTestBase.class.getName());
    private static final Long RECOVERY_CHECK_INTERVAL = 10L; // check for recovery every 10 seconds

    @Rule public TestName testName = new TestName();

    @Inject
    private LraTckConfigBean config;

    LRAClientOps lraClient;

    private static Client tckSuiteClient;

    WebTarget tckSuiteTarget;

    static WebArchive deploy(String archiveName) {
        return ShrinkWrap
            .create(WebArchive.class, archiveName + ".war")
            .addPackages(false, TckTestBase.class.getPackage(),
                Activity.class.getPackage(),
                LraController.class.getPackage())
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @AfterClass
    public static void afterClass() {
        if(tckSuiteClient != null) {
            tckSuiteClient.close();
        }
    }

    @Before
    public void before() {
        LOGGER.info("Running test: " + testName.getMethodName());
        setUpTestCase();

        try {
            tckSuiteTarget = tckSuiteClient.target(URI.create(new URL(config.tckSuiteBaseUrl()).toExternalForm()));
            lraClient = new LRAClientOps(tckSuiteTarget);
        } catch (MalformedURLException mfe) {
            throw new IllegalStateException("Cannot create URL for the LRA TCK suite base url " + config.tckSuiteBaseUrl(), mfe);
        }
    }

    /**
     * Checking if coordinator is running, set ups the client to contact the recovery manager and the TCK suite itself.
     */
    private void setUpTestCase() {
        tckSuiteClient = ClientBuilder.newClient();
    }

    void checkStatusAndCloseResponse(Response.Status expectedStatus, Response response, WebTarget resourcePath) {
        try {
            assertEquals("Not expected status at call '" + resourcePath.getUri() + "'",
                    expectedStatus.getStatusCode(), response.getStatus());
        } finally {
            response.close();
        }
    }

    String checkStatusReadAndCloseResponse(Response.Status expectedStatus, Response response, WebTarget resourcePath) {
        try {
            assertEquals("Response status on call to '" + resourcePath.getUri() + "' failed to match.",
                    expectedStatus.getStatusCode(), response.getStatus());
            return response.readEntity(String.class);
        } finally {
            response.close();
        }
    }

    /**
     * The started LRA will be named based on the class name and the running test name.
     */
    String lraClientId() {
        return this.getClass().getSimpleName() + "#" + testName.getMethodName();
    }

    /**
     * Adjusting the default timeout by the specified timeout factor
     * which can be defined by user.
     */
    long lraTimeout() {
        return Util.adjust(LraTckConfigBean.LRA_TIMEOUT_MILLIS, config.timeoutFactor());
    }

    /**
     * @see LraTckConfigBean#consistencyDelay
     */
    void applyConsistencyDelay() {
        if (config.getConsistencyDelay() > 0) {
            try {
                Thread.sleep(config.getConsistencyDelay());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Trigger or wait for a recovery scan to replay the protocol termination phase (complete or compensate)
     * on a participant that is in need of recovery.
     *
     * @param resource the resource on which recovery should be replayed. If null then recovery
     *                should be attempted on all eligible participants
     */
    void replayEndPhase(String resource) throws InterruptedException {
        // trigger a replay attempt on any participants
        // that have responded to complete/compensate requests with Response.Status.ACCEPTED

        /*
         * The following variable is for keeping track of the number of completion calls made
         * on the resource (TCK_PARTICIPANT_RESOURCE_PATH). It is initialised with the value 2
         * and then passed to the resource. We then loop asking the resource to report its
         * current value. The resource will decrement the value each time it is asked to complete.
         * When the return value hits zero we will know that recovery has happened.
         */
        int recoveryPasses = 2; // 1 for the normal end call and 2 for the recovery pass call
        long maxWait = config.recoveryTimeout(); // how long to wait before giving up on recovery
        WebTarget resourcePath = tckSuiteTarget.path(TCK_PARTICIPANT_RESOURCE_PATH).path(ACCEPT_PATH)
                .queryParam(RECOVERY_PARAM, recoveryPasses);
        Response response = resourcePath.request().put(Entity.text(""));

        checkStatusAndCloseResponse(Response.Status.OK, response, resourcePath);

        do {
            Invocation.Builder builder = resourcePath.request();

            if (config.isUseRecoveryHeader()) {
                builder.header(LRA.LRA_HTTP_RECOVERY_HEADER, resource);
            }

            response = resourcePath.request().get();

            // the resource will tell us how many more times it is expected to be asked to complete
            recoveryPasses = Integer.valueOf(checkStatusReadAndCloseResponse(Response.Status.OK, response, resourcePath));

            if (recoveryPasses == 0) {
                // success, recovery has happened
                if (resource == null) {
                    // recovery may still be running for other resources so linger a little longer
                    Thread.sleep(1000L);
                }

                return; // recovered successfully
            }

            LOGGER.info("replayEndPhase: recoveryPasses=" + recoveryPasses);

            Thread.sleep(RECOVERY_CHECK_INTERVAL * 1000); // delay before rechecking for recovery
            maxWait -= RECOVERY_CHECK_INTERVAL;
        } while (maxWait > 0L);

        throw new RuntimeException("TckTestBase: recovery was not triggered for resource " + resource);
    }
}
