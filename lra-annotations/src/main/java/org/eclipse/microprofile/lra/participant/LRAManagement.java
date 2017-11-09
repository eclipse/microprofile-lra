/*
 * Copyright (c) 2007, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the Eclipse Foundation, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
