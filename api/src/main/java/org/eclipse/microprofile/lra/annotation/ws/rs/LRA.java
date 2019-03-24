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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * <p>
 * An annotation for controlling the lifecycle of Long Running Actions (LRAs).
 *
 * <p>
 * The annotation <b>SHOULD</b> be applied to JAX-RS annotated methods otherwise
 * it <b>MAY</b> have no effect. The annotation determines whether or not the annotated
 * method will run in the context of an LRA and controls whether or not:
 * </p>
 *
 * <ul>
 *   <li>any incoming context should be suspended and if so if a new one should be started</li>
 *   <li>to start a new LRA</li>
 *   <li>to end any LRA context when the method ends</li>
 *   <li>to throw an exception if there should be an LRA context present on method entry</li>
 *   <li>to cancel LRA context when the method returns particular HTTP status codes</li>
 * </ul>
 *
 * <p>
 * Newly created LRAs are uniquely identified and the id is referred to as the
 * LRA context. The context is passed around using a JAX-RS request/response header
 * called {@value #LRA_HTTP_HEADER}.
 * The implementation (of the LRA specification) is expected to manage this context
 * and the application developer is expected to declaratively control the creation,
 * propagation and destruction of LRAs using the {@link LRA} annotation. When a JAX-RS bean
 * method is invoked in the context of an LRA any JAX-RS client requests that it
 * performs will carry the same header so that the receiving resource knows that it
 * is inside an LRA context.
 * </p>
 *
 * <p>
 * If an LRA is propagated to a resource that is not annotated with any
 * particular LRA behaviour then the LRA will be suspended (ie the context
 * will not be available to the resource). But if this resource
 * then performs an outgoing JAX-RS request then the suspended LRA must be propagated
 * on this second outgoing request. For example, suppose resource <code>A</code> starts an LRA
 * and then performs a JAX-RS request to resource <code>B</code> which does not contain any LRA
 * annotations. If resource <code>B</code> then performs a JAX-RS request to a third service, <code>C</code> say,
 * which does contain LRA annotations then the LRA context started at <code>A</code> must be propagated
 * to <code>C</code> (for example if <code>C</code> uses an annotation to join the LRA,
 * then <code>C</code> must be enlisted in the LRA that was started at <code>A</code>).
 * </p>
 *
 * <p>
 * Resource methods can access the LRA context id, if required, by inspecting the request headers
 * using standard JAX-RS mechanisms, ie `@Context` or by injecting it via the JAX-RS {@link HeaderParam}
 * annotation with value {@value #LRA_HTTP_HEADER}. This may be useful, for example, for
 * associating business work with an LRA.
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
    String LRA_HTTP_HEADER = "Long-Running-Action";

    /**
     * the name of the HTTP header field that contains a recovery URI corresponding
     * to a participant enlistment in an LRA
     */
    String LRA_HTTP_RECOVERY_HEADER = "Long-Running-Action-Recovery";

    /**
     * <p>
     * The Type element of the LRA annotation indicates whether a bean method
     * is to be executed within the context of an LRA.
     *
     * <p>
     * If the method is to run in the context of an LRA and the annotated class
     * also contains methods annotated with {@link Compensate}/{@link Compensate}
     * then the bean will enlisted with the LRA. Enlisting with an LRA means that
     * the bean will be notified when the current LRA is later cancelled/closed.
     *
     * <p>
     * The element values
     * {@link LRA.Type#REQUIRED} and {@link LRA.Type#REQUIRES_NEW} can start
     * new LRAs which by default will be closed when the annotated method
     * completes. This default behaviour can be overridden using the
     * {@link LRA#end()} attribute which will leave the new LRA active
     * when the method completes. To force the LRA to cancel instead of complete
     * use the {@link LRA#cancelOnFamily()} or {@link LRA#cancelOn()} attributes.
     *
     * <p>
     * If an LRA was already present before the annotated method is invoked then it
     * will remain active after the method completes. This default behaviour can be
     * overridden using the {@link LRA#end()} attibute which will force
     * the LRA to complete or cancel (if the {@link LRA#cancelOnFamily()} or
     * {@link LRA#cancelOn()} attributes are present).
     *
     * <p>
     * When an LRA is present its identifer <b>MUST</b> be made available to
     * the business logic in the JAX-RS request and response header
     * {@value #LRA_HTTP_HEADER}.
     *
     * @return whether a bean method is to be executed within a transaction context.
     */
    Type value() default Type.REQUIRED;

    enum Type {
        /**
         * <p>
         * If called outside an LRA context a new LRA will be created for the
         * the duration of the method call and when the call completes it will
         * be closed.
         * </p>
         *
         * <p>
         * If called inside an LRA context the invoked method will run with the
         * same context and the context will remain active after the method
         * completes.
         * </p>
         */
        REQUIRED,

        /**
         * <p>
         * If called outside an LRA context a new LRA will be created for the
         * the duration of the method call and when the call completes it will
         * be terminated (closed or cancelled).
         * </p>
         *
         * <p>
         * If called inside an LRA context it will be suspended and a new LRA
         * context will be created for the duration of the call. When the method
         * finishes this new LRA will be terminated (closed or cancelled) and
         * the original context will be resumed.
         * </p>
         *
         * <p>
         * But note that if there was already a context active before the method
         * was invoked and the {@link LRA#end} attribute is set to false
         * then the new LRA is left active. In this case the original LRA will
         * remain suspended.
         * </p>
         */
        REQUIRES_NEW,

        /**
         * <p>
         * If called outside a transaction context, the method call will return
         * with a <code>412 Precondition Failed</code> HTTP status code
         * </p>
         *
         * <p>
         * If called inside a transaction context the bean method execution will
         * then continue within that context.
         * </p>
         */
        MANDATORY,

        /**
         *  <p>
         *  If called outside an LRA context the bean method execution
         *  must then continue outside an LRA context.
         *  </p>
         *
         *  <p>
         *  If called inside an LRA context the managed bean method execution
         *  must then continue inside this LRA context.
         *  </p>
         */
        SUPPORTS,

        /**
         * The bean method is executed without an LRA context. If a context is
         * present on entry then it is suspended and then resumed after the
         * execution has completed.
         */
        NOT_SUPPORTED,

        /**
         * <p>
         * If called outside an LRA context the managed bean method execution
         * must then continue outside an LRA context.
         * </p>
         *
         * <p>
         * If called inside an LRA context the method is not executed and a
         * <code>412 Precondition Failed</code> HTTP status code is returned
         * to the caller.
         * </p>
         */
        NEVER
    }

    /**
     * The period for which the LRA will remain valid. When this period has
     * elapsed the LRA becomes eligble for cancellation. The units are
     * specified in the {@link LRA#timeUnit()} attribute.
     * A value of zero indicates that the LRA will always remain valid.
     *
     * @return the period for which the LRA is guaranteed to run for before
     * becoming eligible for cancellation.
     */
    long timeLimit() default 0;

    /**
     * @return the unit of time that the {@link LRA#timeLimit()} attribute is
     * measured in.
     */
    ChronoUnit timeUnit() default ChronoUnit.SECONDS;

    /**
     * <p>
     * Normally if an LRA is present when a bean method is executed it will not
     * be ended when the method returns. To override this behaviour and force LRA
     * termination on exit use the end element.
     * </p>
     *
     * <p>
     * If the <code>end</code> value is set to <code>false</code>
     * while the annotated method returns the {@link Response} status code
     * with the meaning the LRA to be cancelled (see {@link #cancelOn()} and {@link #cancelOnFamily()})
     * then this error state has precedence over the <code>end</code> attribute.
     * It means the LRA will be cancelled despite the end was set to <code>false</code>.
     * </p>
     *
     * @return true if an LRA that was present before method execution will be
     * terminated when the bean method finishes.
     */
    boolean end() default true;

    /**
     * <p>
     * The cancelOnFamily element can be set to indicate which families of
     * HTTP response codes will cause the current LRA to cancel.
     * This expects the annotated method is the JAX-RS endpoint where the {@link Response}
     * with the status code could be returned.
     * </p>
     *
     * <p>
     * By default client errors (<code>4xx codes</code>) and server errors (<code>5xx codes</code>)
     * will result in cancellation of the LRA.
     * </p>
     *
     * @return the {@link Response.Status.Family} families that will cause
     * cancellation of the LRA
     */
    Response.Status.Family[] cancelOnFamily() default {
        Response.Status.Family.CLIENT_ERROR, Response.Status.Family.SERVER_ERROR
    };

    /**
     * The cancelOn element can be set to indicate which  HTTP response
     * codes will cause the current LRA to cancel.
     * This expects the annotated method is the JAX-RS endpoint where the {@link Response}
     * with the status code could be returned.
     *
     * @return the {@link Response.Status} HTTP status codes that will cause
     * cancellation of the LRA
     */
    Response.Status[] cancelOn() default {};
}
