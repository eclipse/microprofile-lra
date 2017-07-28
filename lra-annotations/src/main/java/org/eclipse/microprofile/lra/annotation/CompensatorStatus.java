/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
 */
package org.eclipse.microprofile.lra.annotation;

/**
 * The status of a compensator. The status is only valid after the coordinator has told the compensator to
 * complete or compensate. The name value of the enum should be returned by compensator methods marked with
 * the {@link Status} annotation.
 */
public enum CompensatorStatus {
    Compensating, // the Compensator is currently compensating for the jfdi.
    Compensated, //  the Compensator has successfully compensated for the jfdi.
    FailedToCompensate, //  the Compensator was not able to compensate for the jfdi. It must maintain information about the work it was to compensate until the org.jboss.narayana.rts.lra.coordinator sends it a forget message.
    Completing, //  the Compensator is tidying up after being told to complete.
    Completed, //  the org.jboss.narayana.rts.lra.coordinator/participant has confirmed.
    FailedToComplete, //  the Compensator was unable to tidy-up.
}
