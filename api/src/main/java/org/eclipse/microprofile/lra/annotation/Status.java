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
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * LRA annotations support distributed communications amongst software
 * components and due to the unreliable nature of networks,
 * messages/requests can be lost, delayed or duplicated etc and the
 * implementation component responsible for invoking {@link Compensate}
 * and {@link Complete} annotated methods may loose track of the status of
 * a participant. In this case, ideally it would just resend the completion
 * or compensation notification but if the participant (the class that
 * contains the Compensate and Complete annotations) does not
 * support idempotency then it must be able to report its' status by
 * by annotating one of the methods with this <em>@Status</em>.
 * The annotated method should report the status according to one of the
 * {@link ParticipantStatus} enum values.
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
 *          &#64;Status
 *          public void status(URI lraId, URI parentId) { ...}
 *     </code>
 * </pre>
 * would be a valid status method declaration. If an invalid signature is detected 
 * the {@link org.eclipse.microprofile.lra.participant.InvalidLRAParticipantDefinitionException} 
 * will be thrown during the application startup.
 *
 * If the participant has already responded successfully to an invocation
 * of the <em>@Compensate</em> or <em>@Complete</em> method then it may
 * report <em>404 Not Found</em> HTTP status code or in case of 
 * non-JAX-RS method returning {@link ParticipantStatus} to return <em>null</em>. 
 * This enables the participant to free up resources.
 *
 * Since the participant generally needs to know the id of the LRA in order
 * to report its status there is generally no benefit to combining this
 * annotation with the `@LRA` annotation (though it is not prohibited).
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Status {
}
