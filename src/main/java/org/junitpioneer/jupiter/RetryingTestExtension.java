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

import static java.lang.String.format;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;
import org.junit.platform.commons.support.AnnotationSupport;
import org.junitpioneer.internal.PioneerAnnotationUtils;
import org.junitpioneer.internal.TestNameFormatter;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

class RetryingTestExtension implements TestTemplateInvocationContextProvider, TestExecutionExceptionHandler {

	private static final Namespace NAMESPACE = Namespace.create(RetryingTestExtension.class);

	@Override
	public boolean supportsTestTemplate(ExtensionContext context) {
		// the annotation only applies to methods (see its `@Target`),
		// so it doesn't matter that this method checks meta-annotations
		return PioneerAnnotationUtils.isAnnotationPresent(context, RetryingTest.class);
	}

	@Override
	public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(ExtensionContext context) {
		FailedTestRetrier retrier = retrierFor(context);
		return stream(spliteratorUnknownSize(retrier, ORDERED), false);
	}

	@Override
	public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
		// this `context` (M) is a child of the context passed to `provideTestTemplateInvocationContexts` (T),
		// which means M's store content is invisible to T's store; this can be fixed by using T's store here
		ExtensionContext templateContext = context
				.getParent()
				.orElseThrow(() -> new IllegalStateException(
					"Extension context \"" + context + "\" should have a parent context."));
		retrierFor(templateContext).failed(throwable);
	}

	private static FailedTestRetrier retrierFor(ExtensionContext context) {
		Method test = context.getRequiredTestMethod();
		return context
				.getStore(NAMESPACE)
				.getOrComputeIfAbsent(test.toString(), __ -> FailedTestRetrier.createFor(test, context),
					FailedTestRetrier.class);
	}

	private static class FailedTestRetrier implements Iterator<RetryingTestInvocationContext> {

		private final int maxRetries;
		private final int minSuccess;
		private final int suspendForMs;
		private final Class<? extends Throwable>[] expectedExceptions;
		private final TestNameFormatter formatter;

		private int retriesSoFar;
		private int exceptionsSoFar;
		private boolean seenFailedAssumption;
		private boolean seenUnexpectedException;

		private FailedTestRetrier(int maxRetries, int minSuccess, int suspendForMs,
				Class<? extends Throwable>[] expectedExceptions, TestNameFormatter formatter) {
			this.maxRetries = maxRetries;
			this.minSuccess = minSuccess;
			this.suspendForMs = suspendForMs;
			this.expectedExceptions = expectedExceptions;
			this.retriesSoFar = 0;
			this.exceptionsSoFar = 0;
			this.formatter = formatter;
		}

		static FailedTestRetrier createFor(Method test, ExtensionContext context) {
			RetryingTest retryingTest = AnnotationSupport
					.findAnnotation(test, RetryingTest.class)
					.orElseThrow(() -> new IllegalStateException("@RetryingTest is missing."));

			int maxAttempts = retryingTest.maxAttempts() != 0 ? retryingTest.maxAttempts() : retryingTest.value();
			int minSuccess = retryingTest.minSuccess();
			String pattern = retryingTest.name();

			if (maxAttempts == 0)
				throw new ExtensionConfigurationException(
					"@RetryingTest requires that one of `value` or `maxAttempts` be set.");
			if (retryingTest.value() != 0 && retryingTest.maxAttempts() != 0)
				throw new ExtensionConfigurationException(
					"@RetryingTest requires that one of `value` or `maxAttempts` be set, but not both.");

			if (minSuccess < 1)
				throw new ExtensionConfigurationException(
					"@RetryingTest requires that `minSuccess` be greater than or equal to 1.");
			else if (maxAttempts <= minSuccess) {
				String additionalMessage = maxAttempts == minSuccess
						? " Using @RepeatedTest is recommended as a replacement."
						: "";
				throw new ExtensionConfigurationException(
					format("@RetryingTest requires that `maxAttempts` be greater than %s.%s",
						minSuccess == 1 ? "1" : "`minSuccess`", additionalMessage));
			}
			if (pattern.isEmpty())
				throw new ExtensionConfigurationException("RetryingTest can not have an empty display name.");
			String displayName = context.getDisplayName();
			TestNameFormatter formatter = new TestNameFormatter(pattern, displayName, RetryingTest.class);

			if (retryingTest.suspendForMs() < 0) {
				throw new ExtensionConfigurationException(
					"@RetryingTest requires that `suspendForMs` be greater than or equal to 0.");
			}

			return new FailedTestRetrier(maxAttempts, minSuccess, retryingTest.suspendForMs(),
				retryingTest.onExceptions(), formatter);
		}

		<E extends Throwable> void failed(E exception) throws E {
			exceptionsSoFar++;

			if (exception instanceof TestAbortedException) {
				seenFailedAssumption = true;
				throw new TestAbortedException("Test execution was skipped, possibly because of a failed assumption.",
					exception);
			}

			if (!expectedException(exception)) {
				seenUnexpectedException = true;
				throw exception;
			}

			if (hasNext())
				throw new TestAbortedException(
					format("Test execution #%d (of up to %d) failed ~> will retry in %d ms...", retriesSoFar,
						maxRetries, suspendForMs),
					exception);
			else
				throw new AssertionFailedError(format(
					"Test execution #%d (of up to %d with at least %d successes) failed ~> test fails - see cause for details",
					retriesSoFar, maxRetries, minSuccess), exception);
		}

		private boolean expectedException(Throwable exception) {
			// if not expected exceptions were specified, all are expected
			if (expectedExceptions.length == 0)
				return true;

			return Arrays.stream(expectedExceptions).anyMatch(type -> type.isInstance(exception));
		}

		private void suspendFor(int millis) {
			if (millis < 1) {
				return;
			}

			try {
				TimeUnit.MILLISECONDS.sleep(millis);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Thread interrupted during retry suspension.", ex);
			}
		}

		private boolean isFirstExecution() {
			return retriesSoFar == 0;
		}

		@Override
		public boolean hasNext() {
			// there's always at least one execution
			if (isFirstExecution())
				return true;
			if (seenFailedAssumption || seenUnexpectedException)
				return false;

			int successfulExecutionCount = retriesSoFar - exceptionsSoFar;
			int remainingExecutionCount = maxRetries - retriesSoFar;
			int requiredSuccessCount = minSuccess - successfulExecutionCount;

			return remainingExecutionCount >= requiredSuccessCount && requiredSuccessCount > 0;
		}

		@Override
		public RetryingTestInvocationContext next() {
			if (!hasNext())
				throw new NoSuchElementException();

			if (!isFirstExecution()) {
				suspendFor(suspendForMs);
			}

			retriesSoFar++;

			return new RetryingTestInvocationContext(formatter);
		}

	}

}
