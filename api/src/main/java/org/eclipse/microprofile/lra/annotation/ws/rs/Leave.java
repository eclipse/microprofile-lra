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
 * A resource may be enlisted with an LRA if one of its resource methods
 * annotated with @LRA is invoked. If the resource also contains a method
 * annotated with @Leave, then invoking such a method in the context of the
 * same LRA that it has joined will result in the resource leaving the LRA
 * (ie it will not receive any callbacks when the LRA is later closed or
 * cancelled). However, leaving an LRA does not prohibit the resource from
 * rejoining the LRA if one of the @LRA methods is invoked again in the
 * context of the same LRA.
 *
 * Note that it is not possible to join or leave an LRA that has already
 * been asked to cancel or close.
 *
 * Leaving a particular LRA has no effect on any other LRA - ie the same
 * resource can be enlisted with many different LRA's and leaving one
 * particular LRA will not affect its participation in any of the other
 * LRA's it has joined.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Leave {
}
