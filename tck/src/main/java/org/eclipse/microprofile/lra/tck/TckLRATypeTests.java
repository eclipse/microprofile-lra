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
import org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckInterface;
import org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource;
import org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckSuperclass;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.temporal.ChronoUnit;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckInterfaceResource.TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckResource.TCK_LRA_TYPE_RESOURCE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.LRATypeTckSuperclassResource.TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH;
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
public class TckLRATypeTests extends TckTestBase {
    private static final Logger LOGGER = Logger.getLogger(TckLRATypeTests.class.getName());

    @Deployment(name = "lra-type-tck-tests")
    public static WebArchive deploy() {
        return deploy(TckLRATypeTests.class.getSimpleName().toLowerCase());
    }

    @After
    public void after() {
        lraClient.cleanUp(LOGGER, testName.getMethodName());
    }

    // enum to indicate which checks to perform on the expected and actual active LRA after running a resource method
    private enum MethodLRACheck {
        NONE, NOT_PRESENT, EQUALS, NOT_EQUALS
    }

    // LRA annotations placed on the resource class (see LRATypeTckResource)

    @Test
    public void requiredWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRED_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void requiredWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRED_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRES_NEW_PATH,
            true, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRES_NEW_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void mandatoryWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.MANDATORY_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void mandatoryWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.MANDATORY_PATH,
            false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.SUPPORTS_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void supportsWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.SUPPORTS_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NOT_SUPPORTED_PATH,
            true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NOT_SUPPORTED_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NEVER_PATH,
            true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithInvalidLRA() {
        neverWithInvalidLRA(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NEVER_PATH);
    }

    @Test
    public void neverWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NEVER_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void requiredEndWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRED_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void requiredEndWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRED_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRES_NEW_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.REQUIRES_NEW_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void mandatoryEndWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.MANDATORY_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void mandatoryEndWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.MANDATORY_WITH_END_FALSE_PATH,
            false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsEndWithLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.SUPPORTS_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void supportsEndWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.SUPPORTS_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NOT_SUPPORTED_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithoutLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NOT_SUPPORTED_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithEndLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NEVER_WITH_END_FALSE_PATH,
            true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithoutEndLRA() {
        resourceRequest(TCK_LRA_TYPE_RESOURCE_PATH, LRATypeTckResource.NEVER_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    // LRA annotations placed on the interface (see LRATypeTckInterfaceResource)

    @Test
    public void requiredWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRED_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void requiredWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRED_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRES_NEW_PATH,
            true, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRES_NEW_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void mandatoryWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.MANDATORY_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void mandatoryWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.MANDATORY_PATH,
            false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.SUPPORTS_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void supportsWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.SUPPORTS_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NOT_SUPPORTED_PATH,
            true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NOT_SUPPORTED_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NEVER_PATH,
            true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithInvalidLRAAtInterface() {
        neverWithInvalidLRA(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NEVER_PATH);
    }

    @Test
    public void neverWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NEVER_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void requiredEndWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRED_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void requiredEndWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRED_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRES_NEW_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.REQUIRES_NEW_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void mandatoryEndWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.MANDATORY_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void mandatoryEndWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.MANDATORY_WITH_END_FALSE_PATH,
            false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsEndWithLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.SUPPORTS_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void supportsEndWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.SUPPORTS_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NOT_SUPPORTED_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithoutLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NOT_SUPPORTED_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithEndLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NEVER_WITH_END_FALSE_PATH,
            true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithoutEndLRAAtInterface() {
        resourceRequest(TCK_LRA_TYPE_INTERFACE_RESOURCE_PATH, LRATypeTckInterface.NEVER_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    // LRA annotations placed on the superclass (see LRATypeTckSuperclassResource)

    @Test
    public void requiredWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRED_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void requiredWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRED_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRES_NEW_PATH,
            true, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void requiresNewWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRES_NEW_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, false);
    }

    @Test
    public void mandatoryWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.MANDATORY_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void mandatoryWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.MANDATORY_PATH,
            false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.SUPPORTS_PATH,
            true, 200, MethodLRACheck.EQUALS, false);
    }

    @Test
    public void supportsWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.SUPPORTS_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NOT_SUPPORTED_PATH,
            true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NOT_SUPPORTED_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NEVER_PATH,
            true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithInvalidLRAAtSuperclass() {
        neverWithInvalidLRA(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NEVER_PATH);
    }

    @Test
    public void neverWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NEVER_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void requiredEndWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRED_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void requiredEndWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRED_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRES_NEW_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void requiresEndNewWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.REQUIRES_NEW_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_EQUALS, true);
    }

    @Test
    public void mandatoryEndWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.MANDATORY_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void mandatoryEndWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.MANDATORY_WITH_END_FALSE_PATH,
            false, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void supportsEndWithLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.SUPPORTS_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.EQUALS, true);
    }

    @Test
    public void supportsEndWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.SUPPORTS_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NOT_SUPPORTED_WITH_END_FALSE_PATH,
            true, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void notSupportedEndWithoutLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NOT_SUPPORTED_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithEndLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NEVER_WITH_END_FALSE_PATH,
            true, 412, MethodLRACheck.NOT_PRESENT, false);
    }

    @Test
    public void neverWithoutEndLRAAtSuperclass() {
        resourceRequest(TCK_LRA_TYPE_SUPERCLASS_RESOURCE_PATH, LRATypeTckSuperclass.NEVER_WITH_END_FALSE_PATH,
            false, 200, MethodLRACheck.NOT_PRESENT, false);
    }

    private void neverWithInvalidLRA(String rootPath, String path) {

        resourceRequest(rootPath, path, true, 412, MethodLRACheck.NOT_PRESENT, false);

        Invocation.Builder target = tckSuiteTarget.path(rootPath)
            .path(path).request();
        // Anything in the header means that Type.NEVER must fail
        target = target.header(LRA.LRA_HTTP_CONTEXT_HEADER, "http://something/like/an/URI");

        Response response = target.get();

        assertEquals(testName.getMethodName() + ": Unexpected status", 412, response.getStatus());

        response.close();
    }

    /**
     * Perform a JAX-RS resource request and check the resulting status and whether or not it ran with
     * the correct LRA context.
     *
     * @param rootPath the root path of the JAX-RS resource to invoke
     * @param path the resource path of the JAX-RS method to invoke
     * @param startLRA indicates whether or not an active context should be present on the request
     * @param expectedStatus the expected JAX-RS status code in the response
     * @param lraCheckType the type of check to perform on the request context and the context specified
     *                     by the {@link LRA.Type} annotation
     * @param methodLRAShouldBeActive if true the LRA started by the invoked method should still
     *                               be active after the resource invocation completes
     */
    private String resourceRequest(String rootPath, String path, boolean startLRA, int expectedStatus, MethodLRACheck lraCheckType,
                                 boolean methodLRAShouldBeActive) {
        Invocation.Builder target = tckSuiteTarget.path(rootPath)
                .path(path).request();
        URI lra = startLRA ? lraClient.startLRA(null, lraClientId(), lraTimeout(), ChronoUnit.MILLIS) : null;

        if (lra != null) {
            target = target.header(LRA.LRA_HTTP_CONTEXT_HEADER, lra);
        }

        Response response = target.get();

        try {
            String methodLraId; // Make it a String as we do some String based tests afterwards
            String incomingLraId = lra == null ? "" : lra.toASCIIString();

            assertEquals(testName.getMethodName() + ": Unexpected status", expectedStatus, response.getStatus());

            if (response.getStatus() == Response.Status.PRECONDITION_FAILED.getStatusCode()) {
                // 412 errors should abort running the target method so skip the LRA check
                lraCheckType = MethodLRACheck.NONE;
                methodLRAShouldBeActive = false;
                methodLraId = "";
            } else {
                methodLraId = response.readEntity(String.class);
            }

            switch (lraCheckType) {
                case NOT_PRESENT:
                    assertEquals(testName.getMethodName() + ": Resource method should not have run with an LRA: " + methodLraId,
                            0, methodLraId.length());
                    break;
                case EQUALS:
                    assertEquals(testName.getMethodName() + ": Resource method should have ran with the incoming LRA",
                            incomingLraId, methodLraId);
                    break;
                case NOT_EQUALS:
                    assertNotEquals(testName.getMethodName() + ": Resource method should not have run with the incoming LRA",
                            incomingLraId, methodLraId);
                    break;
                default:
                    break;
            }

            if (methodLRAShouldBeActive) {
                // validate that the method ran with an LRA and that it is still active
                assertNotEquals(testName.getMethodName() + ": Resource method should not have run with an LRA: " + methodLraId,
                        0, methodLraId.length());
                assertFalse(lraManagementService.isLRAFinished(URI.create(methodLraId)));
                lraClient.closeLRA(methodLraId);
            } else if (methodLraId.length() != 0) {
                // otherwise it should be finished
                assertTrue(lraManagementService.isLRAFinished(URI.create(methodLraId)));
            }

            if (lra != null) {
                lraClient.closeLRA(lra);
            }

            return methodLraId;
        } catch (Throwable e) {
            LOGGER.warning(e.getMessage());
            return null;
        } finally {
            response.close();
        }
    }
}
