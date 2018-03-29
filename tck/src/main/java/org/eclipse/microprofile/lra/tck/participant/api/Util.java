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
package org.eclipse.microprofile.lra.tck.participant.api;

import org.eclipse.microprofile.lra.annotation.Complete;
import org.eclipse.microprofile.lra.annotation.Compensate;
import org.eclipse.microprofile.lra.annotation.Leave;
import org.eclipse.microprofile.lra.annotation.TimeLimit;
import org.eclipse.microprofile.lra.annotation.Status;
import org.eclipse.microprofile.lra.annotation.Forget;
import org.eclipse.microprofile.lra.client.GenericLRAException;

import javax.ws.rs.Path;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


public class Util {
    private static final String COMPLETE = "complete";
    private static final String COMPENSATE = "compensate";
    private static final String STATUS = "status";
    private static final String LEAVE = "leave";
    private static final String FORGET = "forget";

    private static final Logger LOGGER = Logger.getLogger(Util.class.getName());

    private Util() {
    }

    public static Map<String, String> getTerminationUris(Class<?> compensatorClass, URI baseUri) {
        Map<String, String> paths = new HashMap<>();
        final boolean[] asyncTermination = {false};
        Annotation resourcePathAnnotation = compensatorClass.getAnnotation(Path.class);
        String resourcePath = resourcePathAnnotation == null
                ? ""
                : ((Path) resourcePathAnnotation).value().replaceAll("^/+", "");

        final String uriPrefix = String.format("%s:%s%s",
                baseUri.getScheme(), baseUri.getSchemeSpecificPart(), resourcePath)
                .replaceAll("/$", "");

        Arrays.stream(compensatorClass.getMethods()).forEach(method -> {
            Annotation pathAnnotation = method.getAnnotation(Path.class);

            if (pathAnnotation != null) {

                if (checkMethod(paths, COMPENSATE, (Path) pathAnnotation,
                        method.getAnnotation(Compensate.class), uriPrefix) != 0) {
                    TimeLimit timeLimit = method.getAnnotation(TimeLimit.class);

//                    if (timeLimit != null)
//                        paths.put(TIMELIMIT_PARAM_NAME, Long.toString(timeLimit.unit().toMillis(timeLimit.limit())));

                    if (isAsyncCompletion(method))
                        asyncTermination[0] = true;
                }

                if (checkMethod(paths, COMPLETE, (Path) pathAnnotation,
                        method.getAnnotation(Complete.class), uriPrefix) != 0) {
                    if (isAsyncCompletion(method))
                        asyncTermination[0] = true;
                }
                checkMethod(paths, STATUS, (Path) pathAnnotation,
                        method.getAnnotation(Status.class), uriPrefix);
                checkMethod(paths, FORGET, (Path) pathAnnotation,
                        method.getAnnotation(Forget.class), uriPrefix);

                checkMethod(paths, LEAVE, (Path) pathAnnotation, method.getAnnotation(Leave.class), uriPrefix);
            }
        });

        if (asyncTermination[0] && !paths.containsKey(STATUS) && !paths.containsKey(FORGET)) {
            LOGGER.log(Level.WARNING, "LRA participant class "
                    + compensatorClass + " with asynchronous temination but no @Status or @Forget annotations");
            throw new GenericLRAException(null, Response.Status.BAD_REQUEST.getStatusCode(),
                    "LRA participant class with asynchronous temination but no @Status or @Forget annotations", null);
        }

        StringBuilder linkHeaderValue = new StringBuilder();

        if (paths.size() != 0) {
            paths.forEach((k, v) -> makeLink(linkHeaderValue, null, k, v));
            paths.put("Link", linkHeaderValue.toString());
        }

        return paths;
    }

    private static StringBuilder makeLink(StringBuilder b, String uriPrefix, String key, String value) {

        if (value == null)
            return b;

        String terminationUri = uriPrefix == null ? value : String.format("%s%s", uriPrefix, value);
        Link link =  Link.fromUri(terminationUri).title(key + " URI").rel(key).type(MediaType.TEXT_PLAIN).build();

        if (b.length() != 0)
            b.append(',');

        return b.append(link);
    }

    /**
     * Providing information if method is defined to be completed asynchronously.
     * This means that {@link Suspended} annotation is available amongst the method parameters
     * while the method is annotated with {@link Complete} or {@link Compensate}.
     *
     * @param method  method to be checked for async completion
     * @return  true if method is to complete asynchronously, false if synchronously
     */
    public static boolean isAsyncCompletion(Method method) {
        if (method.isAnnotationPresent(Complete.class) || method.isAnnotationPresent(Compensate.class)) {
            for (Annotation[] ann : method.getParameterAnnotations())
                for (Annotation an : ann)
                    if (Suspended.class.getName().equals(an.annotationType().getName())) {
                        LOGGER.log(Level.WARNING, "JAX-RS @Suspended annotation is untested");
                        return true;
                    }
        }

        return false;
    }

    private static int checkMethod(Map<String, String> paths,
                                   String rel,
                                   Path pathAnnotation,
                                   Annotation annotationClass,
                                   String uriPrefix) {
        /*
         * If the annotationClass is null the requested participant annotation is not present,
         */
        if (annotationClass == null) {
            return 0;
        }

        paths.put(rel, uriPrefix + pathAnnotation.value());

        return 1;
    }
}
