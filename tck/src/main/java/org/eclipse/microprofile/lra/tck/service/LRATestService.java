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
package org.eclipse.microprofile.lra.tck.service;

import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.tck.LRAClientOps;
import org.eclipse.microprofile.lra.tck.participant.api.WrongHeaderException;
import org.eclipse.microprofile.lra.tck.service.spi.LRACallbackException;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.junit.Assert;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_ENDED_CONTEXT_HEADER;

@ApplicationScoped
public class LRATestService {
    private static final Logger LOG = Logger.getLogger(LRATestService.class.getName());

    private LRAClientOps lraClient;

    private static Client tckSuiteClient;

    private WebTarget tckSuiteTarget;

    private LRARecoveryService lraRecoveryService = loadService(LRARecoveryService.class);

    @Inject
    private LRAMetricService lraMetricService;

    public void start(URL deploymentURL) {
        tckSuiteClient = ClientBuilder.newClient();
        tckSuiteTarget = tckSuiteClient.target(URI.create(deploymentURL.toExternalForm()));
        lraClient = new LRAClientOps(tckSuiteTarget);
    }

    public void stop() {
        if(tckSuiteClient != null) {
            tckSuiteClient.close();
        }
    }

    public LRAClientOps getLRAClient() {
        return lraClient;
    }

    public WebTarget getTCKSuiteTarget() {
        return tckSuiteTarget;
    }

    public void waitForCallbacks(URI lraId) {
        try {
            lraRecoveryService.waitForCallbacks(lraId);
        } catch (LRACallbackException e) {
            LOG.log(Level.SEVERE, "Fail to 'waitForCallbacks' for LRA " + lraId, e);
            Assert.fail(e.getMessage());
        }
    }

    public void waitForRecovery(URI lraId) {
        try {
            lraRecoveryService.waitForRecovery(lraId);
        } catch (LRACallbackException e) {
            LOG.log(Level.SEVERE, "Fail to 'waitForRecovery' for LRA " + lraId, e);
            Assert.fail(e.getMessage());
        }
    }

    public void waitForEndPhaseReplay(URI lraId) {
        try {
            lraRecoveryService.waitForEndPhaseReplay(lraId);
        } catch (LRACallbackException e) {
            LOG.log(Level.SEVERE, "Fail to 'waitForEndPhaseReplay' for LRA " + lraId, e);
            Assert.fail(e.getMessage());
        }
    }

    public static <T> T loadService(Class<T> type) {
        ServiceLoader<T> serviceLoader = ServiceLoader.load(type);
        Iterator<T> iterator = serviceLoader.iterator();

        if (!iterator.hasNext()) {
            throw new IllegalStateException(String.format("No implementation of %s which is required for the " +
                "TCK run was found with the service loader", type.getName()));
        }

        return iterator.next();
    }

    public void assertHeaderPresent(URI lraId, String path, String headerName) {
        if (lraId == null) {
            throw new WrongHeaderException(String.format("%s: missing '%s' header", path, headerName));
        }
    }

    public Response processAfterLRAInfo(URI endedLRAId, LRAStatus status, Class<?> resourceClass, String path) {
        assertHeaderPresent(endedLRAId, path, LRA_HTTP_ENDED_CONTEXT_HEADER);

        switch (status) {
            case Closed:
                // FALLTHRU
            case Cancelled:
                // FALLTHRU
            case FailedToCancel:
                // FALLTHRU
            case FailedToClose:
                lraMetricService.incrementMetric(
                    LRAMetricType.valueOf(status.name()),
                    endedLRAId,
                    resourceClass);
                return Response.ok().build();
            default:
                return Response.status(Response.Status.BAD_REQUEST).build();
        }

    }

    /**
     * Returns whether the passed LRA and resource are in finished state.
     *
     * @param lra the LRA to test
     * @param resourceName name of the resource that the metrics parameter applies to
     * @return whether or not an LRA has finished
     */
    public boolean isLRAFinished(URI lra, String resourceName) {
        return lraMetricService.getMetric(LRAMetricType.Closed, lra, resourceName) > 0 ||
            lraMetricService.getMetric(LRAMetricType.FailedToClose, lra, resourceName) > 0 ||
            lraMetricService.getMetric(LRAMetricType.Cancelled, lra, resourceName) > 0 ||
            lraMetricService.getMetric(LRAMetricType.FailedToCancel, lra, resourceName) > 0;
    }

    /**
     * Returns whether the passed LRA is in finished state.
     *
     * @param lra the LRA to test
     * @return whether or not an LRA has finished
     */
    public boolean isLRAFinished(URI lra) {
        return lraMetricService.getMetric(LRAMetricType.Closed, lra) > 0 ||
            lraMetricService.getMetric(LRAMetricType.FailedToClose, lra) > 0 ||
            lraMetricService.getMetric(LRAMetricType.Cancelled, lra) > 0 ||
            lraMetricService.getMetric(LRAMetricType.FailedToCancel, lra) > 0;
    }
}
