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
 * If a participant is unable to complete or compensate immediately
 * (ie it has indicated that the request has been accepted and is
 * in progress) or because of a failure (ie will never be able to finish)
 * then it must remember the fact (by reporting it when asked for its'
 * {@link Status})) until explicitly told that it can clean
 * up using this <em>@Forget</em> annotation. The annotated method
 * must be a standard JAX-RS endpoint annotated with the JAX-RS
 * <em>@DELETE</em> annotation.
 *
 * The id of the currently running LRA can be obtained by inspecting the
 * incoming JAX-RS headers.
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
