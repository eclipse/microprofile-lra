/*
 * Copyright (c) 2007, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the Eclipse Foundation, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.eclipse.microprofile.lra.annotation;

import javax.interceptor.InterceptorBinding;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>
 * Used on the interface or class. Defines that the container will create
 * a new LRA for each method invocation, regardless of whether or not there is
 * already an LRA associated with the caller. These LRAs will then
 * either be top-level LRAs or nested automatically depending upon the
 * context within which they are created.
 * <p>
 * When a nested LRA is closed its' compensators are completed but retained. At any time prior to the enclosing LRA
 * being closed or cancelled the nested LRA can be told to compensate (even though it may have already been told
 * to complete).
 * <p>
 * Compatability with the @LRA annotation: if {@link LRA} is not present @NestedLRA is ignored, otherwise the behaviour depends
 * upon the value of the @LRA type attribute:
 * <p>
 * <ul>
 *   <li>REQUIRED
 *     if there is an LRA present a new LRA is nested under it
 *   <li>REQUIRES_NEW,
 *     the @NestedLRA annotation is ignored
 *  <li>MANDATORY,
 *     a new LRA is nested under the incoming LRA
 *  <li>SUPPORTS,
 *    if there is an LRA present a new LRA is nested under otherwise a new top level LRA is begun
 *  <li>NOT_SUPPORTED,
 *    nested does not make sense and operations on this resource that contain a LRA context will immediately return
 *    with a <code>412 Precondition Failed</code> HTTP status code
 *  <li>NEVER,
 *    nested does not make sense and requests that carry a LRA context will immediately return
 *    with a <code>412 Precondition Failed</code> HTTP status code
 * </ul>
 */
@Inherited
@InterceptorBinding
@Retention(value = RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NestedLRA {
}
