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
 * When a bean method executes in the context of an LRA any methods in the bean
 * class that are annotated with @Compensate will be used as a participant for
 * that LRA. If it is applied to multiple methods an arbitrary one is chosen.
 *
 * If the associated LRA is subsequently cancelled the method on which this
 * annotation is present will be invoked.
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
