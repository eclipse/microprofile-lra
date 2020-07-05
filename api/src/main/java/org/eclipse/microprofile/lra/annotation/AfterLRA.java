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
package org.eclipse.microprofile.lra.annotation;

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * If a JAX-RS resource method is annotated with
 * {@link LRA} and is invoked in the context of an LRA then
 * the resource can ask to be notified when the LRA finishes
 * by marking one of the other methods in the class with
 * the <code>&#64;AfterLRA</code> annotation.
 * </p>
 *
 * <p>
 * The listener can register interest in the final outcome of an LRA at
 * any time up until the LRA has closed or cancelled. In other words,
 * if an LRA is closing or cancelling then listener registrations
 * should be allowed. This is in contrast to registering for participant
 * callbacks which are only allowed if the LRA is active.
 * A consequence of this statement is that if a class is annotated with
 * both the AfterLRA and the Compensate annotations and the LRA has
 * already started closing or cancelling then the method invocation
 * will fail with a <code>412 PreCondition Failed</code> JAX-RS response
 * code.
 * </p>
 *
 * <p>
 * If the <code>AfterLRA</code> method is also a JAX-RS resource method
 * then it MUST use the {@link javax.ws.rs.PUT} request method. In this
 * case the LRA context is made available to the annotated method
 * via an HTTP header with the name
 * {@link LRA#LRA_HTTP_ENDED_CONTEXT_HEADER} and the
 * final status is passed to the method as plain text
 * corresponding to one of the {@link LRAStatus} enum values.
 * If this LRA was nested then the parent LRA MUST be present in the header
 * {@link org.eclipse.microprofile.lra.annotation.ws.rs.LRA#LRA_HTTP_PARENT_CONTEXT_HEADER}
 * and value is of type {@link java.net.URI}. For example:
 * </p>
 *
 * <pre>
 * <code>
 *   &#64;PUT
 *   &#64;AfterLRA
 *   public Response afterEnd(&#64;HeaderParam(LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId,
 *                            &#64;HeaderParam(LRA_HTTP_PARENT_CONTEXT_HEADER) URI parentLraId,
 *                            Status status)
 * </code>
 * </pre>
 *
 * <p>
 * The implementation SHOULD keep resending the notification
 * until it receives a <code>200 OK</code> status code from the
 * resource method (which means that the method SHOULD be
 * idempotent).
 * If it stops retrying a warning message SHOULD be logged.
 * </p>
 *
 * <p>
 * If the <code>AfterLRA</code> method is not a JAX-RS resource method
 * then the id of the LRA and its final status can be obtained
 * by ensuring that the annotated method conforms to the
 * signature:
 * </p>
 *
 * <pre>
 * <code>
 *     public void onLRAEnd(URI lraId, LRAStatus status)
 * </code>
 * </pre>
 *
 * <p>
 * The return type is ignored and the method name is not
 * significant.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface AfterLRA {
}
