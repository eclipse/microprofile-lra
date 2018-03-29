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
package org.eclipse.microprofile.lra.tck;

import org.eclipse.microprofile.lra.client.LRAClient;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/tck")
public class TckResource {

    private static final String VERBOSE = "verbose";
    @Inject
    private LRAClient lraClient;

    private TckTests test;

    @PostConstruct
    private void setup() {
        TckTests.beforeClass(lraClient);
        test = new TckTests();
    }

    @PreDestroy
    private void tearDown() {
        TckTests.afterClass();
    }

    @PUT
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public TckResult runTck(@PathParam("name") String testName, @DefaultValue("true") @QueryParam(VERBOSE) boolean isVerbose) {
        test.before();

        TckResult results = test.runTck(lraClient, testName, isVerbose);

        test.after();

        return results;
    }
}
