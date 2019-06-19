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
package org.eclipse.microprofile.lra.tck.participant.activity;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * Storing activities processed by controllers during TCK suite run.
 */
@ApplicationScoped
public class ActivityStorage {
    private Map<URI, Activity> activities = new HashMap<>();

    public Activity getActivityAndAssertExistence(URI lraId, UriInfo jaxrsContext) {
        if(!activities.containsKey(lraId)) {
           String errorMessage = String.format("Activity store does not contain LRA id '%s', "
                   + "invoked from endpoint '%s'", lraId, jaxrsContext.getPath());
           throw new NotFoundException(Response.status(404).entity(errorMessage).build());
        }

        return activities.get(lraId);
    }

    public List<Activity> findAll() {
        return new ArrayList<>(activities.values());
    }

    public Activity add(Activity activity) {
        activities.putIfAbsent(activity.getLraId(), activity);
        return activities.get(activity.getLraId());
    }

    public void remove(URI id) {
        activities.remove(id);
    }
}
