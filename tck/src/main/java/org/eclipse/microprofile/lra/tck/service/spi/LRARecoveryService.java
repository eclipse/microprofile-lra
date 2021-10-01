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
 * The LRA model guarantees eventual consistency but does not say when participants will be brought into a consistent
 * state. When a request to close/cancel an LRA is received, the implementation is allowed to delay sending out the
 * complete/compensate callbacks (the specification only requires that an implementation must eventually send them).
 *
 * This SPI provides a mechanism for an implementation to indicate when it knows that callbacks have been sent and
 * responses received or when callbacks have been issued, all participants have reached an end state, and when all
 * listener notifications have been successfully delivered.
 *
 */
public interface LRARecoveryService {

    /**
     * Wait for the delivery of Complete and Compensate participant callbacks. When this method returns the caller can
     * be certain that the callbacks were sent and responses received (including error responses).
     *
     * @param lraId
     *            the LRA context
     * @throws LRACallbackException
     *             the implementation was unable to determine whether or not the callbacks were received by all
     *             participants
     */
    void waitForCallbacks(URI lraId) throws LRACallbackException;

    /**
     * Wait for all participants to reach an end state and for all
     * {@link org.eclipse.microprofile.lra.annotation.AfterLRA} notifications to be successfully delivered (AfterLRA
     * methods return HTTP 200).
     *
     * The default implementation iterates {@link LRARecoveryService#waitForEndPhaseReplay(URI)} until all participants
     * reach a final state and all AfterLRA listeners notifications are successfully delivered.
     *
     * @param lraId
     *            the LRA context
     * @throws LRACallbackException
     *             the implementation was unable to determine whether or not all participants have reached an end state
     *             and whether or not all listeners have been successfully notified
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
     * Wait for one replay of the end phase of the LRA (the callback calls to Status, Complete, Compensate, and Forget
     * methods of all Compensating/Completing participants. If the LRA is finished as a result of this call all
     * listeners must also be successfully notified before this method returns. The callback calls must be attempted but
     * do not have to be successful (e.g. implementation tries to call Compensate which returns connection refused is a
     * valid invocation of this method).
     *
     * @param lraId
     *            the LRA context
     * @return true if the implementation successfully issued callback requests and received responses that indicate
     *         that final state was achieved and all AfterLRA listeners were successfully notified (AfterLRA methods
     *         returned HTTP 200), or false if the implementation successfully issued callback requests but it did not
     *         receive all responses or the received responses indicate that the final state is not reached yet
     * @throws LRACallbackException
     *             the implementation has no knowledge of this LRA or it was unable to retry the requests to all
     *             participants so it does not make sense to trigger this method with the same argument again
     */
    boolean waitForEndPhaseReplay(URI lraId) throws LRACallbackException;
}
