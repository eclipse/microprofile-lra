/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
 * This interface provides an SPI for the management of the LRAs started and processed
 * in the TCK. It covers necessary operations that should be provided by the 
 * implemenation. However, because the implementations can differ in the underlying structures
 * (e.g., coordination vs decentralization approaches) they cannot be specified directly in the
 * LRA specification.
 */
public interface LRAManagementService {

    /**
     * Returns a boolean value indicationg whether the LRA is already in the end state
     * (Cancelled, Closed, FailedToCancel, FailedToClose).
     * 
     * @param lraId the LRA context of the queried LRA
     * @return true if the LRA is in the end state, false otherwise
     */
    boolean isLRAFinished(URI lraId);
    
}
