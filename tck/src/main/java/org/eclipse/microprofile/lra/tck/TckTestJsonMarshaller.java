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
package org.eclipse.microprofile.lra.tck;

import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class TckTestJsonMarshaller implements MessageBodyWriter<TckMethodResult> {

    @Override
    public long getSize(TckMethodResult test, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
        return clazz == TckMethodResult.class;
    }

    @Override
    public void writeTo(TckMethodResult test, Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType,
                        MultivaluedMap<String, Object> valueMap, OutputStream stream) throws IOException, WebApplicationException {

        JsonObject jsonObject = Json.createObjectBuilder()
                .add("name", test.getTestName())
                .add("result", test.getResult()).build();

        DataOutputStream outputStream = new DataOutputStream(stream);
        outputStream.writeBytes(jsonObject.toString());
    }
}
