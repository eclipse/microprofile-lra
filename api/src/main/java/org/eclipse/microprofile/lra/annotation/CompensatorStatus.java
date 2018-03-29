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

package org.eclipse.microprofile.lra.annotation;

/**
 * The status of a participant. The status is only valid after the coordinator
 * has told the participant to complete or compensate. The name value of the
 * enum should be returned by participant methods marked with
 * the {@link Status} annotation.
 */
public enum CompensatorStatus {
    /**
     * The Compensator is currently compensating for the LRA
     */
    Compensating,
    /**
     * The Compensator has successfully compensated for the LRA
     */
    Compensated,
    /**
     * The Compensator was not able to compensate for the LRA (and must remember
     * it could not compensate until such time that it receives a forget message)
     */
    FailedToCompensate,
    /**
     * The Compensator is tidying up after being told to complete
     */
    Completing,
    /**
     * The Compensator has confirmed
     */
    Completed,
    /**
     * The Compensator was unable to tidy-up
     */
    FailedToComplete,
}
