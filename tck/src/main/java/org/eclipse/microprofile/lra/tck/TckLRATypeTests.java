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
import org.eclipse.microprofile.lra.tck.participant.api.Util;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.MANDATORY_WITH_END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.MANDATORY_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.NEVER_WITH_END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.NEVER_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.NOT_SUPPORTED_WITH_END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.NOT_SUPPORTED_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.REQUIRED_WITH_END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.REQUIRED_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.REQUIRES_NEW_WITH_END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.REQUIRES_NEW_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.SUPPORTS_WITH_END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.SUPPORTS_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.TCK_LRA_TYPE_RESOURCE_PATH;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * <p>
 * Tests that validate that the implementation of the {@link LRA.Type} attribute is correct
 * </p>
 *
 * <p>
 * For each of the type values there are 4 tests:
 * <ul>
 *     <li>start an LRA before invoking the resource method annotated with {@link LRA#end()} set to true</li>
 *     <li>do not start an LRA before invoking the resource method annotated with {@link LRA#end()} set to true</li>
 *     <li>start an LRA before invoking the resource method annotated with {@link LRA#end()} set to false</li>
 *     <li>do not start an LRA before invoking the resource method annotated with {@link LRA#end()} set to false</li>
 * </ul>
 *  *
 * <p>
 * Each test then validates {@link TckLRATypeTests#resourceRequest} that:
 * <ul>
 *     <li>the correct JAX-RS status was returned</li>
 *     <li>the resource method executed with the correct LRA context</li>
 *     <li>the LRA started by the resource method is closed if {@link LRA#end()} is true</li>
 *     <li>the LRA started by the resource method is not closed if {@link LRA#end()} is false</li>
 * </ul>
 */
@RunWith(Arquillian.class)
public class TckLRATypeTests {
    private static final Logger LOGGER = Logger.getLogger(TckLRATypeTests.class.getName());

    @Rule public TestName testName = new TestName();

    @Inject
    private LraTckConfigBean config;

    private LRAClientOps lraClient;

    private static Client tckSuiteClient;

    private WebTarget tckSuiteTarget;

    @Deployment(name = "lra-type-tck-tests", managed = true, testable = true)
    public static WebArchive deploy() {
        String archiveName = TckLRATypeTests.class.getSimpleName().toLowerCase();
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

    private void setUpTestCase() {
        tckSuiteClient = ClientBuilder.newClient();
    }

    // enum to indicate which checks to perform on the expected and actual active LRA after running a resource method
    private enum MethodLRACheck {
        NONE, NOT_PRESENT, EQUALS, NOT_EQUALS
    }

    @Test
    public void requiredWithLRA() {
        resourceRequest(REQUIRED_PATH, true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void requiredWithoutLRA() {
        resourceRequest(REQUIRED_PATH, false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithLRA() {
        resourceRequest(REQUIRES_NEW_PATH, true, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithoutLRA() {
        resourceRequest(REQUIRES_NEW_PATH, false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void mandatoryWithLRA() {
        resourceRequest(MANDATORY_PATH, true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void mandatoryWithoutLRA() {
        resourceRequest(MANDATORY_PATH, false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsWithLRA() {
        resourceRequest(SUPPORTS_PATH, true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void supportsWithoutLRA() {
        resourceRequest(SUPPORTS_PATH, false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithRA() {
        resourceRequest(NOT_SUPPORTED_PATH, true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithoutLRA() {
        resourceRequest(NOT_SUPPORTED_PATH, false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithRA() {
        resourceRequest(NEVER_PATH, true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithoutLRA() {
        resourceRequest(NEVER_PATH, false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    // same set of tests except that the invoked method end = true set on the LRA annotation

    @Test
    public void requiredEndWithLRA() {
        resourceRequest(REQUIRED_WITH_END_PATH, true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void requiredEndWithoutLRA() {
        resourceRequest(REQUIRED_WITH_END_PATH, false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithLRA() {
        resourceRequest(REQUIRES_NEW_WITH_END_PATH, true, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithoutLRA() {
        resourceRequest(REQUIRES_NEW_WITH_END_PATH, false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void mandatoryEndWithLRA() {
        resourceRequest(MANDATORY_WITH_END_PATH, true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void mandatoryEndWithoutLRA() {
        resourceRequest(MANDATORY_WITH_END_PATH, false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsEndWithLRA() {
        resourceRequest(SUPPORTS_WITH_END_PATH, true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void supportsEndWithoutLRA() {
        resourceRequest(SUPPORTS_WITH_END_PATH, false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithRA() {
        resourceRequest(NOT_SUPPORTED_WITH_END_PATH, true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithoutLRA() {
        resourceRequest(NOT_SUPPORTED_WITH_END_PATH, false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithEndRA() {
        resourceRequest(NEVER_WITH_END_PATH, true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithoutEndLRA() {
        resourceRequest(NEVER_WITH_END_PATH, false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    /**
     * Perform a JAX-RS resource request and check the resulting status and whether or not it ran with
     * the correct LRA context.
     *
     * @param path the resource path of the JAX-RS method to invoke
     * @param startLRA indicates whether or not an active context should be present on the request
     * @param expectedStatus the expected JAX-RS status code in the response
     * @param lraCheckType the type of check to perform on the request context and the context specified
     *                     by the {@link LRA.Type} annotation
     * @param methodLRAShouldBeActive if true the LRA started by the invoked method should still
     *                               be active after the resource invocation completes
     */
    private void resourceRequest(String path, boolean startLRA, int expectedStatus, MethodLRACheck lraCheckType,
                                 boolean methodLRAShouldBeActive) {
        Invocation.Builder target = tckSuiteTarget.path(TCK_LRA_TYPE_RESOURCE_PATH)
                .path(path).request();
        URI lra = startLRA ? lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS) : null;

        Response response = lra == null ? target.get() : target.header(LRA.LRA_HTTP_HEADER, lra).get();

        try {
            String methodLRA = response.readEntity(String.class);
            String incomingLRA = lra == null ? "" : lra.toASCIIString();

            assertEquals(testName.getMethodName() + ": Unexpected status", expectedStatus, response.getStatus());

            if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                // 412 errors should abort running the target method so skip the LRA check
                lraCheckType = MethodLRACheck.NONE;
                methodLRAShouldBeActive = false;
            }

            switch (lraCheckType) {
                case NOT_PRESENT:
                    assertEquals(testName.getMethodName() + ": Resource method should not have run with an LRA: " + methodLRA,
                            0, methodLRA.length());
                    break;
                case EQUALS:
                    assertEquals(testName.getMethodName() + ": Resource method should have ran with the incoming LRA",
                            incomingLRA, methodLRA);
                    break;
                case NOT_EQUALS:
                    assertNotEquals(testName.getMethodName() + ": Resource method should not have run with the incoming LRA",
                            incomingLRA, methodLRA);
                    break;
                default:
                    break;
            }

            if (methodLRAShouldBeActive) {
                // validate that the method ran with an LRA and that it is still active
                assertNotEquals(testName.getMethodName() + ": Resource method should not have run with an LRA: " + methodLRA,
                        0, methodLRA.length());
                assertFalse(lraClient.isLRAFinished(methodLRA));
                lraClient.closeLRA(methodLRA);
            } else if (methodLRA.length() != 0) {
                // otherwise it should be finished
                assertTrue(lraClient.isLRAFinished(methodLRA));
            }

            if (lra != null) {
                lraClient.closeLRA(lra);
            }
        } finally {
            response.close();
        }
    }

    /**
     * The started LRA will be named based on the class name and the running test name.
     */
    private String lraClientId() {
        return this.getClass().getSimpleName() + "#" + testName.getMethodName();
    }

    /**
     * Adjusting the default timeout by the specified timeout factor
     * which can be defined by user.
     */
    private long lraTimeout() {
        return Util.adjust(LraTckConfigBean.LRA_TIMEOUT_MILLIS, config.timeoutFactor());
    }
}
