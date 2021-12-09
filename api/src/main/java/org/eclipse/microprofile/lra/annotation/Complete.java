/*
 *******************************************************************************
 * Copyright (c) 2018-2021 Contributors to the Eclipse Foundation
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

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

/**
 * <p>
 * If a resource method executes in the context of an LRA and if the containing class has a method annotated with
 * <code>&#64;Complete</code> (as well as method annotated with <code>&#64;Compensate</code>) then this Complete method
 * will be invoked if the LRA is closed. The resource should attempt to perform any clean up activities relating to any
 * actions it performed in the context of the LRA. If the annotation is present on more than one method then an
 * arbitrary one will be chosen. The LRA specification makes no guarantees about when the Complete method will be
 * invoked, just that it will eventually be called.
 * </p>
 *
 * <p>
 * In the case where the ability to complete the Long Running Action is time bounded, you can limit the lifespan of the
 * Long Running action by providing values for the {@link LRA#timeLimit()} and {@link LRA#timeUnit()} timeUnit}
 * attributes. When the time limit is reached the LRA becomes eligible for automatic cancellation.
 * </p>
 *
 * <p>
 * If the annotation is applied to a JAX-RS resource method then the request method MUST be {@link jakarta.ws.rs.PUT}.
 * The id of the currently running LRA can be obtained by inspecting the incoming JAX-RS headers. If this LRA is nested
 * then the parent LRA MUST be present in the header with the name {@link LRA#LRA_HTTP_PARENT_CONTEXT_HEADER} and the
 * header value will be of type {@link java.net.URI}.
 * </p>
 *
 * <p>
 * If the annotated method is not a JAX-RS resource method then the id of the currently running LRA and its parent LRA
 * (if it is nested) can be obtained by adhering to predefined method signatures as defined in the LRA specification
 * document. For example,
 * </p>
 *
 * <pre>
 *     <code>
 *        &#64;Complete
 *        public void complete(URI lraId, URI parentId) { ...}
 *     </code>
 * </pre>
 *
 * <p>
 * would be a valid completion method declaration. If an invalid signature is detected the implementation of this
 * specification MUST prohibit successful startup of the application (e.g. with a runtime exception).
 * </p>
 *
 * <p>
 * If the participant cannot complete immediately then it must report that completion is in progress by either returning
 * a future (such as {@link java.util.concurrent.CompletionStage}) which will eventually report one of the final states,
 * or a <code>202 Accepted</code> JAX-RS response code or, in the case of non JAX-RS resource methods, by returning
 * {@link ParticipantStatus#Completing} (see the specification document for more details).
 * </p>
 *
 * <p>
 * Note that according to the state model defined by {@link LRAStatus}, it is not possible to receive completion
 * notifications after an LRA has been asked to close. Therefore combining this annotation with an <code>&#64;LRA</code>
 * annotation that does not start a new LRA will result in a <code>412 PreCondition Failed</code> JAX-RS response code.
 * On the other hand, combining it with an <code>&#64;LRA</code> annotation that begins a new LRA can in certain use
 * cases make sense, but in this case the LRA that this method is being asked to complete for will be unavailable.
 * </p>
 *
 * <p>
 * If the method is a JAX-RS resource method (or is a non JAX-RS method annotated with <code>&#64;Complete</code> with
 * return type <code>jakarta.ws.rs.core.Response</code>) then the following are the only valid response codes:
 * </p>
 *
 * <table border="0" cellpadding="3" cellspacing="0" summary="Valid JAX-RS completion response codes">
 * <caption><span>JAX-RS Completion Response Codes</span><span>&nbsp;</span></caption>
 * <tr>
 * <th scope="col">Code</th>
 * <th scope="col">Response Body</th>
 * <th scope="col">Meaning</th>
 * </tr>
 * <tr>
 * <td scope="row">200</td>
 * <td scope="row">Empty</td>
 * <td scope="row">The resource has successfully completed</td>
 * </tr>
 * <tr>
 * <td scope="row">202</td>
 * <td scope="row">Empty</td>
 * <td scope="row">The resource is still attempting completion</td>
 * </tr>
 * <tr>
 * <td scope="row">409</td>
 * <td scope="row">{@link ParticipantStatus} enum value</td>
 * <td scope="row">
 * <p>
 * The resource has failed to complete. The payload contains the reason for the failure. A participant MUST remember
 * this state until its {@link Forget} method is called.
 * </p>
 * <p>
 * The actual value is not important but it MUST correspond to a valid {@link ParticipantStatus} enum value. For
 * example, if completion was not possible because the resource already compensated (without being asked to) then a
 * value such as {@link ParticipantStatus#Compensated} would be appropriate or if it was due to a generic failure then
 * {@link ParticipantStatus#FailedToComplete} would be valid. If the response body does not contain a valid status then
 * the implementation MUST either reinvoke the method or discover the status using the {@link Status} annotation if
 * present.
 * </p>
 * <p>
 * Note that the actual state as reported by the {@link Status} method MUST be
 * {@link ParticipantStatus#FailedToComplete}
 * </p>
 * </td>
 * </tr>
 * <tr>
 * <td scope="row">410</td>
 * <td scope="row">Empty</td>
 * <td scope="row">The resource does not know about the LRA</td>
 * </tr>
 * </table>
 *
 * <p>
 * The implementation will handle the return code 410 in the same way as the return code 200. Specifically, when the
 * implementation calls the Complete method as a result of the LRA being closed and the participant returns the code
 * 410, the implementation assumes that the action is completed and participant returns a 410 since participant is
 * allowed to forget about an action which is completely handled by the participant.
 * </p>
 *
 * <p>
 * If any other code is returned (or, in the 409 case, the body does not correspond to a valid state) then the
 * implementation SHOULD either keep retrying or attempt to discover the status by calling the {@link Status} method if
 * present or a combination of both. If the implementation stops retrying then it SHOULD log a warning. An example
 * scenario where the implementation might attempt to invoke the complete method twice and the status method is as
 * follows:
 * </p>
 *
 * <ol>
 * <li>The implementation invokes the complete method via JAX-RS.</li>
 * <li>The JAX-RS server returns a 500 code (i.e., the notification does not reach the participant).</li>
 * <li>If there is a status method then the implementation uses that to get the current state of the participant. If the
 * status is Active then the implementation may infer that the original request never reached the participant so it is
 * safe to reinvoke the complete method.</li>
 * </ol>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Complete {

}
