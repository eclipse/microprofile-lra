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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When a bean method executes in the context of an LRA any methods in the bean class that are annotated with @Complete
 * will be used as a compensator for that LRA and when it is present, so too must the {@link Compensate} and
 * {@link Status} annotations. If it is applied to multiple methods an arbitrary one is chosen.
 *
 * If the associated LRA is subsequently closed the method annotated with @Complete will be invoked.
 *
 * The annotation can be combined with {@link TimeLimit} annotation to limit the time that the compensator
 * remains valid, after which the corresponding @Compensate method will be called.
 */
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Complete {
}
