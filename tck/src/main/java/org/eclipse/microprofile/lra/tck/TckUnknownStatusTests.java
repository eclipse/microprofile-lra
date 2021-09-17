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

import java.net.URI;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.lra.tck.participant.api.LRAUnknownStatusResource;
import org.eclipse.microprofile.lra.tck.participant.api.Scenario;
import org.eclipse.microprofile.lra.tck.service.LRAMetricAssertions;
import org.eclipse.microprofile.lra.tck.service.LRATestService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * TCK Tests related to the 410 status code handling. Version with a Status method.
 */
@RunWith(Arquillian.class)
public class TckUnknownStatusTests extends TckTestBase {

    @Inject
    private LRAMetricAssertions lraMetric;

    @Inject
    private LRATestService lraTestService;

    @Deployment(name = "tckunkownstatus")
    public static WebArchive deploy() {
        return TckUnknownStatusTests.deploy(TckUnknownStatusTests.class.getSimpleName().toLowerCase());
    }

    @Before
    public void before() {
        super.before();
    }

    @Test
    public void compensate_retry() throws WebApplicationException {
        String lraIdString = invoke(Scenario.COMPENSATE_RETRY);
        URI lraId = URI.create(lraIdString);

        lraTestService.waitForRecovery(lraId);

        lraMetric.assertCompensatedEquals("Number of calls to @Compensate incorrect",
                1, lraId, LRAUnknownStatusResource.class);
        lraMetric.assertStatus("Expect @Status was called", lraId, LRAUnknownStatusResource.class);
        lraMetric.assertAfterLRA("Expect @AfterLRA was called", lraId, LRAUnknownStatusResource.class);
        lraMetric.assertCancelled("Expect Cancel was called", lraId, LRAUnknownStatusResource.class);
    }

    @Test
    public void complete_retry() throws WebApplicationException {
        String lraIdString = invoke(Scenario.COMPLETE_RETRY);
        URI lraId = URI.create(lraIdString);

        lraTestService.waitForRecovery(lraId);

        lraMetric.assertCompletedEquals("Number of calls to @Complete incorrect",
                1, lraId, LRAUnknownStatusResource.class);
        lraMetric.assertStatus("Expect @Status was called", lraId, LRAUnknownStatusResource.class);
        lraMetric.assertAfterLRA("Expect @AfterLRA was called", lraId, LRAUnknownStatusResource.class);
        lraMetric.assertClosed("Expect Close was called", lraId, LRAUnknownStatusResource.class);
    }

    private String invoke(Scenario scenario) {
        WebTarget resourcePath = tckSuiteTarget.path(LRAUnknownStatusResource.LRA_CONTROLLER_PATH)
                .path(LRAUnknownStatusResource.TRANSACTIONAL_WORK_PATH)
                .queryParam("scenario", scenario.name());
        Response response = resourcePath.request().put(Entity.text(""));

        return checkStatusReadAndCloseResponse(Response.Status.fromStatusCode(scenario.getPathResponseCode()), response,
                resourcePath);
    }
}
