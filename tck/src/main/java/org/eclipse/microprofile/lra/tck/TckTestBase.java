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

import org.eclipse.microprofile.lra.tck.participant.activity.Activity;
import org.eclipse.microprofile.lra.tck.participant.api.LraResource;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.net.URL;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class TckTestBase {
    private static final Logger LOGGER = Logger.getLogger(TckTestBase.class.getName());

    @Rule public TestName testName = new TestName();

    @Inject
    private LraTckConfigBean config;
    
    @Inject
    private LRATestService lraTestService;

    @Inject
    private LRAMetricService lraMetricService;
    
    @ArquillianResource
    private URL deploymentURL;

    LRAClientOps lraClient;

    WebTarget tckSuiteTarget;

    static WebArchive deploy(String archiveName) {
        return ShrinkWrap
            .create(WebArchive.class, archiveName + ".war")
            .addPackages(false, TckTestBase.class.getPackage(),
                Activity.class.getPackage(),
                LraResource.class.getPackage(),
                LRAMetricService.class.getPackage(),
                LRARecoveryService.class.getPackage())
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @After
    public void after() {
        lraTestService.stop();
    }

    @Before
    public void before() {
        LOGGER.info("Running test: " + testName.getMethodName());
        
        lraTestService.start(deploymentURL);
        lraMetricService.clear();
        this.lraClient = lraTestService.getLRAClient();
        this.tckSuiteTarget = lraTestService.getTCKSuiteTarget();
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
        if(LraTckConfigBean.LRA_TIMEOUT_MILLIS < 0){
            throw new IllegalArgumentException("value of timeout can't be negative");
        }
        return (long) Math.ceil(LraTckConfigBean.LRA_TIMEOUT_MILLIS * config.timeoutFactor());
    }
}
