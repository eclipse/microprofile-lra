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
 * When a resource method executes in the context of an LRA and if the containing
 * class contains a method annotated with `@Compensate` then the resource will
 * be used as a participant for the LRA. If, in addition, it also contains a method
 * annotated with `@Complete` then the method will be invoked when the associated
 * LRA is later closed. The spec makes no guarantees about when it will be invoked,
 * just that is will eventually be called.
 *
 * If the annotation is present on more than one method then an arbitrary one
 * will be chosen.
 *
 * If the annotated method is a JAX-RS resource method the id of the currently
 * running LRA can be obtained by inspecting the incoming JAX-RS headers. If
 * this LRA is nested then the parent LRA MUST be present in the header with the name
 * {@link org.eclipse.microprofile.lra.annotation.ws.rs.LRA#LRA_HTTP_PARENT_CONTEXT_HEADER}.
 *
 * If the annotated method is not a JAX-RS resource method the id of the currently
 * running LRA can be obtained by adhering to a predefined method signature as
 * defined in the LRA specification document. Similarly the method may determine
 * whether or not it runs with a nested LRA by providing a parameter to hold the parent id.
 * For example,
 * <pre>
 *     <code>
 *          &#64;Complete
 *          public void complete(URI lraId, URI parentId) { ...}
 *     </code>
 * </pre>
 * would be a valid completion method declaration. If an invalid signature is detected 
 * the {@link org.eclipse.microprofile.lra.participant.InvalidLRAParticipantDefinitionException} 
 * will be thrown during the application startup.
 *
 * Note that, according to the state model {@link LRAStatus} once an LRA has been
 * asked to close it is no longer possible to join with it as a participant.
 * Therefore in JAX-RS, combining this annotation with an `@LRA` annotation that does not
 * start a new LRA will result in a `412 PreCondition Failed` status code and is
 * not advised. On the other hand, combining it with an `@LRA` annotation that
 * begins a new LRA can in certain use case make sense, but in this case the LRA
 * that this method is being asked to complete for will be unavailable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Complete {
    /**
     * The period for which the participant will guarantee it will be able
     * to complete for any work that it performed during the associated LRA.
     * When this period elapses the LRA that it joined becomes eligible for
     * cancellation. The units are specified in the {@link Complete#timeUnit()}
     * attribute.
     *
     * A value of zero indicates that it will always be able to complete.
     *
     * @return the period for which the participant can guarantee it
     * will be able to complete when asked to do so
     */
    long timeLimit() default 0;

    /**
     * @return the unit of time that the {@link Complete#timeLimit()} attribute is
     * measured in.
     */
    ChronoUnit timeUnit() default ChronoUnit.SECONDS;
}
