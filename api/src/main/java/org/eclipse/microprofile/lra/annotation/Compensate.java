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
 * be used as a participant for the LRA. If this associated LRA is subsequently
 * cancelled then the method that this annotation is applied to will be invoked
 * (if the annotation is present on more than one method then an arbitrary one
 * will be chosen).
 *
 * The id of the currently running LRA can be obtained by inspecting the incoming
 * JAX-RS headers. If this LRA is nested then the parent LRA MUST be present
 * in the header with the name
 * {@link org.eclipse.microprofile.lra.annotation.ws.rs.LRA#LRA_HTTP_PARENT_CONTEXT_HEADER}.
 *
 * Note that, according to the state model {@link LRAStatus} once an LRA has been
 * asked to cancel it is no longer possible to join with it as a participant.
 * Therefore combining this annotation with an `@LRA` annotation that does not
 * start a new LRA will result in a `412 PreCondition Failed` status code and is
 * not advised. On the other hand, combining it with an `@LRA` annotation that
 * begins a new LRA can in certain use case make sense, but in this case the LRA
 * that this method is being asked to compensate for will be unavailable.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Compensate {
    /**
     * The period for which the participant will guarantee it will be able
     * to compensate for any work that it performed during the associated LRA.
     * When this period elapses the LRA that it joined becomes eligible for
     * cancellation. The units are specified in the {@link Compensate#timeUnit()}
     * attribute.
     *
     * A value of zero indicates that it will always be able to compensate.
     *
     * @return the period for which the participant can guarantee it
     * will be able to compensate when asked to do so
     */
    long timeLimit() default 0;

    /**
     * @return the unit of time that the {@link Compensate#timeLimit()} attribute is
     * measured in.
     */
    ChronoUnit timeUnit() default ChronoUnit.SECONDS;
}
