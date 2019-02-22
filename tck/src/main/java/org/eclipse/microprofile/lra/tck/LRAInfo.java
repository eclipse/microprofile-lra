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

package org.eclipse.microprofile.lra.tck;

/**
 * Data object carrying information about an instance
 * of LRA (specified by LRA id) and it's status.
 */
public interface LRAInfo {

    /**
     * @return  LRA id that LRA instance is identified by
     */
    String getLraId();

    /**
     * @return  LRA client id
     */
    String getClientId();

    /**
     * @return  true if LRA was successfully closed, false otherwise
     */
    boolean isClosed();

    /**
     * @return  true if LRA was cancelled, false otherwise
     */
    boolean isCancelled();

    /**
     * @return  true if recovery is in progress on the LRA, false otherwise
     */
    boolean isRecovering();

    /**
     * @return  true if LRA is in active state right now, false otherwise
     */
    boolean isActive();

    /**
     * @return  true if LRA is top level (not nested), false otherwise
     */
    boolean isTopLevel();
}
