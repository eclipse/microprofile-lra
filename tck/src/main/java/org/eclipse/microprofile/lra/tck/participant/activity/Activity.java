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
package org.eclipse.microprofile.lra.tck.participant.activity;

import org.eclipse.microprofile.lra.annotation.ParticipantStatus;

import java.io.Serializable;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple DTO that provides information about
 * the work processed in the TCK suite controllers.
 */
public class Activity implements Serializable {
    private static final long serialVersionUID = 1L;

    private URI lraId;
    private URI recoveryUri;
    private String statusUrl;
    private ParticipantStatus status;
    private String userData;
    private String endData;

    private final AtomicInteger acceptedCount = new AtomicInteger(0);

    public Activity(URI lraId) {
        this.setLraId(lraId);
    }

    public URI getLraId() {
        return lraId;
    }

    public Activity setLraId(URI lraId) {
        this.lraId = lraId;
        return this;
    }

    public URI getRecoveryUri() {
        return recoveryUri;
    }

    public Activity setRecoveryUri(URI recoveryUri) {
        this.recoveryUri = recoveryUri;
        return this;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public Activity setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
        return this;
    }

    public ParticipantStatus getStatus() {
        return status;
    }

    public Activity setStatus(ParticipantStatus status) {
        this.status = status;
        return this;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "lraId='" + getLraId() + '\'' +
                ", recoveryUri='" + getRecoveryUri() + '\'' +
                ", statusUrl='" + getStatusUrl() + '\'' +
                ", status=" + getStatus() +
                ", userData='" + getUserData() + '\'' +
                ", endData='" + getEndData() + '\'' +
                '}';
    }

    public int getAndDecrementAcceptCount() {
        return getAcceptedCount().getAndDecrement();
    }

    public String getUserData() {
        return userData;
    }

    public Activity setUserData(String userData) {
        this.userData = userData;
        return this;
    }

    public String getEndData() {
        return endData;
    }

    public Activity setEndData(String endData) {
        this.endData = endData;
        return this;
    }

    public AtomicInteger getAcceptedCount() {
        return acceptedCount;
    }

    public Activity setAcceptedCount(int acceptedCount) {
        this.acceptedCount.set(acceptedCount);
        return this;
    }
}
