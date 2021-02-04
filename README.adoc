//
// Copyright (c) 2019 Contributors to the Eclipse Foundation
//
// See the NOTICES file(s) distributed with this work for additional
// information regarding copyright ownership.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
image:https://badges.gitter.im/eclipse/microprofile-lra.svg[link="https://gitter.im/eclipse/microprofile-lra?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge"]

# Long Running Actions for MicroProfile

== Introduction

The specification introduces APIs for services to coordinate activities.

The main thrust of the proposal introduces an API for loosely coupled
services to coordinate long running activities in such a way as to
guarantee a globally consistent outcome without the need to take locks
on data.

== Motivation

In a loosely coupled service based environment there is sometimes a need
for different services to provide consistency guarantees. Typical
examples include:

* order processing involving three services (take order, bill customer,
ship product). If the shipping service finds that it is out of stock
then the customer will have been billed with no prospect of receiving
his item;
* an airline overbooks a flight which means that the booking count and
the flight capacity are inconsistent.

There are various ways that systems overcome such inconsistency but it
would be advantageous to provide a generic solution which handles
failure conditions, maintains state for those flows that span long
periods of time and ensures that remedial activities are called
correctly.

Traditional techniques for guaranteeing consistency in distributed
environments has focused on XA transactions where locks may be held for
long periods thereby introducing strong coupling between services and
decreasing concurrency to unacceptable levels. Additionally, if such a
transaction aborts then valuable work which may be valid will be rolled
back. In view of these issues an alternative approach is desirable.

Goals

* support long running actions
* no strong coupling between services
* allow actions to finish early
* allow compensating actions if a business activity is cancelled

== Contributing

Do you want to contribute to this project? link:CONTRIBUTING.adoc[Find out how you can help here].
