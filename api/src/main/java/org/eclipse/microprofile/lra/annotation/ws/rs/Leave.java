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

package org.eclipse.microprofile.lra.annotation.ws.rs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * If a resource method is annotated with `@Leave` and is invoked in the context of
 * an LRA and if the bean class has registered a participant with that LRA then
 * it will be removed from the LRA just before the bean method is entered.
 * The participant can forget about this LRA, in particular it will not
 * be asked to complete or compensate when the LRA is subsequently ended.
 * Even though the method runs without an LRA context, the implementation
 * MUST still make the context available via a JAX-RS header and any outgoing
 * JAX-RS invocations performed by the method will still carry the context that
 * the participant has just left. Therefore the business logic must be
 * careful about any JAX-RS invocations it makes in the body of the annotated
 * method which may result in other resources being enlisted with the LRA.
 * </p>
 *
 * <p>
 * If the resource method (or class) is also annotated with `@LRA` the method will
 * execute with the context dictated by the `@LRA` annotation. If this `@LRA` annotation
 * results in the creation of a new LRA then the participant will still be removed
 * from the incoming context and will be enlisted with the new context (and the method
 * will execute with this new context). Note that in this case the context exposed in
 * the `LRA_HTTP_HEADER` JAX-RS header will be set to the new LRA (and not the original
 * one), ie the orignal context will not be available to the business logic.
 * </p>
 *
 * <p>
 * Also note that it is not possible to join or leave an LRA that has already
 * been asked to cancel or close (since that would conflict with the
 * the participant state model as defined in the LRA specification).
 * </p>
 *
 * <p>
 * Leaving a particular LRA has no effect on any other LRA - ie the same
 * resource can be enlisted with many different LRAs and leaving one
 * particular LRA will not affect its participation in any of the other
 * LRA's it has joined.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Leave {
}
