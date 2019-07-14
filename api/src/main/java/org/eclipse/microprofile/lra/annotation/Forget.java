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

import org.eclipse.microprofile.lra.annotation.ws.rs.LRA;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * If a participant is unable to complete or compensate immediately
 * (ie it has indicated that the request has been accepted and is
 * in progress) or because of a failure (ie will never be able to finish)
 * then it must remember the fact (by reporting it when asked for its'
 * {@link Status}) until explicitly told that it can clean
 * up using this <code>&#64;Forget</code> annotation.
 * </p>
 *
 * <p>
 * A similar remark applies if the participant was enlisted in a
 * nested LRA {@link LRA.Type#NESTED}. Actions performed in the context
 * of a nested LRA must remain compensatable until the participant
 * is explicitly told it can clean up using this <code>&#64;Forget</code>
 * annotation.
 * </p>
 *
 * <p>
 * If the annotated method is a JAX-RS resource method the id of the currently
 * running LRA can be obtained by inspecting the incoming JAX-RS headers. If
 * this LRA is nested then the parent LRA MUST be present in the header with the name
 * {@link LRA#LRA_HTTP_PARENT_CONTEXT_HEADER}
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
 *          &#64;Forget
 *          public void forget(URI lraId, URI parentId) { ...}
 *     </code>
 * </pre>
 *
 * <p>
 * would be a valid forget method declaration. If an invalid signature is detected 
 * the implementation of this specification MUST prohibit successful startup of the application
 * (e.g. with a runtime exception).
 * </p>
 *
 * <p>
 * Since the participant generally needs to know the id of the LRA in order
 * to clean up there is generally no benefit to combining this annotation
 * with the <code>&#64;LRA</code> annotation (though it is not prohibited).
 * </p>
 *
 * <p>
 * Related information is provided in the javadoc for the {@link Status}
 * </p>
 * <p>
 * If the method is a JAX-RS resource method (or is a non JAX-RS method
 * annotated with <code>&#64;Forget</code> with return type
 * <code>javax.ws.rs.core.Response</code>) then the following are the only
 * valid response codes:
 * </p>
 *
 *<table border="0" cellpadding="3" cellspacing="0"
 *   summary="Valid JAX-RS response codes for Forget methods">
 * <caption><span>JAX-RS Response Codes For Forget Methods</span><span>&nbsp;</span></caption>
 * <tr>
 *   <th scope="col">Code</th>
 *   <th scope="col">Response Body</th>
 *   <th scope="col">Meaning</th>
 * </tr>
 * <tr>
 *   <td scope="row">200</td>
 *   <td scope="row">Empty</td>
 *   <td scope="row">The participant may have removed all knowledge of the LRA</td>
 * </tr>
 * <tr>
 *   <td scope="row">404</td>
 *   <td scope="row">Empty</td>
 *   <td scope="row">The method does not know about the LRA</td>
 * </tr>
 * </table>
 *
 * <p>
 * If any other code is returned then the implementation SHOULD keep retrying.
 * If it stops retrying a warning message SHOULD be logged.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Forget {
}
