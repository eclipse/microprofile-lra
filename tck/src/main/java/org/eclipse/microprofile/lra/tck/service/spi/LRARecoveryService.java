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
package org.eclipse.microprofile.lra.tck.service.spi;

import java.net.URI;
import java.util.logging.Logger;

/**
 *     The LRA model guarantees eventual consistency but does not say when
 *     participants will be brought into a consistent state. When a request
 *     to close/cancel an LRA is received, the implementation is allowed to
 *     delay sending out the complete/compensate callbacks (the specification
 *     only requires that an implementation must eventually send them).
 *
 *     This SPI provides a mechanism for an implementation to indicate when
 *     it knows that callbacks have been sent and responses received or when
 *     callbacks have been issued, all participants have reached an end state,
 *     and when all listener notifications have been successfully delivered.
 *
 */
public interface LRARecoveryService {

    /**
     * Wait for the delivery of Complete and Compensate participant callbacks.
     * When this method returns the caller can be certain that the callbacks were
     * sent and responses received (including error responses).
     *
     * @param lraId the LRA context
     * @throws LRACallbackException the implementation was unable to determine whether
     * or not the callbacks were received by all participants
     */
    void waitForCallbacks(URI lraId) throws LRACallbackException;

    /**
     * Wait for all participants to reach an end state and for all
     * {@link org.eclipse.microprofile.lra.annotation.AfterLRA} notifications to be successfully delivered.
     * 
     * The default implementation iterates {@link LRARecoveryService#waitForEndPhaseReplay(URI)} until
     * all participants reach a final state and for all listeners notifications to be successfully delivered.
     *
     * @param lraId the LRA context
     * @throws LRACallbackException the implementation was unable to determine whether
     * or not all participants have reached an end state and whether or not all listeners have been
     * successfully notified
     */
    default void waitForRecovery(URI lraId) throws LRACallbackException {
        Logger log = Logger.getLogger(LRARecoveryService.class.getName());
        int counter = 0;

        do {
            log.info("Recovery attempt #" + ++counter);
        } while (!waitForEndPhaseReplay(lraId));
        log.info("LRA " + lraId + "has finished the recovery");
    }

    /**
     * Wait for one replay of the end phase of the LRA (the calls to Status, Complete, Compensate, and Forget
     * methods of all Compensating/Completing participants. If the LRA is finished as a result of this call
     * all listeners must also be successfully notified before this method returns.
     * 
     * @param lraId the LRA context
     * @return true if after the recovery all participants reached an end state and all AfterLRA listeners were
     * successfully notified, false otherwise
     * @throws LRACallbackException the implementation was unable to determine whether
     * or not the callbacks were received by all participants
     */
    boolean waitForEndPhaseReplay(URI lraId) throws LRACallbackException;
}
