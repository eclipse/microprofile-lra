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
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class TckTestBase {
    private static final Logger LOGGER = Logger.getLogger(TckTestBase.class.getName());

    @Rule public TestName testName = new TestName();

    @Inject
    private LraTckConfigBean config;

    LRAClientOps lraClient;

    private static URL recoveryCoordinatorBaseUrl;
    private static Client tckSuiteClient;
    private static Client recoveryCoordinatorClient;

    WebTarget tckSuiteTarget;
    private WebTarget recoveryTarget;

    static WebArchive deploy(String archiveName) {
        return ShrinkWrap
            .create(WebArchive.class, archiveName + ".war")
            .addPackages(true, "org.eclipse.microprofile.lra.tck")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @AfterClass
    public static void afterClass() {
        if(tckSuiteClient != null) {
            tckSuiteClient.close();
        }
        if(recoveryCoordinatorClient != null) {
            recoveryCoordinatorClient.close();
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
        recoveryTarget = recoveryCoordinatorClient.target(URI.create(recoveryCoordinatorBaseUrl.toExternalForm()));
    }

    /**
     * Checking if coordinator is running, set ups the client to contact the recovery manager and the TCK suite itself.
     */
    private void setUpTestCase() {
        if(recoveryCoordinatorBaseUrl != null) {
            // we've already set up the recovery urls and REST clients for the tests
            return;
        }

        try {
            // see https://github.com/eclipse/microprofile-lra/issues/116
            recoveryCoordinatorBaseUrl = new URL(String.format("http://%s:%d/%s",
                    config.recoveryHostName(), config.recoveryPort(), config.recoveryPath()));

            tckSuiteClient = ClientBuilder.newClient();
            recoveryCoordinatorClient = ClientBuilder.newClient();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Cannot properly setup the TCK tests (coordinator endpoint, testsuite endpoints...)", e);
        }
    }

    private void checkStatusAndCloseResponse(Response.Status expectedStatus, Response response, WebTarget resourcePath) {
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
    protected String lraClientId() {
        return this.getClass().getSimpleName() + "#" + testName.getMethodName();
    }

    /**
     * Adjusting the default timeout by the specified timeout factor
     * which can be defined by user.
     */
    protected long lraTimeout() {
        return Util.adjust(LraTckConfigBean.LRA_TIMEOUT_MILLIS, config.timeoutFactor());
    }


    void replayEndPhase(String resource) {
        // trigger a recovery scan which trigger a replay attempt on any participants
        // that have responded to complete/compensate requests with Response.Status.ACCEPTED
        WebTarget recoveryPath = recoveryTarget.path("recovery");
        Response response2 = recoveryPath
                .request().get();

        checkStatusAndCloseResponse(Response.Status.OK, response2, recoveryPath);
    }
}
