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
package org.eclipse.microprofile.lra.participant;

/**
 * Runtime exception thrown when invalid non-JAX-RS LRA signature definition is detected.
 * 
 * The prohibited valid signatures are:
 * Return type: 
 *  - void: successfull execution is mapped to `Compensated` or `Completed` participant statuses, 
 *     error execution is handled by exceptions thrown in the participant method
 *      -  not applicable for `@Status` participant methods
 *  - {@link org.eclipse.microprofile.lra.annotation.ParticipantStatus}
 *  - {@link javax.ws.rs.core.Response}: handled similarly as for JAX-RS participant methods
 *  - java.util.concurrent.CompletionStage: with the parameter of any of the previously 
 *    defined types
 * 
 * Arguments: up to 2 arguments of types in this order:
 *  - {@link java.net.URI}: representing current LRA context identification
 *  - {@link java.net.URI}: representing potentional parent LRA context identification
 *  
 * Any other signature will result in deployment / start time of this type.
 */
public class InvalidLRAParticipantDefinitionException extends RuntimeException {

    public InvalidLRAParticipantDefinitionException(String message) {
        super(message);
    }

    public InvalidLRAParticipantDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidLRAParticipantDefinitionException(Throwable cause) {
        super(cause);
    }
}
