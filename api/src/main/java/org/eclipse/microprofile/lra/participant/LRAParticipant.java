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

import javax.ws.rs.NotFoundException;
import java.io.Serializable;
import java.net.URL;
import java.util.concurrent.Future;

/**
 * The API for notifying participants that a LRA is completing or cancelling.
 * A participant joins with an LRA via a call to
 * {@link LRAManagement#joinLRA(LRAParticipant, URL, Long,
 * java.util.concurrent.TimeUnit)}
 */
public interface LRAParticipant extends Serializable {
    /**
     * Notifies the participant that the LRA is closing
     * @param lraId the LRA that is closing
     * @return null if the participant completed successfully. If the participant
     *         cannot complete immediately it should return a future that the caller
     *         can useto monitor progress. If the JVM crashes before the participant
     *         can finish it should expect this method to be called again. If the
     *         participant fails to complete it must cancel the future or throw a
     *         TerminationException.
     * @throws NotFoundException the participant does not know about this LRA
     * @throws TerminationException the participant was unable to complete and will
     *         never be able to do so
     */
    Future<Void> completeWork(URL lraId)
            throws NotFoundException, TerminationException;

    /**
     * Notifies the participant that the LRA is cancelling
     * @param lraId the LRA that is closing
     * @return null if the participant completed successfully. If the participant
     *         cannot complete immediately it should return a future that the
     *         caller can use to monitor progress. If the JVM crashes before
     *         the participant can finish it should expect this method to be
     *         called again. If the participant fails to complete it must cancel
     *         the future or throw a TerminationException.
     * @throws NotFoundException the participant does not know about this LRA
     * @throws TerminationException the participant was unable to complete and
     *         will never be able to do so
     */
    Future<Void> compensateWork(URL lraId)
            throws NotFoundException, TerminationException;
}

