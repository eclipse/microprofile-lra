/*
 *******************************************************************************
 * Copyright (c) 2018-2021 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.lra.annotation;

/**
 * A representation of the status of a Long Running Action according to a
 * LRA state model:
 *
 * The initial state {@link #Active} is entered when an LRA is created.
 *
 * The state {@link #Cancelling} is entered when a request to cancel
 * an LRA is received. The transition to end state {@link #Cancelled}
 * should occur when all the enlisted participants have indicated that
 * they successfully compensated for any actions they performed when the
 * LRA was executing. If any participant could not, and will never be able
 * to, compensate then the final state of {@link #FailedToCancel} is entered.
 *
 * The state {@link #Closing} is entered when a request to close
 * an LRA is received. The transition to end state {@link #Closed}
 * should occur when all the enlisted participants have indicated that
 * they successfully completed any actions they performed when the
 * LRA was executing. If any participant could not, and will never be able
 * to, complete then the final state of {@link #FailedToClose} is entered.
 */
public enum LRAStatus {
    /**
     * The LRA has not yet been asked to Close or Cancel
     */
    Active,
    /**
     * The LRA is currently informing participants that they should
     * compensate for any work they performed when the LRA was active
     */
    Cancelling,
    /**
     * All participants associated with the LRA have successfully
     * compensated for any work they performed when the LRA was active
     */
    Cancelled,
    /**
     * One or more participants associated with the LRA were not able to
     * compensate for the work they performed when the LRA was active
     */
    FailedToCancel,
    /**
     * The LRA is asking all participants to complete
     */
    Closing,
    /**
     * The LRA successfully told all participants to complete
     */
    Closed,
    /**
     * One or more participants associated with the LRA were not able to
     * complete the work they performed when the LRA was active
     */
    FailedToClose,
}
