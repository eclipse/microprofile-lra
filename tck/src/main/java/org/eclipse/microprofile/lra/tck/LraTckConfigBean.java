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
import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.annotation.ParticipantStatus;

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
     * <p>
     *     The LRA model guarantees eventual consistency but does not say when
     *     participants will be brought into a consistent state, for example an
     *     implementation may not notify participants immediately when an LRA
     *     is closed or cancelled.
     *
     *     To support implementations that do not immediately attempt to notify
     *     participants the TCK needs to know how long to delay before checking whether
     *     or not the implementation called them.
     *     This delay will depend on the particular implementation so it is
     *     configurable, i.e, use this property to configure the number of
     *     milliseconds that a test will wait for before it checks if a complete
     *     or compensate method has been called.
     *
     *     There are two properties for configuring delays. The long version should be
     *     used when multiple calls to these participant methods needs to be performed.
     *
     *     The default is set to zero which implies that the implementation will
     *     notify participants as soon as the LRA enters the
     *     {@link LRAStatus#Closing} or {@link LRAStatus#Cancelling}
     *     states. Note that this does not imply that the participant will
     *     complete or compensate immediately since it may enter the
     *     {@link ParticipantStatus#Completing} or {@link ParticipantStatus#Compensating}
     *     states first if a participant cannot clean up or compensate immediately.
     * </p>
     */
    @Inject @ConfigProperty(name = "lra.tck.consistency.longDelay", defaultValue = "0")
    private long longConsistencyDelay;

    /**
     * <p>
     *     An alternative to @see LraTckConfigBean#longConsistencyDelay
     *     This can be useful if the consistency delay needs to be changed for some tests
     *
     *     There are two properties for configuring delays. The short version should be
     *     used when we just need to wait for the implementation to call the participant methods
     * </p>
     */
    @Inject @ConfigProperty(name = "lra.tck.consistency.shortDelay", defaultValue = "0")
    private long shortConsistencyDelay;

    /**
     * <p>
     *     the maximum number of seconds to wait for recovery
     * </p>
     */
    @Inject @ConfigProperty(name = "lra.tck.recovery.timeout", defaultValue = "200")
    private long recoveryTimeout;

    /**
     * If true then the TCK should use the recovery header to give a hint to the implementation
     * under test that it should replay the LRA protocol termination phase ie to call the
     * compensate/complete callback on target resource (if the resource is also an LRA participant
     * and is in need of recovery).
     *
     * See the method {@link TckTestBase#replayEndPhase} for how/where it is used. The replayEndPhase
     * method will keep waiting for recovery until either recovery has been triggered or until
     * {@link #recoveryTimeout} seconds have elapsed.
     *
     * Implementations that do not support triggering recovery should ensure that the
     * {@link #recoveryTimeout} value is set to a value that is longer than the implementations
     * typical recovery period.
     */
    @Inject @ConfigProperty(name = "lra.tck.recovery.trigger", defaultValue = "true")
    private boolean useRecoveryHeader;

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

    public long getLongConsistencyDelay() {
        return longConsistencyDelay;
    }

    public long getShortConsistencyDelay() {
        return shortConsistencyDelay;
    }

    public Long recoveryTimeout() {
        return recoveryTimeout;
    }

    public boolean isUseRecoveryHeader() {
        return useRecoveryHeader;
    }

    public String tckSuiteBaseUrl() {
        return tckSuiteBaseUrl;
    }
}
