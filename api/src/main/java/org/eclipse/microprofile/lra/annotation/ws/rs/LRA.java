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

package org.eclipse.microprofile.lra.annotation.ws.rs;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Complete;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * <p>
 * An annotation for controlling the lifecycle of Long Running Actions (LRAs).
 * </p>
 *
 * <p>
 * The annotation <b>SHOULD</b> be applied to JAX-RS annotated methods otherwise
 * it <b>MAY</b> have no effect. The annotation determines whether or not the
 * annotated method will run in the context of an LRA and controls whether or not:
 * </p>
 *
 * <ul>
 *   <li>any incoming context should be suspended and if so if a new one should be
 *       started</li>
 *   <li>to start a new LRA</li>
 *   <li>to end any LRA context when the method ends</li>
 *   <li>to return a error status code without running the annotated method if
 *       there should have been an LRA context present on method entry</li>
 *   <li>to cancel the LRA context when the method returns particular HTTP
 *       status codes</li>
 * </ul>
 *
 * <p>
 * Newly created LRAs are uniquely identified and the id is referred to as the
 * LRA context. The context is passed around using a JAX-RS request/response header
 * called {@value #LRA_HTTP_CONTEXT_HEADER}.
 * The implementation (of the LRA specification) is expected to manage this context
 * and the application developer is expected to declaratively control the creation,
 * propagation and destruction of LRAs using this {@link LRA} annotation. When a
 * JAX-RS resource method is invoked in the context of an LRA, any JAX-RS client
 * requests that it performs will carry the same header so that the receiving
 * resource knows that it is inside an LRA context. The behaviour may be overridden
 * by manually setting the context header.
 * </p>
 *
 * <p>
 * If an LRA is propagated to a resource that is not annotated with any
 * particular LRA behaviour then the LRA will be suspended (ie the context
 * will not be available to the resource). But if this resource
 * then performs an outgoing JAX-RS request then the suspended LRA must be propagated
 * on this second outgoing request. For example, suppose resource <code>A</code>
 * starts an LRA and then performs a JAX-RS request to resource <code>B</code> which
 * does not contain any LRA annotations. If resource <code>B</code> then performs a
 * JAX-RS request to a third service, <code>C</code> say, which does contain LRA
 * annotations then the LRA context started at <code>A</code> must be propagated
 * to <code>C</code> (for example if <code>C</code> uses an annotation to join the LRA,
 * then <code>C</code> must be enlisted in the LRA that was started at <code>A</code>).
 * </p>
 *
 * <p>
 * Resource methods can access the LRA context id by inspecting the request headers
 * using standard JAX-RS mechanisms, ie `@Context` or by injecting it via the JAX-RS
 * {@link HeaderParam} annotation with value {@value #LRA_HTTP_CONTEXT_HEADER}.
 * This may be useful, for example, for associating business work with an LRA.
 * </p>
 */
@Inherited
@Retention(value = RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LRA {
    /**
     * When a JAX-RS invocation is made with an active LRA it is made available
     * via an HTTP header field with the following name. The value contains
     * the LRA id associated with the HTTP request/response
     */
    String LRA_HTTP_CONTEXT_HEADER = "Long-Running-Action";

    /**
     * the name of the HTTP header field that contains a recovery URI corresponding
     * to a participant enlistment in an LRA
     */
    String LRA_HTTP_RECOVERY_HEADER = "Long-Running-Action-Recovery";

    /**
     * <p>
     *     The Type element of the LRA annotation indicates whether a resource method
     *     is to be executed within the context of an LRA.
     * </p>
     *
     * <p>
     *     If the method is to run in the context of an LRA and the annotated class
     *     also contains a method annotated with {@link Compensate}
     *     then the resource will be enlisted with the LRA. Enlisting with an LRA
     *     means that the resource will be notified when the current LRA is later
     *     cancelled. The resource can also receive notifications when the LRA is
     *     closed if it additionally defines a method annotated with {@link Complete}.
     * </p>
     *
     * <p>
     *     The element values {@link LRA.Type#REQUIRED} and
     *     {@link LRA.Type#REQUIRES_NEW} can start new LRAs.
     * </p>
     *
     * <p>
     *     If the method does run in the context of an LRA then the application
     *     can control whether or not it is closed when the method returns using
     *     the {@link LRA#end()} element.
     * </p>
     *
     * <p>
     *     When an LRA is present its identifier is made available to
     *     the business logic in the JAX-RS request and response headers with the
     *     name {@value #LRA_HTTP_CONTEXT_HEADER}.
     * </p>
     *
     * @return the type of behaviour expected when the annotated method is executed.
     */
    Type value() default Type.REQUIRED;

    enum Type {
        /**
         * <p>
         *     If called outside an LRA context the invoked method will run with a
         *     new context.
         * </p>
         *
         * <p>
         *     If called inside an LRA context the invoked method will run with the
         *     same context.
         * </p>
         */
        REQUIRED,

        /**
         * <p>
         *     If called outside an LRA context the invoked method will run with a
         *     new context.
         * </p>
         *
         * <p>
         *     If called inside an LRA context the invoked method will run with a
         *     new context. The original context is ignored.
         * </p>
         */
        REQUIRES_NEW,

        /**
         * <p>
         *     If called outside an LRA context the method is not executed and a
         *     <code>412 Precondition Failed</code> HTTP status code is returned
         *     to the caller.
         * </p>
         *
         * <p>
         *     If called inside a transaction context the resource method execution
         *     will then continue within that context.
         * </p>
         */
        MANDATORY,

        /**
         *  <p>
         *      If called outside an LRA context the resource method execution
         *      must then continue outside an LRA context.
         *  </p>
         *
         *  <p>
         *      If called inside an LRA context the resource method execution
         *      must then continue with the same LRA context.
         *  </p>
         */
        SUPPORTS,

        /**
         * <p>
         *     The resource method is executed without an LRA context.
         * </p>
         */
        NOT_SUPPORTED,

        /**
         * <p>
         *     If called outside an LRA context the resource method execution
         *     must then continue outside an LRA context.
         * </p>
         *
         * <p>
         *     If called inside an LRA context the method is not executed and a
         *     <code>412 Precondition Failed</code> HTTP status code is returned
         *     to the caller.
         * </p>
         */
        NEVER
    }

    /**
     * <p>
     * The period for which the LRA will remain valid. When this period has
     * elapsed the LRA becomes eligible for cancellation. The units are
     * specified in the {@link LRA#timeUnit()} element.
     * A value of zero indicates that the LRA will always remain valid.
     * </p>
     *
     * @return the period for which the LRA is guaranteed to run for before
     * becoming eligible for cancellation.
     */
    long timeLimit() default 0;

    /**
     * @return the unit of time that the {@link LRA#timeLimit()} element is
     * measured in.
     */
    ChronoUnit timeUnit() default ChronoUnit.SECONDS;

    /**
     * <p>
     * If the annotated method runs with an LRA context then this element determines
     * whether or not it is closed when the method returns. If the element has the
     * value {@literal true} then the LRA will be ended and if it has the value
     * {@literal false} then the LRA will not be ended.
     * </p>
     *
     * <p>
     * If the <code>end</code> value is set to {@literal false} but the annotated
     * method finishes with a status code that matches any of the values specified
     * in the {@link #cancelOn()} or {@link #cancelOnFamily()} elements
     * then the  LRA will be cancelled. In other words the
     * {@link #cancelOn()} and {@link #cancelOnFamily()} elements take precedence
     * over the <code>end</code> element.
     * </p>
     *
     * @return true if an LRA that was active when the method ran should be closed
     * when the method execution finishes.
     */
    boolean end() default true;

    /**
     * <p>
     * The cancelOnFamily element can be set to indicate which families of
     * HTTP response codes will cause the current LRA to cancel. If the LRA
     * has already been closed when the annotated method returns then this
     * element is silently ignored,
     * </p>
     *
     * <p>
     * If a JAX-RS method is annotated with this element and the method
     * returns a response code which matches any of the specified families
     * then the LRA will be cancelled. The method can return status codes
     * in a {@link Response} or via a JAX-RS exception mappper.
     * </p>
     *
     * @return the {@link Response.Status.Family} status families that will cause
     * cancellation of the LRA
     */
    Response.Status.Family[] cancelOnFamily() default {
        Response.Status.Family.CLIENT_ERROR, Response.Status.Family.SERVER_ERROR
    };

    /**
     * <p>
     * The cancelOn element can be set to indicate which  HTTP response
     * codes will cause the current LRA to cancel. If the LRA
     * has already been closed when the annotated method returns then this
     * element is silently ignored,
     * </p>
     *
     * <p>
     * If a JAX-RS method is annotated with this element and the method
     * returns a response code which matches any of the specified status
     * codes then the LRA will be cancelled. The method can return status
     * codes in a {@link Response} or via an exception mappper.
     * </p>
     *
     * @return the {@link Response.Status} HTTP status codes that will cause
     * cancellation of the LRA
     */
    Response.Status[] cancelOn() default {};
}
