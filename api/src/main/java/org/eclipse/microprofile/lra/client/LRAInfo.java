/*
 *******************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.lra.client;

/**
 * Data object carrying information about an instance
 * of LRA (specified by lra id) and it's status.
 */
public interface LRAInfo {

    /**
     * @return  lra id that lra instance is identified by
     */
    String getLraId();

    /**
     * @return  lra client id
     */
    String getClientId();

    /**
     * @return  true if lra was successfully completed, false otherwise
     */
    boolean isComplete();

    /**
     * @return  true if lra was compensated, false otherwise
     */
    boolean isCompensated();

    /**
     * @return  true if recovery is in progress on the lra, false otherwise
     */
    boolean isRecovering();

    /**
     * @return  true if lra is in active state right now, false otherwise
     */
    boolean isActive();

    /**
     * @return  true if lra is top level (not nested), false otherwise
     */
    boolean isTopLevel();
}
