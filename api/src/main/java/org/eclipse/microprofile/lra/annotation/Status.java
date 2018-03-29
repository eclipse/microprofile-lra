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
 *
 * In order to support recovery compensators must be able to report their status
 * once the completion part of the protocol starts.
 *
 * Methods annotated with this annotation must be JAX-RS resources and respond
 * to GET requests (ie are annotated with javax.ws.rs.Path and javax.ws.rs.GET,
 * respectively). They must report their status using one of the enum names
 * listed in {@link CompensatorStatus} whenever an HTTP GET request is made on
 * the method.
 *
 * If the participant has not yet been asked to complete or compensate it should
 * return with a <code>412 Precondition Failed</code> HTTP status code. NB although
 * this circumstance could be detected via the framework it would necessitate a
 * network call to the LRA coordinator.
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface Status {
}
