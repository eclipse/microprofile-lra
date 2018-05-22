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

package org.eclipse.microprofile.lra.client;

import org.eclipse.microprofile.lra.annotation.CompensatorStatus;

import javax.ws.rs.NotFoundException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public interface LRAClient {
    /**
     * the name of the HTTP header field that contains the LRA id associated with
     * a request/response
     */
    String LRA_HTTP_HEADER = "Long-Running-Action";

    /**
     * the name of the HTTP header field that contains a recovery URL corresponding
     * to a participant
     * enlistment in an LRA {@link LRAClient#updateCompensator}
     */
    String LRA_HTTP_RECOVERY_HEADER = "Long-Running-Action-Recovery";

    /**
     * Key for looking up the config property that specifies which host a
     * coordinator is running on
     */
    String LRA_COORDINATOR_HOST_KEY = "lra.http.host";

    /**
     * Key for looking up the config property that specifies which port a
     * coordinator is running on
     */
    String LRA_COORDINATOR_PORT_KEY = "lra.http.port";

    /**
     * Key for looking up the config property that specifies which JAX-RS path a
     * coordinator is running on
     */
    String LRA_COORDINATOR_PATH_KEY = "lra.coordinator.path";

    /**
     * Key for looking up the config property that specifies which JAX-RS path a
     * recovery coordinator is running on
     */
    String LRA_RECOVERY_HOST_KEY = "lra.http.recovery.host";

    /**
     * Key for looking up the config property that specifies which JAX-RS path a
     * recovery coordinator is running on
     */
    String LRA_RECOVERY_PORT_KEY = "lra.http.recovery.port";

    /**
     * Key for looking up the config property that specifies which JAX-RS path a
     * recovery coordinator is running on
     */
    String LRA_RECOVERY_PATH_KEY = "lra.coordinator.recovery.path";

    /**
     * Set the endpoint on which the coordinator is available
     *
     * @param uri the url of the LRA coordinator
     */
    void setCoordinatorURI(URI uri);

    /**
     * Set the endpoint on which the recovery coordinator is available
     *
     * @param uri the url of the LRA recovery coordinator
     */
    void setRecoveryCoordinatorURI(URI uri);

    /**
     * Explicitly dispose of all resources. After this call the instance may no
     * longer be useable
     */
    void close();

    /**
     * Start a new LRA
     *
     * If the LRA was started during a JAX-RS resource method invocation then the
     * id of the new LRA will be made available as the JAX-RS response header
     * {@link LRAClient#LRA_HTTP_HEADER}
     *
     * @param parentLRA The parent of the LRA that is about to start. If null then
     *                 the new LRA will be top level
     * @param clientID The client may provide a (preferably) unique identity which
     *                will be reported back when the LRA is queried.
     * @param timeout Specifies the maximum time that the LRA will exist for. If the
     *               LRA is terminated because of a timeout it will be cancelled.
     * @param unit Specifies the unit that the timeout is measured in
     *
     * @throws NotFoundException if the parent LRA is known to no longer exist
     *
     * @throws GenericLRAException a new LRA could not be started. The specific
     * reason is available in {@link GenericLRAException#getStatusCode()}
     *
     * @return the identifier of the new LRA
     */
    URL startLRA(URL parentLRA, String clientID, Long timeout, TimeUnit unit)
            throws GenericLRAException;

    /**
     * Start a top level LRA (ie similar to
     * {@link LRAClient#startLRA(URL, String, Long, TimeUnit)}
     *
     * @param clientID The client may provide a (preferably) unique identity which
     *                will be reported back when the LRA is queried.
     * @param timeout Specifies the maximum time that the LRA will exist for. If the
     *               LRA is terminated because of a timeout it will be cancelled.
     * @param unit Specifies the unit that the timeout is measured in
     *
     * @throws NotFoundException if the parent LRA is known to no longer exist
     *
     * @throws GenericLRAException a new LRA could not be started. The specific
     * reason is available in {@link GenericLRAException#getStatusCode()}
     *
     * @return the identifier of the new LRA
     */
    URL startLRA(String clientID, Long timeout, TimeUnit unit)
            throws GenericLRAException;

    /**
     * Attempt to cancel an LRA
     *
     * Trigger compensation of all participants enlisted with the LRA (ie the
     * compensate message will be sent to each participant).
     *
     * @param lraId The unique identifier of the LRA (required)
     *
     * {@link CompensatorStatus#name()}. If the final status is not returned the
     *             client can still discover the final state using the
     *             {@link LRAClient#getStatus(URL)} method
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException Communication error (the reason is availalbe via
     * the {@link GenericLRAException#getStatusCode()} method
     *
     * @return the response MAY contain the final status of the LRA as reported by
     */
    String cancelLRA(URL lraId) throws GenericLRAException;

    /**
     * Attempt to close an LRA
     *
     * Tells the LRA to close normally. All participants will be triggered by the
     * coordinator (ie the complete message will be sent to each participant).
     *
     * @param lraId The unique identifier of the LRA (required)
     *
     * {@link CompensatorStatus#name()}. If the final status is not returned the
     *             client can still discover the final state using the
     *             {@link LRAClient#getStatus(URL)} method
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException Communication error (the reason is availalbe via
     * the {@link GenericLRAException#getStatusCode()} method
     *
     * @return the response MAY contain the final status of the LRA as reported by
     */
    String closeLRA(URL lraId) throws GenericLRAException;

    /**
     * Lookup active LRAs
     *
     * @return a list of active LRAs
     *
     * @throws GenericLRAException on error
     */
    List<LRAInfo> getActiveLRAs() throws GenericLRAException;

    /**
     * Returns all (both active and recovering) LRAs
     *
     * @return a list of all LRAs known to this coordinator
     *
     * @throws GenericLRAException on error
     */
    List<LRAInfo> getAllLRAs() throws GenericLRAException;

    /**
     * List recovering Long Running Actions
     *
     * @return LRAs that are recovering (ie the participant is still
     * attempting to complete or compensate
     *
     *
     * @throws GenericLRAException on error
     */
    List<LRAInfo> getRecoveringLRAs() throws GenericLRAException;

    /**
     * Lookup the status of an LRA
     *
     * @param lraId the LRA whose status is being requested
     *
     * @return the status or empty if the the LRA is still active (ie has not yet
     * been closed or cancelled)
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()}
     * may provide a more specific reason.
     */
    Optional<CompensatorStatus> getStatus(URL lraId) throws GenericLRAException;

    /**
     * Indicates whether an LRA is active. The same information can be obtained via
     * a call to {@link LRAClient#getStatus(URL)}.
     *
     * @param lraId The unique identifier of the LRA (required)
     *
     * @return whether or not the specified LRA is active
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()}
     * may provide a more specific reason.
     */
    Boolean isActiveLRA(URL lraId) throws GenericLRAException;

    /**
     * Indicates whether an LRA was compensated. The same information can be
     * obtained via a call to {@link LRAClient#getStatus(URL)}.
     *
     * @param lraId The unique identifier of the LRA (required)
     *
     * @return whether or not the specified LRA has been compensated
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()}
     * may provide a more specific reason.
     */
    Boolean isCompensatedLRA(URL lraId) throws GenericLRAException;

    /**
     * Indicates whether an LRA is complete. The same information can be obtained
     * via a call to {@link LRAClient#getStatus(URL)}.
     *
     * @param lraId The unique identifier of the LRA (required)
     *
     * @return whether or not the specified LRA has been completed
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()}
     * may provide a more specific reason.     */
    Boolean isCompletedLRA(URL lraId) throws GenericLRAException;


    /**
     * A participant can join with the LRA at any time prior to the completion of
     * an activity. The participant provides end points on which it will listen
     * for LRA related events.
     *
     * @param lraId   The unique identifier of the LRA (required) to enlist with
     * @param timelimit The time limit (in seconds) that the participant can
     *                 guarantee that it can compensate the work performed while
     *                 the LRA is active.
     * @param compensateUrl the `compensatation URL`
     * @param completeUrl the `completion URL`
     * @param forgetUrl the `forget URL`
     * @param leaveUrl the `leave URL`
     * @param statusUrl the `status URL`
     *
     * @param compensatorData data that will be stored with the coordinator and
     *                       passed back to the participant when the LRA is closed
     *                       or cancelled
     * @return a recovery URL for this enlistment
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException  if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()}
     * may provide a more specific reason.
     */
    String joinLRA(URL lraId, Long timelimit,
                   URL compensateUrl, URL completeUrl, URL forgetUrl,
                   URL leaveUrl, URL statusUrl,
                   String compensatorData) throws GenericLRAException;

    /**
     * Join an LRA passing in a class that will act as the participant.
     * Similar to {@link LRAClient#joinLRA(URL, Class, URI, String)} except
     * that the various participant URLs are expressed as CDI annotations on
     * the passed in resource class.
     *
     * @param lraId The unique identifier of the LRA (required)
     * @param resourceClass An annotated class for the participant methods:
     * {@link org.eclipse.microprofile.lra.annotation.Compensate}, etc.
     * @param baseUri Base uri for the participant endpoints
     * @param compensatorData Compensator specific data that the coordinator will
     *                       pass to the participant when the LRA is closed or
     *                       cancelled
     *
     * @return a recovery URL for this enlistment
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()} may provide a more specific reason.
     */
    String joinLRA(URL lraId, Class<?> resourceClass, URI baseUri, String compensatorData)
            throws GenericLRAException;

    /**
     * Change the endpoints that a participant can be contacted on.
     *
     * @param recoveryUrl the recovery URL returned from a participant join request
     * @param compensateUrl the URL to invoke when the LRA is cancelled
     * @param completeUrl the URL to invoke when the LRA is closed
     * @param statusUrl if a participant cannot finish immediately then it provides
     *                  this URL that the coordinator uses to monitor the progress
     * @param forgetUrl used to inform the participant that can forget about this LRA
     * @param compensatorData opaque data that returned to the participant when the
     *                       LRA is closed or cancelled
     * @return an updated recovery URL for this participant
     * @throws GenericLRAException if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()} may provide a more specific reason.
     */
    URL updateCompensator(URL recoveryUrl, URL compensateUrl, URL completeUrl,
                          URL forgetUrl, URL statusUrl,
                          String compensatorData) throws GenericLRAException;

    /**
     * A Compensator can resign from the LRA at any time prior to the completion
     * of an activity
     *
     * @param lraId The unique identifier of the LRA (required)
     * @param body  (optional)
     *
     *              @throws NotFoundException if the LRA no longer exists
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @throws GenericLRAException if the request to the coordinator failed.
     * {@link GenericLRAException#getCause()} and/or
     * {@link GenericLRAException#getStatusCode()} may provide a more specific reason.
     */
    void leaveLRA(URL lraId, String body) throws GenericLRAException;

    /**
     * LRAs can be created with timeouts after which they are cancelled. Use this
     * method to update the timeout.
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @param lraId the id of the lra to update
     * @param limit the new timeout period
     * @param unit the time unit for limit
     */
    void renewTimeLimit(URL lraId, long limit, TimeUnit unit);

    /**
     * checks whether there is an LRA associated with the calling thread
     * (this method provides an alternative to relying on the presence
     * of the {@link LRAClient#LRA_HTTP_HEADER} HTTP header fields since
     * not all users of the API are being called in the context of JAX-RS
     * resource requests).
     *
     * @return the current LRA (can be null)
     */
    URL getCurrent();

    /**
     * Update the clients notion of the current coordinator.
     *
     * @throws NotFoundException if the LRA no longer exists
     *
     * @param lraId the id of the LRA (can be null)
     */
    void setCurrentLRA(URL lraId);
}
