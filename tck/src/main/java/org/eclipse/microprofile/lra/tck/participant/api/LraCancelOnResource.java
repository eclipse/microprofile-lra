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
package org.eclipse.microprofile.lra.tck.participant.api;

import static org.eclipse.microprofile.lra.annotation.ws.rs.LRA.LRA_HTTP_CONTEXT_HEADER;

import java.net.URI;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;
import org.eclipse.microprofile.lra.annotation.ws.rs.LRA.Type;
import org.eclipse.microprofile.lra.tck.service.LRAMetricService;
import org.eclipse.microprofile.lra.tck.service.LRAMetricType;

@ApplicationScoped
@Path(LraCancelOnResource.LRA_CANCEL_ON_RESOURCE_PATH)
public class LraCancelOnResource {
    private static final Logger LOGGER = Logger.getLogger(LraCancelOnResource.class.getName());
    public static final String LRA_CANCEL_ON_RESOURCE_PATH = "lraresource-cancelon";

    @Inject
    private LRAMetricService lraMetricService;

    public static final String CANCEL_ON_FAMILY_DEFAULT_4XX = "cancelOnFamilyDefault4xx";
    /**
     * Default return status for cancelling LRA is <code>4xx</code> and <code>5xx</code>
     * @param lraId The LRA id generated for this action
     * @return JAX-RS response
     */
    @GET
    @Path(CANCEL_ON_FAMILY_DEFAULT_4XX)
    @LRA(value = Type.REQUIRED)
    public Response cancelOnFamilyDefault4xx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.status(Status.BAD_REQUEST).entity(lraId).build();
    }

    public static final String CANCEL_ON_FAMILY_DEFAULT_5XX = "cancelOnFamilyDefault5xx";
    /**
     * Default return status for cancelling LRA is <code>4xx</code> and <code>5xx</code>
     * @param lraId The LRA id generated for this action
     * @return JAX-RS response
     */
    @GET
    @Path(CANCEL_ON_FAMILY_DEFAULT_5XX)
    @LRA(value = Type.REQUIRED)
    public Response cancelOnFamilyDefault5xx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(lraId).build();
    }

    public static final String CANCEL_ON_FAMILY_3XX = "cancelOnFamily3xx";
    /**
     * Cancel on family is set to <code>3xx</code>. The <code>3xx</code> return code
     * has to cancel the LRA.
     * @param lraId The LRA id generated for this action
     * @return JAX-RS response
     */
    @GET
    @Path(CANCEL_ON_FAMILY_3XX)
    @LRA(value = Type.REQUIRES_NEW,
            cancelOnFamily = Family.REDIRECTION)
    public Response cancelOnFamily3xx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.status(Status.SEE_OTHER).entity(lraId).build();
    }

    public static final String CANCEL_ON_301 = "cancelOn301";
    /**
     * Cancel on is set to <code>301</code>. The <code>301</code> return code
     * has to cancel the LRA.
     * @param lraId The LRA id generated for this action
     * @return JAX-RS response
     */
    @GET
    @Path(CANCEL_ON_301)
    @LRA(value = Type.REQUIRES_NEW,
        cancelOn = {Status.MOVED_PERMANENTLY})
    public Response cancelOn301(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.status(Status.MOVED_PERMANENTLY).entity(lraId).build();
    }

    public static final String NOT_CANCEL_ON_FAMILY_5XX = "notCancelOnFamily5xx";
    /**
     * Cancel on family is set to <code>4xx</code>,
     * the code from other families (e.g. for <code>5xx</code>
     * should not cancel but should go with close the LRA.
     * @param lraId The LRA id generated for this action
     * @return JAX-RS response
     */
    @GET
    @Path(NOT_CANCEL_ON_FAMILY_5XX)
    @LRA(value = Type.REQUIRES_NEW,
            cancelOnFamily = {Family.CLIENT_ERROR})
    public Response notCancelOnFamily5xx(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        return Response.status(Status.INTERNAL_SERVER_ERROR).entity(lraId).build();
    }

    public static final String CANCEL_FROM_REMOTE_CALL = "cancelFromRemoteCall";
    /**
     * <p>
     * Returning <code>200</code> thus the LRA should be closed but
     * beforehand it makes a remote REST call which returns 5xx which is the
     * default for the cancelling and so the whole LRA should be cancelled.
     * </p>
     * <p>
     * The remote REST call invokes the same resource class {@link LraCancelOnResource}
     * That assumes the call to the representative of the same LRA participant
     * as it's already enlisted by the method {@link #cancelFromRemoteCall(java.net.URI, javax.ws.rs.core.UriInfo)} invoked by the test.
     * Because the specification mandates that the same participant can be enlisted
     * only once per LRA instance then
     * the {@link Compensate} method {@link #compensateWork(URI, String)}
     * will be called only once for the test invocation.
     * </p>
     * @param lraId The LRA id generated for this action
     * @return JAX-RS response
     */
    @GET
    @Path(CANCEL_FROM_REMOTE_CALL)
    @LRA(value = Type.REQUIRES_NEW)
    public Response cancelFromRemoteCall(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId, @Context UriInfo uriInfo) {
        Client client = ClientBuilder.newClient();

        try {
            Response response = client
                    .target(uriInfo.getBaseUri())
                    .path(LRA_CANCEL_ON_RESOURCE_PATH)
                    .path(LraCancelOnResource.CANCEL_ON_FAMILY_DEFAULT_5XX)
                    .request().get();
            assert response.getStatus() == 500;
        } finally {
            client.close();
        }
        return Response.ok(lraId).build();
    }


    @PUT
    @Path("/complete")
    @Complete
    public Response completeWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId) {
        if (lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        lraMetricService.incrementMetric(LRAMetricType.Completed, lraId);

        LOGGER.info(String.format("LRA id '%s' was completed", lraId.toASCIIString()));
        return Response.ok().build();
    }

    @PUT
    @Path("/compensate")
    @Compensate
    public Response compensateWork(@HeaderParam(LRA_HTTP_CONTEXT_HEADER) URI lraId, String userData) {
        if (lraId == null) {
            throw new NullPointerException("lraId can't be null as it should be invoked with the context");
        }

        lraMetricService.incrementMetric(LRAMetricType.Compensated, lraId);

        LOGGER.info(String.format("LRA id '%s' was compensated", lraId.toASCIIString()));
        return Response.ok().build();
    }
}
