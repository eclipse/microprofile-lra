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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@ApplicationScoped
public class LRATestService {

    @Inject
    LRAMetricService lraMetricService;
    
    @Inject
    LraTckConfigBean config;

    private LRAClientOps lraClient;

    private static Client tckSuiteClient;

    private WebTarget tckSuiteTarget;

    public void start() {
        tckSuiteClient = ClientBuilder.newClient();
        lraMetricService.clear();

        try {
            tckSuiteTarget = tckSuiteClient.target(URI.create(new URL(config.tckSuiteBaseUrl()).toExternalForm()));
            lraClient = new LRAClientOps(tckSuiteTarget);
        } catch (MalformedURLException mfe) {
            throw new IllegalStateException("Cannot create URL for the LRA TCK suite base url " + config.tckSuiteBaseUrl(), mfe);
        }
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

    /**
     * @see LraTckConfigBean#getShortConsistencyDelay() 
     */
    public void applyShortConsistencyDelay() {
        if (config.getShortConsistencyDelay() > 0) {
            try {
                Thread.sleep(config.getShortConsistencyDelay());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * @see LraTckConfigBean#getLongConsistencyDelay() 
     */
    public void applyLongConsistencyDelay() {
        if (config.getLongConsistencyDelay() > 0) {
            try {
                Thread.sleep(config.getLongConsistencyDelay());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
