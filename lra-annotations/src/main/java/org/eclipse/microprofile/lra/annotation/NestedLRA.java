/*
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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
 */
package org.eclipse.microprofile.lra.annotation;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used on the interface or class. Defines that the container will create
 * a new LRA for each method invocation, regardless of whether or not there is
 * already an LRA associated with the caller. These LRAs will then
 * either be top-level LRAs or nested automatically depending upon the
 * context within which they are created.
 *
 * When a nested LRA is closed its' compensators are completed but retained. At any time prior to the enclosing LRA
 * being closed or cancelled the nested LRA can be told to compensate (even though it may have already been told
 * to complete).
 *
 * Compatability with the @LRA annotation: if @LRA is not present @Nested is ignored, otherwise the behaviour depends
 * upon the value of the Type attribute:
 *
 * REQUIRED
 *  if there is an LRA present a new LRA is nested under it
 *
 * REQUIRES_NEW,
 *  the @Nested annotation is ignored
 *
 * MANDATORY,
 *  a new LRA is nested under the incoming LRA
 *
 * SUPPORTS,
 *  if there is an LRA present a new LRA is nested under otherwise a new top level LRA is begun
 *
 * NOT_SUPPORTED,
 *  nested does not make sense and operations on this resource that contain a LRA context will immediately return
 *  with a 412 Precondition Failed HTTP status code
 *
 * NEVER
 *  nested does not make sense and requests that carry a LRA context will immediately return
 *  with a 412 Precondition Failed HTTP status code
 */
@Inherited
@InterceptorBinding
@Retention(value = RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NestedLRA {
}
