/*
 * Copyright 2016-2022 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junitpioneer.jupiter;

import static org.junitpioneer.testkit.assertion.PioneerAssert.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junitpioneer.testkit.ExecutionResults;
import org.junitpioneer.testkit.PioneerTestKit;
import org.junitpioneer.testkit.assertion.reportentry.ReportEntryContentAssert;

@DisplayName("Stopwatch extension ")
public class StopwatchExtensionTests {

	@Test
	@DisplayName("should be executed with annotation on class level and report an entry for test method")
	void runClassLevelAnnotationTest() {
		String methodName = "stopwatchExtensionShouldBeExecutedWithAnnotationOnClassLevel";
		ExecutionResults results = PioneerTestKit.executeTestClass(ClassLevelAnnotationTestCases.class);

		assertThat(results).hasNumberOfReportEntries(1);

		assertStringStartWithUnitAndContainsName(results, methodName);
	}

	@Test
	@DisplayName("should be executed with annotation on class level and test method and report an entry for test method")
	void runClassAndMethodLevelAnnotationTest() {
		String methodName = "stopwatchExtensionShouldBeExecutedWithAnnotationOnClassAndMethodLevel";
		ExecutionResults results = PioneerTestKit.executeTestClass(ClassAndMethodLevelAnnotationTestCases.class);
		assertThat(results).hasNumberOfReportEntries(1);

		assertStringStartWithUnitAndContainsName(results, methodName);
	}

	@Test
	@DisplayName("should be executed with annotation on test method and report an entry for test method")
	void runMethodLevelAnnotationTest() {
		String methodName = "stopwatchExtensionShouldBeExecutedOnWithAnnotationOnMethodLevel";
		ExecutionResults results = PioneerTestKit.executeTestMethod(MethodLevelAnnotationTestCases.class, methodName);
		assertThat(results).hasNumberOfReportEntries(1);

		assertStringStartWithUnitAndContainsName(results, methodName);
	}

	@Test
	@DisplayName("should not be executed and therefore no entry should be published")
	void runAnnotationTest() {
		String methodName = "stopwatchExtensionShouldNotBeExecuted";
		ExecutionResults results = PioneerTestKit.executeTestMethod(NonAnnotationTestCases.class, methodName);

		assertThat(results).hasNumberOfReportEntries(0);
	}

	@Test
	@DisplayName("should not change the report entry key")
	void verifyReportEntryKey() {
		// the store key is mentioned in the documentation and changing it would break
		// `TestExecutionListener` implementations that use it to filter stopwatch report entries
		Assertions.assertThat(StopwatchExtension.STORE_KEY).startsWith("StopwatchExtension");
	}

	private void assertStringStartWithUnitAndContainsName(ExecutionResults results, String methodName) {
		ReportEntryContentAssert reportEntry = assertThat(results).hasNumberOfReportEntries(1);
		reportEntry.firstValue().matches(String.format("Execution of '%s\\(\\)' took \\[[0-9]*\\] ms.", methodName));
		reportEntry.firstKey().isEqualTo(StopwatchExtension.STORE_KEY);
	}

	/**
	 * Inner test class for testing the class level annotation.
	 */
	@Stopwatch
	static class ClassLevelAnnotationTestCases {

		@Test
		void stopwatchExtensionShouldBeExecutedWithAnnotationOnClassLevel() {
		}

	}

	/**
	 * Inner test class for testing the method level annotation.
	 */
	static class MethodLevelAnnotationTestCases {

		@Test
		@Stopwatch
		void stopwatchExtensionShouldBeExecutedOnWithAnnotationOnMethodLevel() {
		}

	}

	/**
	 * Inner test class for testing the class level annotation.
	 */
	@Stopwatch
	static class ClassAndMethodLevelAnnotationTestCases {

		@Test
		@Stopwatch
		void stopwatchExtensionShouldBeExecutedWithAnnotationOnClassAndMethodLevel() {
		}

	}

	/**
	 * Inner test class for testing a not annotated method / class annotation.
	 */
	static class NonAnnotationTestCases {

		@Test
		void stopwatchExtensionShouldNotBeExecuted() {
		}

	}

}
