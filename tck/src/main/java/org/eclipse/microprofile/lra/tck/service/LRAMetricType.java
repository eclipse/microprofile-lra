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
package org.eclipse.microprofile.lra.tck.service;

public enum LRAMetricType {
    // LRA statistics
    Closed, // an LRA that closed
    FailedToClose, // an LRA that failed to close
    Cancelled, // an LRA that was cancelled
    FailedToCancel, // an LRA that failed to cancel

    // Participant statistics
    Compensated, // a participant that was asked to compensate
    Completed, // a participant that was asked to complete
    Status, // a participant that was asked for its status
    Forget, // a participant that was told to forget
    Nested, // a participant callback that was invoked in the context of a parent LRA

    // Other statistics
    AfterLRA // a listener that has received a notification that an LRA finished
}
