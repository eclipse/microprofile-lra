/*
 *******************************************************************************
 * Copyright (c) 2020-2021 Contributors to the Eclipse Foundation
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

package org.eclipse.microprofile.lra;

import javax.ws.rs.core.Response;

/**
 * The utility class that will create the correct {@link Response} or {@link javax.ws.rs.core.Response.ResponseBuilder}
 * for the response that should be returned from the LRA JAX-RS methods.
 */
public final class LRAResponse {

    private LRAResponse() {}

    public static Response compensated() {
        return Builder.compensated().build();
    }

    public static Response compensated(Object entity) {
        return Builder.compensated(entity).build();
    }

    public static Response compensating() {
        return Builder.compensating().build();
    }

    public static Response compensating(Object entity) {
        return Builder.compensating(entity).build();
    }

    public static Response failedToCompensate() {
        return Builder.failedToCompensate().build();
    }

    public static Response failedToCompensate(Object entity) {
        return Builder.failedToCompensate(entity).build();
    }

    public static Response completed() {
        return Builder.completed().build();
    }

    public static Response completed(Object entity) {
        return Builder.completed(entity).build();
    }

    public static Response completing() {
        return Builder.completing().build();
    }

    public static Response completing(Object entity) {
        return Builder.completing(entity).build();
    }

    public static Response failedToComplete() {
        return Builder.failedToComplete().build();
    }

    public static Response failedToComplete(Object entity) {
        return Builder.failedToComplete(entity).build();
    }

    public static final class Builder {
        public static Response.ResponseBuilder compensated() {
            return Response.ok();
        }

        public static Response.ResponseBuilder compensated(Object entity) {
            return Response.ok(entity);
        }

        public static Response.ResponseBuilder compensating() {
            return Response.accepted();
        }

        public static Response.ResponseBuilder compensating(Object entity) {
            return Response.accepted(entity);
        }

        public static Response.ResponseBuilder failedToCompensate() {
            return Response.status(Response.Status.CONFLICT);
        }

        public static Response.ResponseBuilder failedToCompensate(Object entity) {
            return Response.status(Response.Status.CONFLICT).entity(entity);
        }

        public static Response.ResponseBuilder completed() {
            return Response.ok();
        }

        public static Response.ResponseBuilder completed(Object entity) {
            return Response.ok(entity);
        }

        public static Response.ResponseBuilder completing() {
            return Response.accepted();
        }

        public static Response.ResponseBuilder completing(Object entity) {
            return Response.accepted(entity);
        }

        public static Response.ResponseBuilder failedToComplete() {
            return Response.status(Response.Status.CONFLICT);
        }

        public static Response.ResponseBuilder failedToComplete(Object entity) {
            return Response.status(Response.Status.CONFLICT).entity(entity);
        }
    }
}
