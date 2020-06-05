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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class LraTckConfigBean {

    /**
     * Definition of LRA default timeout which should be used
     * by any method which needs to work with timeout
     */
    public static final Long LRA_TIMEOUT_MILLIS = 50000L;

    /**
     * <p>
     * Timeout factor which adjusts waiting time and timeouts for the TCK suite.
     * <p>
     * The default value is set to <code>1.0</code> which means the defined timeout
     * is multiplied by <code>1</code>.
     * <p>
     * If you wish the test waits longer then set the value bigger than <code>1.0</code>.
     * If you wish the test waits shorter time than designed
     * or the timeout is elapsed faster then set the value less than <code>1.0</code>
     */
    @Inject @ConfigProperty(name = "lra.tck.timeout.factor", defaultValue = "1.0")
    private double timeoutFactor;

    /**
     * Base URL where the LRA suite is started at. It's URL where container exposes the test suite deployment.
     * The test paths will be constructed based on this base URL.
     * <p>
     * The default base URL where TCK suite is expected to be started is <code>http://localhost:8180/</code>.
     */
    @Inject @ConfigProperty(name = "lra.tck.base.url", defaultValue = "http://localhost:8180/")
    private String tckSuiteBaseUrl;


    public double timeoutFactor() {
        return timeoutFactor;
    }

}
