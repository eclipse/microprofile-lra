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

import org.eclipse.microprofile.lra.tck.participant.api.LraCancelOnResource;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import java.net.URI;

import static org.eclipse.microprofile.lra.tck.participant.api.LraCancelOnResource.LRA_CANCEL_ON_RESOURCE_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Arquillian.class)
public class TckCancelOnTests extends TckTestBase {

    @Inject
    private LRAMetricService lraMetricService;

    @Inject
    private LRATestService lraTestService;
    
    @Deployment(name = "tcktests-cancelon")
    public static WebArchive deploy() {
        return TckTestBase.deploy(TckCancelOnTests.class.getSimpleName().toLowerCase());
    }

    private WebTarget getSuiteTarget() {
        return tckSuiteTarget;
    }

    /**
     * See {@link LraCancelOnResource#cancelOnFamilyDefault4xx(java.net.URI)}
     */
    @Test
    public void cancelOnFamilyDefault4xx() {
        WebTarget resourcePath = getSuiteTarget().path(LRA_CANCEL_ON_RESOURCE_PATH)
            .path(LraCancelOnResource.CANCEL_ON_FAMILY_DEFAULT_4XX);
        
        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Status.BAD_REQUEST, response, resourcePath));
        lraTestService.waitForCallbacks(lraId);
        assertTrue("After 400 compensate should be invoked",
            lraMetricService.getMetric(LRAMetricType.Compensated, lraId) >= 1);
        assertEquals("After 400 complete can't be invoked", 
            0, lraMetricService.getMetric(LRAMetricType.Completed, lraId));
    }

    /**
     * See {@link LraCancelOnResource#cancelOnFamilyDefault5xx(java.net.URI)}
     */
    @Test
    public void cancelOnFamilyDefault5xx() {
        WebTarget resourcePath = getSuiteTarget().path(LRA_CANCEL_ON_RESOURCE_PATH)
            .path(LraCancelOnResource.CANCEL_ON_FAMILY_DEFAULT_5XX);
        
        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Status.INTERNAL_SERVER_ERROR, response, resourcePath));
        lraTestService.waitForCallbacks(lraId);
        assertTrue("After 500 compensate should be invoked",
            lraMetricService.getMetric(LRAMetricType.Compensated, lraId) >= 1);
        assertEquals("After 500 complete can't be invoked", 
            0, lraMetricService.getMetric(LRAMetricType.Completed, lraId));
    }

    /**
     * See {@link LraCancelOnResource#cancelOnFamily3xx(java.net.URI)}
     */
    @Test
    public void cancelOnFamily3xx() {
        WebTarget resourcePath = getSuiteTarget().path(LRA_CANCEL_ON_RESOURCE_PATH)
            .path(LraCancelOnResource.CANCEL_ON_FAMILY_3XX);
        
        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Status.SEE_OTHER, response, resourcePath));
        lraTestService.waitForCallbacks(lraId);
        assertTrue("After status code 303 is received, compensate should be invoked as set by attribute cancelOnFamily",
                lraMetricService.getMetric(LRAMetricType.Compensated, lraId) >= 1);
        assertEquals("After status code 303 is received, complete can't be invoked as not defined in annotation @LRA",
                0, lraMetricService.getMetric(LRAMetricType.Completed, lraId));
    }

    /**
     * See {@link LraCancelOnResource#cancelOn301(java.net.URI)}
     */
    @Test
    public void cancelOn301() {
        WebTarget resourcePath = getSuiteTarget().path(LRA_CANCEL_ON_RESOURCE_PATH)
            .path(LraCancelOnResource.CANCEL_ON_301);
        
        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Status.MOVED_PERMANENTLY, response, resourcePath));
        lraTestService.waitForCallbacks(lraId);
        assertTrue("After status code 301 is received, compensate should be invoked as set by attribute cancelOn",
                lraMetricService.getMetric(LRAMetricType.Compensated, lraId) >= 1);
        assertEquals("After status code 301 is received, complete can't be invoked as not defined in annotation @LRA",
                0, lraMetricService.getMetric(LRAMetricType.Completed, lraId));
    }

    /**
     * See {@link LraCancelOnResource#notCancelOnFamily5xx(java.net.URI)}
     */
    @Test
    public void notCancelOnFamily5xx() {
        WebTarget resourcePath = getSuiteTarget().path(LRA_CANCEL_ON_RESOURCE_PATH)
            .path(LraCancelOnResource.NOT_CANCEL_ON_FAMILY_5XX);
        
        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Status.INTERNAL_SERVER_ERROR, response, resourcePath));
        lraTestService.waitForCallbacks(lraId);
        assertEquals("After status code 500 is received, compensate can't be invoked as default behaviour was changed",
                0, lraMetricService.getMetric(LRAMetricType.Compensated, lraId));
        assertTrue("After status code 500 is received, complete has to be called as default behaviour was changed",
                lraMetricService.getMetric(LRAMetricType.Completed, lraId) >= 1);
    }

    /**
     * See {@link LraCancelOnResource#cancelFromRemoteCall(java.net.URI, javax.ws.rs.core.UriInfo)}
     */
    @Test
    public void cancelFromRemoteCall() {
        WebTarget resourcePath = getSuiteTarget().path(LRA_CANCEL_ON_RESOURCE_PATH)
            .path(LraCancelOnResource.CANCEL_FROM_REMOTE_CALL);
        
        Response response = resourcePath.request().get();

        URI lraId = URI.create(checkStatusReadAndCloseResponse(Status.OK, response, resourcePath));
        lraTestService.waitForCallbacks(lraId);
        assertTrue("Status was 200 but compensate should be called as LRA should be cancelled for remotely called participant as well",
                lraMetricService.getMetric(LRAMetricType.Compensated, lraId) >= 1);
        assertEquals("Even the 200 status was received the remotely called participant should cause the LRA being cancelled",
                0, lraMetricService.getMetric(LRAMetricType.Completed, lraId));
    }
}
