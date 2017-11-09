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

import javax.ws.rs.NotFoundException;
import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * The API for notifying participants that a LRA is completing or cancelling.
 * A participant joins with an LRA via a call to
 * {@link LRAManagement#joinLRA(LRAParticipant, LRAParticipantDeserializer, URL, Long, TimeUnit)}
 */
public interface LRAParticipant extends Serializable {
    /**
     * Notifies the participant that the LRA is closing
     * @param lraId the LRA that is closing
     * @return null if the participant completed successfully. If the participant cannot
     *         complete immediately it should return a future that the caller can use
     *         to monitor progress. If the JVM crashes before the participant can finish
     *         it should expect this method to be called again. If the participant fails
     *         to complete it must cancel the future or throw a TerminationException.
     * @throws NotFoundException the participant does not know about this LRA
     * @throws TerminationException the participant was unable to complete and will never
     *         be able to do so
     */
    Future<Void> completeWork(URL lraId) throws NotFoundException, TerminationException;

    /**
     * Notifies the participant that the LRA is cancelling
     * @param lraId the LRA that is closing
     * @return null if the participant completed successfully. If the participant cannot
     *         complete immediately it should return a future that the caller can use
     *         to monitor progress. If the JVM crashes before the participant can finish
     *         it should expect this method to be called again. If the participant fails
     *         to complete it must cancel the future or throw a TerminationException.
     * @throws NotFoundException the participant does not know about this LRA
     * @throws TerminationException the participant was unable to complete and will never
     *         be able to do so
     */
    Future<Void> compensateWork(URL lraId) throws NotFoundException, TerminationException;
}

