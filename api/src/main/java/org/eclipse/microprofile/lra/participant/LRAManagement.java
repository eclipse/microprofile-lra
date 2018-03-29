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

import javax.enterprise.context.ApplicationScoped;
import java.net.URL;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public interface LRAManagement {
    /**
     * Join an existing LRA.
     *
     * @param participant an instance of a {@link LRAParticipant} that will be
     *                   notified when the target LRA ends
     * @param lraId the LRA that the join request pertains to @param timeLimit
     *             the time for which the participant should remain valid. When
     *             this time limit is exceeded the participant may longer be able
     *             to fulfil the protocol guarantees.
     * @param unit the unit that the timeLimit parameter is expressed in
     *
     * @return a recovery URL for this enlistment
     *
     * @throws JoinLRAException if the request to the coordinator failed.
     * {@link JoinLRAException#getCause()} and/or
     * {@link JoinLRAException#getStatusCode()} may provide a more specific reason
     */
    String joinLRA(LRAParticipant participant, URL lraId, Long timeLimit,
                   TimeUnit unit)
            throws JoinLRAException;

    /**
     * Join an existing LRA. In contrast to the other form of registration this
     * method does not indicate a time limit for the participant meaning that the
     * participant registration will remain valid until it terminates successfully
     * or unsuccessfully (ie it will never be timed out externally).
     *
     * @param participant an instance of a {@link LRAParticipant} that will be
     *                   notified when the target LRA ends
     * @param lraId the LRA that the join request pertains to
     *
     * @return a recovery URL for this enlistment
     *
     * @throws JoinLRAException if the request to the coordinator failed.
     * {@link JoinLRAException#getCause()} and/or
     * {@link JoinLRAException#getStatusCode()} may provide a more specific reason
     */
    String joinLRA(LRAParticipant participant, URL lraId) throws JoinLRAException;

    /**
     * Register an object for recreating participants during recovery. Use this
     * mechanism after a JVM that hosted a LRA participant has terminated with
     * outstanding LRAs. The LRA recovery coordinator will use this to ask the
     * application to recreate an instance of LRAParticipant that will be notified
     * when an LRA is closing or canceling.
     *
     * @param deserializer an object that knows how to recreate participants.
     *                     Note that when the LRA manager is recreating a
     *                     participant it may run through all registered
     *                     deserializers. In recovery scenarios the first such
     *                     deserializer returning a valid {@link LRAParticipant}
     *                     is used for sending completion or compensation
     *                     notifications
     */
    void registerDeserializer(LRAParticipantDeserializer deserializer);

    /**
     * Unregister a participant deserializer. The first deserializer registered
     * with the LRA manager for which the equals method returns true is removed
     * from LRA manager
     *
     * @param deserializer the deserializer to unregister
     */
    void unregisterDeserializer(LRAParticipantDeserializer deserializer);
}
