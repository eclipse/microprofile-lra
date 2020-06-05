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

/**
 * <p>
 * LRA annotations support distributed communications amongst software
 * components and due to the unreliable nature of networks,
 * messages/requests can be lost, delayed or duplicated etc and the
 * implementation component responsible for invoking {@link Compensate}
 * and {@link Complete} annotated methods may loose track of the status of
 * a participant. In this case, ideally it would just resend the completion
 * or compensation notification but if the participant (the class that
 * contains the Compensate and Complete annotations) does not
 * support idempotency then it must be able to report its' status by
 * by annotating one of the methods with this <code>&#64;Status</code>.
 * The annotated method should report the status according to one of the
 * {@link ParticipantStatus} enum values.
 * </p>
 *
 * <p>
 * If the annotation is applied to a JAX-RS resource method then the request
 * method MUST be {@link javax.ws.rs.GET}. The id of the currently
 * running LRA can be obtained by inspecting the incoming JAX-RS headers. If
 * this LRA is nested then the parent LRA MUST be present in the header with the name
 * {@link org.eclipse.microprofile.lra.annotation.ws.rs.LRA#LRA_HTTP_PARENT_CONTEXT_HEADER}
 * and value is of type {@link java.net.URI}.
 * </p>
 *
 * <p>
 * If the annotated method is not a JAX-RS resource method the id of the currently
 * running LRA can be obtained by adhering to a predefined method signature as
 * defined in the LRA specification document. Similarly the method may determine
 * whether or not it runs with a nested LRA by providing a parameter to hold the parent id.
 * For example,
 * </p>
 *
 * <pre>
 *     <code>
 *          &#64;Status
 *          public void status(URI lraId, URI parentId) { ...}
 *     </code>
 * </pre>
 *
 * <p>
 * would be a valid status method declaration. If an invalid signature is detected 
 * the implementation of this specification MUST prohibit successful startup of the application
 * (e.g. with a runtime exception).
 * </p>
 *
 * <p>
 * If the participant has already responded successfully to an invocation
 * of the <code>&#64;Compensate</code> or <code>&#64;Complete</code> method then it may
 * report <code>410 Gone</code> HTTP status code or in case of
 * non-JAX-RS method returning {@link ParticipantStatus} to return <code>null</code>.
 * </p>
 *
 * <p>
 * Since the participant generally needs to know the id of the LRA in order
 * to report its status there is generally no benefit to combining this
 * annotation with the <code>&#64;LRA</code> annotation (though it is not prohibited).
 * </p>
 *
 * <p>
 * If the method is a JAX-RS resource method (or is a non JAX-RS method
 * annotated with <code>&#64;Status</code> with return type
 * <code>javax.ws.rs.core.Response</code>) then the following are the only
 * valid response codes:
 * </p>
 *
 * <table border="0" cellpadding="3" cellspacing="0"
 *   summary="Valid JAX-RS response codes for Status methods">
 * <caption><span>JAX-RS Response Codes For Status Methods</span><span>&nbsp;</span></caption>
 * <tr>
 *   <th scope="col">Code</th>
 *   <th scope="col">Response Body</th>
 *   <th scope="col">Meaning</th>
 * </tr>
 * <tr>
 *   <td scope="row">200</td>
 *   <td scope="row">{@link ParticipantStatus} enum value</td>
 *   <td scope="row">The current status of the participant</td>
 * </tr>
 * <tr>
 *   <td scope="row">202</td>
 *   <td scope="row">Empty</td>
 *   <td scope="row">The resource is attempting to determine the status and
 *   the caller should retry later</td>
 * </tr>
 * <tr>
 *   <td scope="row">410</td>
 *   <td scope="row">Empty</td>
 *   <td scope="row">The method does not know about the LRA</td>
 * </tr>
 * </table>
 *
 * <p>
 * The implementation will handle the return code 410 in the same way
 * as the return code 200. Specifically, when the implementation calls the Status method
 * after it has called the Complete or Compensated method and received a response which indicates
 * that the process is in progress (with a return code 202, for example). The response code 410
 * which is received when calling this Status annotated method, MUST be interpreted by the implementation
 * that the process is successfully completed and the participant already forget about the LRA.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Status {
}
