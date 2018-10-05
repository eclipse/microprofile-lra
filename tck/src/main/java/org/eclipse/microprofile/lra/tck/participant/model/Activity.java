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
import java.util.logging.Level;
import java.util.logging.Logger;

public class Activity implements Serializable {
    private static final long serialVersionUID = -4141599248046299770L;
    private static final Logger LOGGER = Logger.getLogger(Activity.class.getName());

    public String id;
    private String rcvUrl;
    private String statusUrl;
    private CompensatorStatus status;
    private boolean registered;
    private String registrationStatus;
    private String userData;
    private String endData;
    private String how;
    private String arg;
    private boolean waiting;

    private final AtomicInteger acceptedCount = new AtomicInteger(0);

    public Activity(String txId) {
        this.id = txId;
    }

    public void setEndData(String endData) {
        this.endData = endData;
    }

    public String getEndData() {
        return endData;
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
                "id='" + id + '\'' +
                ", rcvUrl='" + getRcvUrl() + '\'' +
                ", statusUrl='" + getStatusUrl() + '\'' +
                ", status=" + getStatus() +
                ", endData='" + getEndData() + '\'' +
                '}';
    }

    public int getAcceptedCount() {
        return acceptedCount.get();
    }

    public void setAcceptedCount(int acceptedCount) {
        this.acceptedCount.set(acceptedCount);
    }


    public int getAndDecrementAcceptCount() {
        return acceptedCount.getAndDecrement();
    }

    public String getHow() {
        return how;
    }

    public void setHow(String how) {
        this.how = how;
    }

    public String getArg() {
        return arg;
    }

    public void setArg(String arg) {
        this.arg = arg;
    }

    public String getId() {
        return id;
    }

    public synchronized void waitFor(long ms) {
        waiting = true;

        try {
            wait(ms <= 0 ? Long.MAX_VALUE : ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            LOGGER.log(Level.INFO, "activity wait threw " + e.getMessage());
        }

        waiting = false;
    }

    public synchronized void cleanup() {
        if (waiting) {
            notify();
            waiting = false;
        }
    }
}
