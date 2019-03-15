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
package org.eclipse.microprofile.lra.tck.participant.activity;

import org.eclipse.microprofile.lra.tck.participant.api.LraCancelOnController;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@ApplicationScoped
public class ParticipantService {

    private static final Logger LOGGER = Logger.getLogger(LraCancelOnController.class.getName());

    private final List<String> completedParticipants = Collections.synchronizedList(new ArrayList<>());
    private final List<String> compensatedParticipants = Collections.synchronizedList(new ArrayList<>());
    
    public void clear() {
        completedParticipants.clear();
        compensatedParticipants.clear();
    }
    
    public void completeWork(String lraId, String participant) throws NotFoundException {
        if(lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        completedParticipants.add(participant);

        LOGGER.fine(String.format("LRA id '%s' was completed", lraId));
    }
    
    public void compensateWork(String lraId, String participant) throws NotFoundException {
        if(lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        compensatedParticipants.add(participant);

        LOGGER.fine(String.format("LRA id '%s' was compensated", lraId));
    }

    public List<String> completed() {
        return Collections.unmodifiableList(completedParticipants);
    }

    public List<String> compensated() {
        return Collections.unmodifiableList(compensatedParticipants);
    }
}
