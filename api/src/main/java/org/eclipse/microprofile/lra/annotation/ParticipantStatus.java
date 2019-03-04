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

import org.eclipse.microprofile.lra.annotation.ws.rs.Leave;

/**
 * A representation of the status of a participant according to a
 * participant state model:
 *
 * The initial state Active is entered when a participant is first
 * associated with a Long Runing Action.
 *
 * The state Compensating is entered when a compensate
 * notification is received (which indicates that the associated
 * LRA was cancelled). The transition to end state Compensated
 * should occur when the participant has compensated for any actions
 * it performed when the LRA was executing. If compensation is not,
 * and will never be, possible then the final state of FailedToCompensate
 * is entered and the participant cannot leave this state until it receives
 * a forget notification {@link Forget}.
 *
 * The state Completing is entered when a complete
 * notification is received (which indicates that the associated
 * LRA was closed). This state is followed by Completed
 * or FailedToComplete depending upon whether the participant was or
 * was not able to tidy up.
 *
 * Note that a particant can leave this state model via the {@link Leave}
 * annotation provided that the associated LRA is in the state
 * {@link LRAStatus#Active}.
 *
 * The name value of the enum should be returned by participant methods marked
 * with the {@link Status}, {@link Compensate} and {@link Complete} annotations.
 */
public enum ParticipantStatus {
    /**
     * The participant has not yet been asked to Complete or Compensate
     */
    Active,
    /**
     * The participant is currently compensating any work it performed
     */
    Compensating,
    /**
     * The participant has successfully compensated for any work it performed
     */
    Compensated,
    /**
     * The participant was not able to compensate the work it performed (and must
     * remember it could not compensate until such time that it receives a forget
     * message ({@link Forget})
     */
    FailedToCompensate,
    /**
     * The participant is tidying up after being told to complete
     */
    Completing,
    /**
     * The participant has confirmed that is has completed any tidy-up actions
     */
    Completed,
    /**
     * The participant was unable to tidy-up
     */
    FailedToComplete,
}
