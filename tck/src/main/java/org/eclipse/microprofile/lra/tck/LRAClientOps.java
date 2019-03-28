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
package org.eclipse.microprofile.lra.tck;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.client.GenericLRAException;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.temporal.ChronoUnit;

import static junit.framework.TestCase.fail;
import static org.eclipse.microprofile.lra.tck.participant.api.NonParticipatingTckResource.END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.NonParticipatingTckResource.START_BUT_DONT_END_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.NonParticipatingTckResource.STATUS_CODE_QUERY_NAME;
import static org.eclipse.microprofile.lra.tck.participant.api.NonParticipatingTckResource.TCK_NON_PARTICIPANT_RESOURCE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.JOIN_WITH_EXISTNG_LRA_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.LEAVE_PATH;
import static org.eclipse.microprofile.lra.tck.participant.api.ParticipatingTckResource.TCK_PARTICIPANT_RESOURCE_PATH;

public class LRAClientOps {
    /**
     * TODO recovery needs to be properly speced
     * It is used in the two tests for participants that return 202:
     * {@link TckTests#acceptCancelTest()}  and {@link TckTests#acceptCloseTest()}
     * to connect to an endpoint that will trigger recovery (the alternative would
     * be to wait)
     */

    /**
     * Key for looking up the config property that specifies which host a
     * recovery coordinator is running on
     */
    static final String LRA_RECOVERY_HOST_KEY = "lra.http.recovery.host";

    /**
     * Key for looking up the config property that specifies which port a
     * recovery coordinator is listening on
     */
    static final String LRA_RECOVERY_PORT_KEY = "lra.http.recovery.port";

    /**
     * Key for looking up the config property that specifies which JAX-RS path a
     * recovery coordinator is running on
     */
    static final String LRA_RECOVERY_PATH_KEY = "lra.http.recovery.path";

    private final WebTarget target;

    public LRAClientOps(WebTarget target) {
        this.target = target;
    }

    // see if it is possible to join with an LRA - if it is possible to do that then the LRA is still active
    private int tryToEnlistWithAnLRA(String lra) {
        // call a JAX-RS endpint that should result in the enlistment of a resource into the LRA
        int status = invokeRestEndpointAndReturnStatus(lra, TCK_PARTICIPANT_RESOURCE_PATH, JOIN_WITH_EXISTNG_LRA_PATH, 200);

        if (status == 200) {
            // leave the LRA otherwise any tests checking completion/compensation counts would fail
            try {
                leaveLRA(new URI(lra), TCK_PARTICIPANT_RESOURCE_PATH, LEAVE_PATH);
            } catch (URISyntaxException e) {
                fail("tryToEnlistWithAnLRA: invalid URI " + lra);
            }
        }

        return status;
    }

    boolean isLRAFinished(URI lra) {
        return isLRAFinished(lra.toASCIIString());
    }

    boolean isLRAFinished(String lra) {
        // if the LRA has finished/finishing or does not exist 412 or 404 MUST be be reported
        int status = tryToEnlistWithAnLRA(lra);

        return status == Response.Status.NOT_FOUND.getStatusCode() || status == Response.Status.PRECONDITION_FAILED.getStatusCode();
    }

    private Response invokeRestEndpoint(URI lra, String basePath, String path, int coerceResponse) {
        return invokeRestEndpoint(lra == null ? null : lra.toASCIIString(), basePath, path, coerceResponse);
    }

    private Response invokeRestEndpoint(String lra, String basePath, String path, int coerceResponse) {
        WebTarget resourcePath = target.path(basePath).path(path).queryParam(STATUS_CODE_QUERY_NAME, coerceResponse);
        Invocation.Builder builder = resourcePath.request();

        if (lra != null) {
            builder.header(LRA.LRA_HTTP_HEADER, lra);
        }

        return builder.put(Entity.text(""));
    }

    public String invokeRestEndpointAndReturnLRA(String lra, String basePath, String path, int coerceResponse) {
        Response response = invokeRestEndpoint(lra, basePath, path, coerceResponse);

        try {
            return response.readEntity(String.class);
        } finally {
            response.close();
        }
    }

    private int invokeRestEndpointAndReturnStatus(String lra, String basePath, String path, int coerceResponse) {
        Response response = invokeRestEndpoint(lra, basePath, path, coerceResponse);

        try {
            return response.getStatus();
        } finally {
            response.close();
        }
    }

    private URI toURI(String lra) throws GenericLRAException {
        try {
            return new URI(lra);
        } catch (URISyntaxException e) {
            throw new GenericLRAException(null, 0, e.getMessage(), e);
        }
    }

    public URI startLRA(URI parentLRA, String clientID, Long timeout, ChronoUnit unit)
            throws GenericLRAException {
        return toURI(invokeRestEndpoint(parentLRA,
                TCK_NON_PARTICIPANT_RESOURCE_PATH, START_BUT_DONT_END_PATH, 200)
                .readEntity(String.class));
    }

    void cancelLRA(String lra) {
        invokeRestEndpointAndReturnLRA(lra, TCK_NON_PARTICIPANT_RESOURCE_PATH, END_PATH, 200);
    }

    void cancelLRA(URI lraId) throws GenericLRAException {
        invokeRestEndpointAndReturnLRA(lraId.toASCIIString(), TCK_NON_PARTICIPANT_RESOURCE_PATH, END_PATH, 500);
    }

    void closeLRA(URI lraId) throws GenericLRAException {
        invokeRestEndpointAndReturnLRA(lraId.toASCIIString(), TCK_NON_PARTICIPANT_RESOURCE_PATH, END_PATH, 200);
    }


    public void closeLRA(String lraId) {
        invokeRestEndpointAndReturnLRA(lraId, TCK_NON_PARTICIPANT_RESOURCE_PATH, END_PATH, 200);
    }

    void leaveLRA(URI lra, String basePath, String resourcePath) throws GenericLRAException {
        invokeRestEndpoint(lra, basePath, resourcePath, 200);
    }
}
