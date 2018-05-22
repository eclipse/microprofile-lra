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
package org.eclipse.microprofile.lra.tck.participant.model;

import org.eclipse.microprofile.lra.annotation.CompensatorStatus;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

public class Activity implements Serializable {
    private String id;
    private String rcvUrl;
    private String statusUrl;
    private CompensatorStatus status;
    private String userData;
    private String endData;

    private final AtomicInteger acceptedCount = new AtomicInteger(0);

    public Activity(String txId) {
        this.setId(txId);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRcvUrl() {
        return rcvUrl;
    }

    public void setRcvUrl(String rcvUrl) {
        this.rcvUrl = rcvUrl;
    }

    public String getStatusUrl() {
        return statusUrl;
    }

    public void setStatusUrl(String statusUrl) {
        this.statusUrl = statusUrl;
    }

    public CompensatorStatus getStatus() {
        return status;
    }

    public void setStatus(CompensatorStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Activity{" +
                "id='" + getId() + '\'' +
                ", rcvUrl='" + getRcvUrl() + '\'' +
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

    public void setUserData(String userData) {
        this.userData = userData;
    }

    public String getEndData() {
        return endData;
    }

    public void setEndData(String endData) {
        this.endData = endData;
    }

    public AtomicInteger getAcceptedCount() {
        return acceptedCount;
    }

    public void setAcceptedCount(int acceptedCount) {
        this.acceptedCount.set(acceptedCount);
    }
}
