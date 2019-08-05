/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.lra.tck.participant.api.LRAUnknownResource;
import org.eclipse.microprofile.lra.tck.participant.api.Scenario;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.junit.Assert.assertEquals;

/**
 * TCK Tests related to the 410 status code handling. Version without a Status method.
 */
@RunWith(Arquillian.class)
public class TckUnknownTests extends TckTestBase {

    @Inject
    private LRAMetricService lraMetricService;

    @Deployment(name = "tckunkown")
    public static WebArchive deploy() {
        return TckTestBase.deploy(TckUnknownTests.class.getSimpleName().toLowerCase());
    }

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void compensate_immediate() throws WebApplicationException {
        String lraIdString = invoke(Scenario.COMPENSATE_IMMEDIATE);
        URI lraId = URI.create(lraIdString);

        applyConsistencyDelay();
        int compensated = lraMetricService.getMetric(LRAMetricType.Compensated, lraId);
        int afterLRA = lraMetricService.getMetric(LRAMetricType.AfterLRA, lraId);
        int cancelled = lraMetricService.getMetric(LRAMetricType.Cancelled, lraId);

        assertEquals("Number of calls to @Compensate incorrect", 1, compensated);
        assertEquals("Number of calls to @AfterLRA incorrect", 1, afterLRA);
        assertEquals("Final LRA status of Cancelled incorrect", 1, cancelled);
    }

    @Test
    public void compensate_retry() throws WebApplicationException {
        String lraIdString = invoke(Scenario.COMPENSATE_RETRY);
        URI lraId = URI.create(lraIdString);

        applyConsistencyDelay();
        int compensated = lraMetricService.getMetric(LRAMetricType.Compensated, lraId);
        int afterLRA = lraMetricService.getMetric(LRAMetricType.AfterLRA, lraId);
        int cancelled = lraMetricService.getMetric(LRAMetricType.Cancelled, lraId);

        assertEquals("Number of calls to @Compensate incorrect", 2, compensated);
        assertEquals("Number of calls to @AfterLRA incorrect", 1, afterLRA);
        assertEquals("Final LRA status of Cancelled incorrect", 1, cancelled);
    }

    @Test
    public void complete_immediate() throws WebApplicationException {
        String lraIdString = invoke(Scenario.COMPLETE_IMMEDIATE);
        URI lraId = URI.create(lraIdString);

        applyConsistencyDelay();
        int completed = lraMetricService.getMetric(LRAMetricType.Completed, lraId);
        int afterLRA = lraMetricService.getMetric(LRAMetricType.AfterLRA, lraId);
        int closed = lraMetricService.getMetric(LRAMetricType.Closed, lraId);

        assertEquals("Number of calls to @Complete incorrect", 1, completed);
        assertEquals("Number of calls to @AfterLRA incorrect", 1, afterLRA);
        assertEquals("Final LRA status of Closed incorrect",1, closed);
    }

    @Test
    public void complete_retry() throws WebApplicationException {
        String lraIdString = invoke(Scenario.COMPLETE_RETRY);
        URI lraId = URI.create(lraIdString);

        applyConsistencyDelay();
        int completed = lraMetricService.getMetric(LRAMetricType.Completed, lraId);
        int afterLRA = lraMetricService.getMetric(LRAMetricType.AfterLRA, lraId);
        int closed = lraMetricService.getMetric(LRAMetricType.Closed, lraId);

        assertEquals("Number of calls to @Complete incorrect", 2, completed);
        assertEquals("Number of calls to @AfterLRA incorrect", 1, afterLRA);
        assertEquals("Final LRA status of Closed incorrect",1, closed);
    }

    private String invoke(Scenario scenario) {
        WebTarget resourcePath = tckSuiteTarget.path(LRAUnknownResource.LRA_CONTROLLER_PATH).path(LRAUnknownResource.TRANSACTIONAL_WORK_PATH)
                .queryParam("scenario", scenario.name());
        Response response = resourcePath.request().put(Entity.text(""));

        return checkStatusReadAndCloseResponse(Response.Status.fromStatusCode(scenario.getPathResponseCode()), response, resourcePath);
    }
}
