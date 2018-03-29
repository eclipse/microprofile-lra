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
package org.eclipse.microprofile.lra.tck.participant.service;

import org.eclipse.microprofile.lra.tck.participant.model.Activity;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ActivityService {
    private Map<String, Activity> activities = new HashMap<>();

    public Activity getActivity(String txId) throws NotFoundException {
        if (!activities.containsKey(txId))
            throw new NotFoundException(Response.status(404).entity("Invalid activity id: " + txId).build());

        return activities.get(txId);
    }

    public List<Activity> findAll() {
        return new ArrayList<>(activities.values());
    }

    public void add(Activity activity) {
        activities.putIfAbsent(activity.getId(), activity);
    }

    public void remove(String id) {
        activities.remove(id);
    }
}
