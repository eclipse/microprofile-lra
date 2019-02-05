/*
 *******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.microprofile.lra.tck.spi;

import org.eclipse.microprofile.lra.annotation.LRAStatus;
import org.eclipse.microprofile.lra.client.LRAInfo;

import javax.ws.rs.NotFoundException;
import java.net.URL;
import java.util.List;

public interface ManagementSPI {
    /**
     * Lookup information about an LRA
     *
     * @param lraId the LRA whose status is being requested
     *
     * @return the status of the the LRA
     *
     * @throws NotFoundException if the LRA no longer exists (which may or may
     * not indicate that the LRA has already closed or cancelled depending upon
     * how long the implementation chooses to maintain such information).
     */
    LRAInfo getStatus(URL lraId) throws NotFoundException;

    /**
     * Lookup LRAs that are in a particular state.
     *
     * @param status the state that the returned LRAs should be in. A null value
     *               indicates that all LRAs should be returned regardless of their
     *               state.
     *
     * @return all LRAs that are in the given state. The order of the elements in
     * the list are implementation specific (for example an implementation may choose
     * to return them in the order in which they were created).
     */
    List<LRAInfo> getLRAs(LRAStatus status);
}
