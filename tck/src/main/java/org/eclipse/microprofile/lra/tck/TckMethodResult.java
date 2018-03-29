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

import java.util.Objects;
import java.util.function.Function;

class TckMethodResult {
    private String testName;
    private Function<TckTests, String> testMethod;

    private boolean passed;
    private boolean ran;
    private boolean verbose;
    private String result;
    private Throwable failureReason;

    TckMethodResult(String testName, Function<TckTests, String> testMethod, boolean verbose) {
        this.testName = testName;
        this.testMethod = testMethod;
        this.verbose = verbose;
    }

    public String getTestName() {
        return testName;
    }

    public boolean isPassed() {
        return passed;
    }

    public boolean isRan() {
        return ran;
    }

    public String getResult() {
        return result;
    }

    public Throwable getFailureReason() {
        return failureReason;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TckMethodResult tckMethodResult = (TckMethodResult) o;
        return Objects.equals(testName, tckMethodResult.testName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(testName);
    }

    void test(TckTests suite){
        System.out.printf("Starting test %s%n", testName);

        suite.before();

        try {
            ran = true;
            result = testMethod.apply(suite);
            passed = true;
            failureReason = null;
        } catch (Throwable t) {
            result = t.getMessage();
            passed = false;
            failureReason = verbose ? t : null;
        } finally {
            suite.after();
        }
    }
}
