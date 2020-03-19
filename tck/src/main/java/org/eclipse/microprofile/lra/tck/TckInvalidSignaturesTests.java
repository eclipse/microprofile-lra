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

import org.eclipse.microprofile.lra.tck.participant.invalid.LRAResourceWithoutCompensateOrAfteRLRA;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.InvalidAfterLRASignatureListener;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.InvalidArgumentTypesParticipant;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.InvalidReturnTypeParticipant;
import org.eclipse.microprofile.lra.tck.participant.nonjaxrs.TooManyArgsParticipant;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.test.api.Deployer;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

/**
 * <p>
 * TCK that verifies that invalid non-JAX-RS participant method signatures are reported during deployment
 * </p>
 * 
 * <p>
 * Each test deploys an archive containing single invalid participant containing an error in its participant 
 * method signature and expects that such deployment is aborted according to the specification.
 * </p>
 */
@RunWith(Arquillian.class)
public class TckInvalidSignaturesTests {
    
    private static final String INVALID_RETURN_TYPE_DEPLOYMENT = "nonjaxrs-return-type-deploy";
    private static final String TOO_MANY_ARGS_DEPLOYMENT = "too-many-args-deploy";
    private static final String INVALID_ARGUMENT_TYPE_DEPLOYMENT = "nonjaxrs-argument-type-deploy";
    private static final String INVALID_AFTER_LRA_SIGNATURE_DEPLOYMENT = "invalid-after-lra-deploy";
    private static final String INVALID_LRA_RESOURCE_DEPLOYMENT = "invalid-lra-resource-deploy";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Rule
    public DeploymentNameRule deploymentNameRule = new DeploymentNameRule();

    @ArquillianResource
    private Deployer deployer;
    
    @Deployment(name = INVALID_RETURN_TYPE_DEPLOYMENT, managed = false)
    public static WebArchive deployInvalidReturnTypeParticipant() {
        return createArchive(InvalidReturnTypeParticipant.class);
    }

    @Deployment(name = TOO_MANY_ARGS_DEPLOYMENT, managed = false)
    public static WebArchive deployTooManyArgsParticipant() {
        return createArchive(TooManyArgsParticipant.class);
    }

    @Deployment(name = INVALID_ARGUMENT_TYPE_DEPLOYMENT, managed = false)
    public static WebArchive deployInvalidArgumentTypeParticipant() {
        return createArchive(InvalidArgumentTypesParticipant.class);
    }
    
    @Deployment(name = INVALID_AFTER_LRA_SIGNATURE_DEPLOYMENT, managed = false)
    public static WebArchive deployInvalidAfterLRASignatureResource() {
        return createArchive(InvalidAfterLRASignatureListener.class);
    }

    @Deployment(name = INVALID_LRA_RESOURCE_DEPLOYMENT, managed = false)
    public static WebArchive deployInvalidLRAResource() {
        return createArchive(LRAResourceWithoutCompensateOrAfteRLRA.class);
    }

    private static WebArchive createArchive(Class<?> resourceClass) {
        String archiveName = resourceClass.getSimpleName();
        return ShrinkWrap
            .create(WebArchive.class, archiveName + ".war")
            .addClasses(resourceClass, JaxRsActivator.class)
            .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml");
    }
    
    @After
    public void after() {
        deployer.undeploy(deploymentNameRule.deploymentName);
    }

    /**
     * Verify that invalid return type (String) in participant method is detected
     */
    @Test
    public void invalidReturnTypeInParticipantMethodTest() {
        testInvalidDeployment(INVALID_RETURN_TYPE_DEPLOYMENT);
    }

    /**
     * Verify that too many arguments (more than 2) in partcipant method are detected
     */
    @Test
    public void tooManyArgsInParticipantMethodTest() {
        testInvalidDeployment(TOO_MANY_ARGS_DEPLOYMENT);
    }

    /**
     * Verify that invalid type of argument (int) in participant method is detected
     */
    @Test
    public void invalidArgumentTypeInParticipantMethodTest() {
        testInvalidDeployment(INVALID_ARGUMENT_TYPE_DEPLOYMENT);
    }

    /**
     * Verify that invalid <code>&#64;AfterLRA</code> method signature is detected
     */
    @Test
    public void invalidAfterLRASignatureTest() {
        testInvalidDeployment(INVALID_AFTER_LRA_SIGNATURE_DEPLOYMENT);
    }

    /**
     * Verify that invalid LRA resource which does not contain any of @Compensate or @AfterLRA methods is detected
     */
    @Test
    public void invalidLRAResourceWithoutCompensateOrAfterLRATest() {
        testInvalidDeployment(INVALID_LRA_RESOURCE_DEPLOYMENT);
    }

    private void testInvalidDeployment(String deploymentName) {
        deploymentNameRule.deploymentName = deploymentName;
        expectedException.expect(DeploymentException.class);

        deployer.deploy(deploymentName);
    }

    private static final class DeploymentNameRule extends TestName {
        
        String deploymentName;
    }
}
