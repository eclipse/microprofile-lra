/*
 *******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.lra.tck.service;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.net.URI;

/**
 * JAX-RS endpoints for the {@link LRAMetricService}.
 */
@Path(LRAMetricRest.LRA_TCK_METRIC_RESOURCE_PATH)
public class LRAMetricRest {
    public static final String LRA_TCK_METRIC_RESOURCE_PATH = "lra-tck-metric";
    public static final String METRIC_PATH = "metric";

    public static final String METRIC_TYPE_PARAM = "metricType";
    public static final String LRA_ID_PARAM = "lraId";
    public static final String PARTICIPANT_NAME_PARAM = "participantName";

    @Inject
    private LRAMetricService lraMetricService;

    @Path(METRIC_PATH)
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public int getMetric(@QueryParam(METRIC_TYPE_PARAM) LRAMetricType metricType, @QueryParam(LRA_ID_PARAM) URI lra,
                         @QueryParam(PARTICIPANT_NAME_PARAM) String participantName) {
        if (metricType == null) {
            throw new NullPointerException("metricType");
        }
        if (lra == null) {
            throw new NullPointerException("lraId");
        }
        if (participantName == null) {
            throw new NullPointerException("participantName");
        }
        return lraMetricService.getMetric(metricType, lra, participantName);
    }
}
