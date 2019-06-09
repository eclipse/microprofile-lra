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

package org.eclipse.microprofile.lra.participant;

import org.eclipse.microprofile.lra.annotation.ParticipantStatus;

import javax.ws.rs.NotFoundException;
import java.net.URI;

// interface for synchronous non-JAXRS participants
public interface LRAParticipant {
    ParticipantStatus compensate(URI lra, URI parentLlra) throws NotFoundException;
    ParticipantStatus complete(URI lra, URI parentLlra) throws NotFoundException;
    void forget(URI lra);
}
