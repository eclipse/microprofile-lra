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
 * If the <code>AfterLRA</code> method is also a JAX-RS resource method
 * then the LRA context is made available to the annotated method
 * via an HTTP header with the name
 * {@link LRA#LRA_HTTP_ENDED_CONTEXT_HEADER} and the
 * final status is passed to the method as plain text
 * corresponding to one of the {@link LRAStatus} enum values.
 * For example:
 * </p>
 *
 * <pre>
 * <code>
 *   &#64;PUT
 *   &#64;AfterLRA
 *   public Response afterEnd(@HeaderParam(LRA_HTTP_ENDED_CONTEXT_HEADER) URI lraId,
 *                          LRAStatus status)
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
