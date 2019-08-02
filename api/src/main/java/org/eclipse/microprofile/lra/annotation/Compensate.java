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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * <p>
 * If a resource method executes in the context of an LRA and if the containing
 * class has a method annotated with <code>&#64;Compensate</code> then this
 * method will be invoked if the LRA is cancelled. The resource should attempt to
 * compensate for any actions it performed in the context of the LRA.
 * If the annotation is present on more than one method then an arbitrary one
 * will be chosen. The LRA specification makes no guarantees about when
 * Compensate method will be invoked, just that it will eventually be called.
 * </p>
 *
 * <p>
 * If the annotation is applied to a JAX-RS resource method then the request
 * method MUST be {@link javax.ws.rs.PUT}. The id of the currently
 * running LRA can be obtained by inspecting the incoming JAX-RS headers. If
 * this LRA is nested then the parent LRA MUST be present in the header with the name
 * {@link org.eclipse.microprofile.lra.annotation.ws.rs.LRA#LRA_HTTP_PARENT_CONTEXT_HEADER}
 * and the header value will be of type {@link java.net.URI}.
 * </p>
 *
 * <p>
 * If the annotated method is not a JAX-RS resource method then the id of the currently
 * running LRA and its parent LRA (if it is nested) can be obtained by adhering to
 * predefined method signatures as defined in the LRA specification document.
 * For example,
 * </p>
 *
 * <pre>
 *     <code>
 *        &#64;Compensate
 *        public void compensate(URI lraId, URI parentId) { ...}
 *     </code>
 * </pre>
 *
 * <p>
 * would be a valid compensation method declaration. If an invalid signature is detected 
 * the implementation of this specification MUST prohibit successful startup of the application
 * (e.g. with a runtime exception).
 * </p>
 *
 * <p>
 * If the participant cannot compensate immediately then it must report that the
 * compensation is in progress by either returning a future (such as
 * {@link java.util.concurrent.CompletionStage}) which will eventually report
 * one of the final states, or a <code>202 Accepted</code> JAX-RS response code or,
 * in the case of non JAX-RS resource methods, by returning
 * {@link ParticipantStatus#Compensating} (see the specification
 * document for more details).
 * </p>
 *
 * <p>
 * Note that, according to the state model defined by {@link LRAStatus}, it is not possible
 * to receive compensation notifications after an LRA has been asked to cancel.
 * Therefore combining this annotation with an <code>&#64;LRA</code> annotation that does not
 * start a new LRA will result in a <code>412 PreCondition Failed</code> JAX-RS response
 * code. On the other hand, combining it with an <code>&#64;LRA</code> annotation that
 * begins a new LRA can in certain uses case make sense, but in this case the LRA
 * that this method is being asked to compensate for will be unavailable.
 * </p>
 *
 * <p>
 * If the method is a JAX-RS resource method (or is a non JAX-RS method
 * annotated with <code>&#64;Compensate</code> with return type
 * <code>javax.ws.rs.core.Response</code>) then the following are the only
 * valid response codes:
 * </p>
 *
 *<table border="0" cellpadding="3" cellspacing="0"
 *   summary="Valid JAX-RS compensation response codes">
 * <caption>
 *     <span>JAX-RS Compensation Response Codes</span>
 *     <span>&nbsp;</span>
 * </caption>
 * <tr>
 *   <th scope="col">Code</th>
 *   <th scope="col">Response Body</th>
 *   <th scope="col">Meaning</th>
 * </tr>
 * <tr>
 *   <td scope="row">200</td>
 *   <td scope="row">Empty</td>
 *   <td scope="row">The resource has successfully compensated</td>
 * </tr>
 * <tr>
 *   <td scope="row">202</td>
 *   <td scope="row">Empty</td>
 *   <td scope="row">The resource is still attempting compensation</td>
 * </tr>
 * <tr>
 *   <td scope="row">410</td>
 *   <td scope="row">Empty</td>
 *   <td scope="row">The resource does not know about the LRA</td>
 * </tr>
 * <tr>
 *   <td scope="row">500</td>
 *   <td scope="row">{@link ParticipantStatus} enum value</td>
 *   <td scope="row"><p>The resource has failed to compensate.
 *   The payload contains the reason for the failure.
 *   A participant MUST remember this state until its
 *   {@link Forget} method is called.</p>
 *   <p>The actual value is not important but it MUST
 *   correspond to a valid {@link ParticipantStatus} enum value. For example,
 *   if compensation was not possible because the resource already
 *   completed (without being asked to) then a value such as
 *   {@link ParticipantStatus#Completed} would be appropriate or
 *   if it was due to a generic failure then
 *   {@link ParticipantStatus#FailedToCompensate} would be valid.
 *   </p>
 *   <p>
 *   Note that the
 *   actual state as reported by the {@link Status} method MUST
 *   be {@link ParticipantStatus#FailedToCompensate}</p></td>
 * </tr>
 * </table>
 *
 * <p>
 * The implementation will handle the return code 410 in the same way
 * as the return code 200. Specifically, when the implementation calls the Compensate method
 * as a result of the LRA being cancelled, and the participant returns the code
 * 410, the implementation assumes that the action is compensated and participant returns
 * a 410 since participant is allowed to forget about an action which is completely
 * handled by the participant.
 * </p>
 *
 * <p>
 * If any other code is returned (or, in the 500 case, the body does not
 * correspond to a valid state) then the implementation SHOULD either keep
 * retrying or attempt to discover the status by calling the
 * {@link Status} method if present or a combination of both.
 * If the implementation stops retrying then it SHOULD log a warning.
 * An example scenario where the implementation might attempt to invoke the
 * compensate method twice and the status method is as follows:
 * </p>
 *
 * <ol>
 * <li>The implementation invokes the compensate method via JAX-RS.</li>
 * <li>The JAX-RS server returns a 500 code (ie the notification does not reach the participant).</li>
 * <li>If there is a status method then the implementation uses that to get the current
 * state of the participant. If the status is Active then the implementation may
 * infer that the original request never reached the participant so it is safe to
 * reinvoke the compensate method.</li>
 * </ol>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Compensate {
    /**
     * The period for which the participant will guarantee it will be able
     * to compensate for any work that it performed during the associated LRA.
     * When this period elapses the LRA that it joined becomes eligible for
     * cancellation. The units are specified in the {@link #timeUnit()}
     * attribute.
     *
     * A value of zero indicates that it will always be able to compensate.
     *
     * @return the period for which the participant can guarantee it
     * will be able to compensate when asked to do so
     */
    long timeLimit() default 0;

    /**
     * @return the unit of time that the {@link #timeLimit()} attribute is
     * measured in.
     */
    ChronoUnit timeUnit() default ChronoUnit.SECONDS;
}
