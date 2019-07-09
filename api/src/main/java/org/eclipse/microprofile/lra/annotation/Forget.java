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
 * If a participant is unable to complete or compensate immediately
 * (ie it has indicated that the request has been accepted and is
 * in progress) or because of a failure (ie will never be able to finish)
 * then it must remember the fact (by reporting it when asked for its'
 * {@link Status})) until explicitly told that it can clean
 * up using this <em>@Forget</em> annotation.
 *
 * A similar remark applies if the participant was enlisted in a
 * nested LRA {@link LRA#value()}. Actions performed in the context
 * of a nested LRA must remain compensatable until the participant
 * is explicitly told it can clean up using this <em>@Forget</em>
 * annotation.
 *
 * If the annotated method is a JAX-RS resource method the id of the currently
 * running LRA can be obtained by inspecting the incoming JAX-RS headers. If
 * this LRA is nested then the parent LRA MUST be present in the header with the name
 * {@link org.eclipse.microprofile.lra.annotation.ws.rs.LRA#LRA_HTTP_PARENT_CONTEXT_HEADER}
 * and value is of type {@link java.net.URI}.
 *
 * If the annotated method is not a JAX-RS resource method the id of the currently
 * running LRA can be obtained by adhering to a predefined method signature as
 * defined in the LRA specification document. Similarly the method may determine
 * whether or not it runs with a nested LRA by providing a parameter to hold the parent id.
 * For example,
 * <pre>
 *     <code>
 *          &#64;Forget
 *          public void forget(URI lraId, URI parentId) { ...}
 *     </code>
 * </pre>
 * would be a valid forget method declaration. If an invalid signature is detected 
 * the {@link java.lang.RuntimeException} will be thrown during the application startup.
 *
 *
 * Since the participant generally needs to know the id of the LRA in order
 * to clean up there is generally no benefit to combining this annotation
 * with the `@LRA` annotation (though it is not prohibited).
 *
 * Related information is provided in the javadoc for the {@link Status}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Forget {
}
