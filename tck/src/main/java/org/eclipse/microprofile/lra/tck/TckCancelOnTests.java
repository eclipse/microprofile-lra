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

import static org.eclipse.microprofile.lra.tck.participant.api.LraCancelOnController.LRA_CANCEL_ON_CONTROLLER_PATH;
import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.lra.tck.participant.api.LraCancelOnController;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
@RunAsClient
public class TckCancelOnTests {
    private static final Logger LOGGER = Logger.getLogger(TckCancelOnTests.class.getName());


    @Rule public TestName testName = new TestName();

    private static final String TCK_SUITE_BASE_URL = System.getProperty("lra.tck.base.url", "http://localhost:8180");
    private static Client tckSuiteClient;

    private int beforeCompletedCount;
    private int beforeCompensatedCount;

    @Deployment(name = "tcktests-cancelon", managed = true, testable = true)
    public static WebArchive deploy() {
        String archiveName = TckCancelOnTests.class.getSimpleName().toLowerCase();
        return ShrinkWrap
            .create(WebArchive.class, archiveName + ".war")
            .addPackages(true, "org.eclipse.microprofile.lra.tck")
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @BeforeClass
    public static void beforeClass() {
        tckSuiteClient = ClientBuilder.newClient();
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
        this.beforeCompletedCount = getCompletedCount();
        this.beforeCompensatedCount = getCompensateCount();
    }

    private WebTarget getSuiteTarget() {
        if(TCK_SUITE_BASE_URL == null) {
            throw new NullPointerException("The test is not provided with the system property 'lra.tck.base.url'");
        }
        try {
            return tckSuiteClient.target(URI.create(new URL(TCK_SUITE_BASE_URL).toExternalForm()));
        } catch (MalformedURLException mfe) {
            throw new IllegalStateException("Cannot create REST test target for the LRA TCK suite base url "
                    + TCK_SUITE_BASE_URL, mfe);
        }
    }

    /**
     * See {@link LraCancelOnController#cancelOnFamilyDefault4xx()}
     */
    @Test
    public void cancelOnFamilyDefault4xx() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.CANCEL_ON_FAMILY_DEFAULT_4XX).request().get();
        assertEquals("The 400 status response is expected", Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("After 400 compensate should be invoked", beforeCompensatedCount + 1, getCompensateCount());
        assertEquals("After 400 complete can't be invoked", beforeCompletedCount, getCompletedCount());
    }

    /**
     * See {@link LraCancelOnController#cancelOnFamilyDefault5xx()}
     */
    @Test
    public void cancelOnFamilyDefault5xx() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.CANCEL_ON_FAMILY_DEFAULT_5XX).request().get();
        assertEquals("The 500 status response is expected", Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("After 500 compensate should be invoked", beforeCompensatedCount + 1, getCompensateCount());
        assertEquals("After 500 complete can't be invoked", beforeCompletedCount, getCompletedCount());
    }

    /**
     * See {@link LraCancelOnController#cancelOnFamily3xx()}
     */
    @Test
    public void cancelOnFamily3xx() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.CANCEL_ON_FAMILY_3XX).request().get();
        assertEquals("The 303 status response is expected", Status.SEE_OTHER.getStatusCode(), response.getStatus());
        assertEquals("After status code 303 is received, compensate should be invoked as set by attribute cancelOnFamily",
                beforeCompensatedCount + 1, getCompensateCount());
        assertEquals("After status code 303 is received, complete can't be invoked as not defined in annotation @LRA",
                beforeCompletedCount, getCompletedCount());
    }

    /**
     * See {@link LraCancelOnController#cancelOn301()}
     */
    @Test
    public void cancelOn301() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.CANCEL_ON_301).request().get();
        assertEquals("The 301 status response is expected", Status.MOVED_PERMANENTLY.getStatusCode(), response.getStatus());
        assertEquals("After status code 301 is received, compensate should be invoked as set by attribute cancelOn",
                beforeCompensatedCount + 1, getCompensateCount());
        assertEquals("After status code 301 is received, complete can't be invoked as not defined in annotation @LRA",
                beforeCompletedCount, getCompletedCount());
    }

    /**
     * See {@link LraCancelOnController#notCancelOnFamily5xx()}
     */
    @Test
    public void notCancelOnFamily5xx() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.NOT_CANCEL_ON_FAMILY_5XX).request().get();
        assertEquals("The 500 status response is expected", Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
        assertEquals("After status code 500 is received, compensate can't be invoked as default behaviour was changed",
                beforeCompensatedCount, getCompensateCount());
        assertEquals("After status code 500 is received, complete has to be called as default behaviour was changed",
                beforeCompletedCount + 1, getCompletedCount());
    }

    /**
     * See {@link LraCancelOnController#cancelFromRemoteCall()}
     */
    @Test
    public void cancelFromRemoteCall() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.CANCEL_FROM_REMOTE_CALL).request().get();
        assertEquals("The 200 status response is expected", Status.OK.getStatusCode(), response.getStatus());
        // LraCancelOnController enlists twice the same participant, compensate is expected to be called only once
        assertEquals("Status was 200 but compensate should be called as LRA should be cancelled for remotely called participant as well",
                beforeCompensatedCount + 1, getCompensateCount());
        assertEquals("Even the 200 status was received the remotly called participant should cause the LRA being cancelled",
                beforeCompletedCount, getCompletedCount());
    }



    private int getCompletedCount() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.COMPLETED_COUNT_PATH).request().get();
        return response.readEntity(Integer.class);
    }

    private int getCompensateCount() {
        Response response = getSuiteTarget().path(LRA_CANCEL_ON_CONTROLLER_PATH)
                .path(LraCancelOnController.COMPENSATED_COUNT_PATH).request().get();
        return Integer.valueOf(response.readEntity(String.class));
    }
}
