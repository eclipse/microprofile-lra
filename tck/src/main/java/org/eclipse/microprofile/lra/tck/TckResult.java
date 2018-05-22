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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

class TckResult {
    private List<TckMethodResult> tests;
    private List<TckMethodResult> results;
    private List<String> failures;

    TckResult() {
        tests = new ArrayList<>();
        failures = new ArrayList<>();
    }

    void add(String testName, Function<TckTests, String> testMethod, boolean verbose) {
        tests.add( new TckMethodResult(testName, testMethod, verbose));
    }

    void runTests(TckTests testSpec, String testname) {
        Optional<TckMethodResult> tckTest = tests.stream()
                .filter(name -> name.getTestName().equalsIgnoreCase(testname))
                .findFirst();

        if (tckTest.isPresent()) {
            tckTest.get().test(testSpec);
        } else {
            tests.forEach(t -> t.test(testSpec));
        }

        failures = tests.stream()
                .filter(t -> !t.isPassed() && t.isRan())
                .map(TckMethodResult::getTestName)
                .collect(Collectors.toList());

        results = tests.stream()
                .filter(TckMethodResult::isRan)
                .collect(Collectors.toList());
    }

    public int getNumberOfFailures() {
        return failures.size();
    }

    public List<String> getFailures() {
        return failures;
    }

    public List<TckMethodResult> getTestResults() {
        return results;
    }
}
