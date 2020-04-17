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

import org.eclipse.microprofile.lra.tck.LRAClientOps;
import org.eclipse.microprofile.lra.tck.LraTckConfigBean;
import org.eclipse.microprofile.lra.tck.service.spi.LRACallbackException;
import org.eclipse.microprofile.lra.tck.service.spi.LRARecoveryService;
import org.junit.Assert;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.URI;
import java.net.URL;
import java.util.Iterator;
import java.util.ServiceLoader;

@ApplicationScoped
public class LRATestService {

    @Inject
    LraTckConfigBean config;

    private LRAClientOps lraClient;

    private static Client tckSuiteClient;

    private WebTarget tckSuiteTarget;

    private LRARecoveryService lraRecoveryService = loadService(LRARecoveryService.class);

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
            Assert.fail(e.getMessage());
        }
    }

    public void waitForRecovery(URI lraId) {
        try {
            lraRecoveryService.waitForRecovery(lraId);
        } catch (LRACallbackException e) {
            Assert.fail(e.getMessage());
        }
    }

    public void waitForEndPhaseReplay(URI lraId) {
        try {
            lraRecoveryService.waitForEndPhaseReplay(lraId);
        } catch (LRACallbackException e) {
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
}
