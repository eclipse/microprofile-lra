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

package org.eclipse.microprofile.lra.annotation;

import javax.ws.rs.core.Response;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation for controlling the lifecycle of Long Running Actions (LRAs).
 *
 * The annotation SHOULD be applied to JAX-RS annotated methods otherwise
 * it MAY have no effect. The annotation determines whether or not the annotated
 * method will run in the context of an LRA and controls whether or not:
 *
 * - any incomming context should be suspended and if so if a new one should be started.
 * - to start an new LRA.
 * - to end any LRA context when the method ends.
 * - to throw an exception if there should be an LRA present on method entry.
 * - to throw an exception if the method returns particular HTTP status codes.
 *
 * Newly created LRAs are uniquely identified and the id is referred to as the
 * LRA context. The context is passed around using a JAX-RS request/response header
 * called {@link org.eclipse.microprofile.lra.client.LRAClient#LRA_HTTP_HEADER}.
 * The implementation (of the LRA specification) is expected to manage this context
 * and the application developer is expected to declaratively control the creation,
 * propagation and destruction of LRAs using the @LRA annotation. When a JAX-RS bean
 * method is invoked in the context of an LRA any JAX-RS client requests that it
 * performs will carry the same header so that the receiving resource knows that it
 * is inside an LRA context.
 *
 * If an LRA is propagated to a resource that is not annotated with any
 * particular LRA behaviour then the LRA will be suspended. But if this resource
 * then performs an outgoing JAX-RS request then the suspended LRA must be propagated
 * on this second outgoing request. For example, suppose resource A starts an LRA
 * and then performs a JAX-RS request to resource B which does not contain any LRA
 * annotations. If resource B then performs a JAX-RS request to a third service, C say,
 * which does contain LRA annotations then the LRA context started at A must be propagated
 * to C (for example if C uses LRAClient or annotations to join the LRA, then C must be
 * enlisted in the LRA that was started at A).
 *
 * Resource methods can access the LRA context id, if required, by injecting it via
 * the JAX-RS @HeaderParam annotation. This may be useful, for example, for
 * associating business work with an LRA.
 */
@Inherited
@Retention(value = RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface LRA {

    /**
     * The Type element of the LRA annotation indicates whether a bean method
     * is to be executed within the context of a LRA.
     *
     * If the method is to run in the context of an LRA and the annotated class
     * also contains methods annotated with {@link Compensate}/{@link Compensate}
     * then the bean will enlisted with the LRA. Enlisting with an LRA means that
     * the bean will be notified when the current LRA is later cancelled/closed.
     *
     * The element values
     * {@link LRA.Type#REQUIRED} and {@link LRA.Type#REQUIRES_NEW} can start
     * new LRAs which by default will be closed when the annotated method
     * completes. This default behaviour can be overridden using the
     * {@link LRA#terminal()} attribute which will leave the new LRA active
     * when the method completes. To force the LRA to cancel instead of complete
     * use the {@link LRA#cancelOnFamily()} or {@link LRA#cancelOn()} attributes.
     *
     * If an LRA was already present before the annotated method is invoked then it
     * will remain active after the method completes. This default behaviour can be
     * overridden using the {@link LRA#terminal()} attibute which will force
     * the LRA to complete or cancel (if the {@link LRA#cancelOnFamily()} or
     * {@link LRA#cancelOn()} attributes are present).
     *
     * When an LRA is present its identifer MUST be made available to
     * the business logic in the JAX-RS request and response header
     * {@link org.eclipse.microprofile.lra.client.LRAClient#LRA_HTTP_HEADER}
     *
     * @return whether a bean method is to be executed within a transaction context.
     */
    Type value() default Type.REQUIRED;

    enum Type {
        /**
         * If called outside a LRA context a new LRA will be created for the
         * the duration of the method call and when the call completes it will
         * be closed.
         *
         * If called inside a LRA context the invoked method will run with the
         * same context and the context will remain active after the method
         * completes.
         */
        REQUIRED,

        /**
         * If called outside a LRA context a new LRA will be created for the
         * the duration of the method call and when the call completes it will
         * be terminated (closed or cancelled).
         *
         * If called inside a LRA context it will be suspended and a new LRA
         * context will be created for the duration of the call. When the method
         * finishes this new LRA will be terminated (closed or cancelled) and
         * the original context will be resumed.
         *
         * But note that if there was already a context active before the method
         * was invoked and the {@link LRA#terminal} attribute is set to false
         * then the new LRA is left active. In this case the original LRA will
         * remain suspended.
         */
        REQUIRES_NEW,

        /**
         *  If called outside a transaction context, the method call will return
         *  with a 412 Precondition Failed HTTP status code
         *
         *  If called inside a transaction context the bean method execution will
         *  then continue within that context.
         */
        MANDATORY,

        /**
         *  If called outside a LRA context the bean method execution
         *  must then continue outside a LRA context.
         *
         *  If called inside a LRA context the managed bean method execution
         *  must then continue inside this LRA context.
         */
        SUPPORTS,

        /**
         *  The bean method is executed without a LRA context. If a context is
         *  present on entry then it is suspended and then resumed after the
         *  execution has completed.
         */
        NOT_SUPPORTED,

        /**
         *  If called outside a LRA context the managed bean method execution
         *  must then continue outside a LRA context.
         *
         *  If called inside a LRA context the method is not executed and a
         *  <code>412 Precondition Failed</code> HTTP status code is returned
         *  to the caller.
         */
        NEVER
    }

    /**
     * Normally if an LRA is present when a bean method is executed it will not
     * be ended when the method returns. To override this behaviour and force LRA
     * termination on exit use the terminal element
     *
     * @return true if an LRA that was present before method execution will be
     * terminated when the bean method finishes.
     */
    boolean terminal() default true;

    /**
     * The cancelOnFamily element can be set to indicate which families of
     * HTTP response codes will cause the current LRA to cancel. By default client
     * errors (4xx codes) and server errors (5xx codes) will result in
     * cancellation of the LRA.
     *
     * @return the {@link Response.Status.Family} families that will cause
     * cancellation of the LRA
     */
    Response.Status.Family[] cancelOnFamily() default {
    };

    /**
     * The cancelOn element can be set to indicate which  HTTP response
     * codes will cause the current LRA to cancel
     *
     * @return the {@link Response.Status} HTTP status codes that will cause
     * cancellation of the LRA
     */
    Response.Status[] cancelOn() default {};
}
