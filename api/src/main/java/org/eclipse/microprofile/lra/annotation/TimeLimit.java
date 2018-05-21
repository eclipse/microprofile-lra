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
import java.util.concurrent.TimeUnit;

/**
 * Used on ({@link LRA} and {@link Compensate} annotations to indicate the
 * maximum time that the LRA or participant should remain active for.
 *
 * When applied at the class level the timeout applies to any method that
 * starts an LRA or registers a participant.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TimeLimit {
    /**
     * @return the period for which the LRA or participant will remain valid.
     * A value of zero indicates that it is always remain valid.
     *
     * For compensations the corresponding compensation (a method annotated with
     * {@link Compensate} in the same class) will be invoked if the time limit is
     * reached.
     */
    long limit() default 0;

    TimeUnit unit() default TimeUnit.SECONDS;
}
