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

/**
 * This interface is providing the implementation with the ability to trigger recovery (replay of the Compensate,
 * Complete or Status) calls when the TCK requires to perform such action. This allows to not wait for the periodic
 * recovery and thus it makes the TCK runs faster.
 */
public interface LraRecoveryService {

    /**
     * Triggers the recovery of all active LRAs in the system.
     */
    void triggerRecovery();

    /**
     * Triggers the recovery of a single LRA passed as an argument.
     *
     * @param lraId the LRA context of the LRA to be recovered
     */
    void triggerRecovery(URI lraId);
}
