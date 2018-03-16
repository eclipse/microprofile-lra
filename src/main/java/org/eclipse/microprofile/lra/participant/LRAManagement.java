/*
 *******************************************************************************
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
 *******************************************************************************/

package org.eclipse.microprofile.lra.participant;

import java.net.URL;
import java.util.concurrent.TimeUnit;

public interface LRAManagement {
    /**
     * Join an existing LRA
     *
     * @param participant an instance of a {@link LRAParticipant} that will be notified when the target LRA ends
     * @param deserializer a mechanism for recreating participants during recovery.
     *                     If the parameter is null then standard Java object deserialization will be used
     * @param lraId the LRA that the join request pertains to
     * @param timeLimit the time for which the participant should remain valid. When this time limit is exceeded
     *                  the participant may longer be able to fulfil the protocol guarantees.
     * @param unit the unit that the timeLimit parameter is expressed in
     */
    String joinLRA(LRAParticipant participant, LRAParticipantDeserializer deserializer,
                   URL lraId, Long timeLimit, TimeUnit unit) throws JoinLRAException;

    /**
     * Join an existing LRA. In contrast to the other form of registration this method does not indicate a time limit
     * for the participant meaning that the participant registration will remain valid until it terminates successfully
     * or unsuccessfully (ie it will never be timed out externally).
     *
     * @param participant an instance of a {@link LRAParticipant} that will be notified when the target LRA ends
     * @param deserializer a mechanism for recreating participants during recovery.
     *                     If the parameter is null then standard Java object deserialization will be used
     * @param lraId the LRA that the join request pertains to
     */
    String joinLRA(LRAParticipant participant, LRAParticipantDeserializer deserializer, URL lraId) throws JoinLRAException;
}
