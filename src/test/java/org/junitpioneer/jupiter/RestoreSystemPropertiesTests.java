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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.platform.commons.support.HierarchyTraversalMode;
import org.junit.platform.commons.support.ReflectionSupport;
import org.junitpioneer.testkit.assertion.PropertiesAssert;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;

/**
 * Verify proper behavior when annotated on a top level class
 *
 * VerifySysPropsExtension is registered as an extension BEFORE RestoreSystemProperties.
 * VerifySysPropsExtension stores the initial Sys Props and verifies them at the end.
 */
@DisplayName("RestoreSystemProperties Annotation")
@ExtendWith(RestoreSystemPropertiesTests.VerifySysPropsExtension.class)	// 1st: Order is important here
@RestoreSystemProperties																// 2nd
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Execution(SAME_THREAD)	// Single thread.  See VerifySysPropsExtension inner class
class RestoreSystemPropertiesTests {

	@BeforeAll
	static void globalSetUp() {
		System.setProperty("A", "all sys A");
		System.setProperty("B", "all sys B");

		// Replace Sys Props w/ a crazy new instance using defaults for all values.
		// RestoreSystemProperties should ensure this instance is in place for Sys Props before
		// each test and auto-revert to the original Sys Prop instance when the Test class is done.
		Properties orgProps = SystemPropertyExtension.createEffectiveClone(System.getProperties());	// Will include A & B from above
		Properties newProps = new Properties(orgProps);

		newProps.setProperty("C", "all sys C");	//	one prop actually set (not a default)

		System.setProperties(newProps);
	}

	@AfterAll
	static void globalTearDown() {
		// Can I break this??
		// Restore should restore it after @AfterAll - will be verified in VerifySysPropsExtension
		System.setProperties(new Properties());
	}

	@BeforeEach
	void beforeEachMethod() {
		System.setProperty("M", "each sys M");
		System.setProperty("N", "each sys N");
		System.setProperty("O", "each sys O");
	}

	// Test a util method used in this test class
	@Test @Order(1)
	@DisplayName("Verify local deepClone method")
	public void deepCloneTest() throws Exception {
		Properties inner1 = new Properties();
		Properties inner2 = new Properties(inner1);
		Properties outer = new Properties(inner2);
		final Object B_OBJ = new Object();
		final Object D_OBJ = new Object();
		final Object F_OBJ = new Object();

		inner1.setProperty("A", "is A");
		inner1.put("B", B_OBJ);
		inner2.setProperty("C", "is C");
		inner2.put("D", "is D");
		outer.setProperty("E", "is E");
		outer.put("F", F_OBJ);

		Properties cloned = deepClone(outer);
		PropertiesAssert.assertThat(cloned).isStrictlyTheSameAs(outer);
	}

	@Test @Order(2)
	@DisplayName("verify initial state from BeforeAll & BeforeEach and set prop")
	void verifyInitialState() {
		assertThat(System.getProperty("A")).isEqualTo("all sys A");
		assertThat(System.getProperty("B")).isEqualTo("all sys B");
		assertThat(System.getProperty("C")).isEqualTo("all sys C");

		assertThat(System.getProperty("M")).isEqualTo("each sys M");
		assertThat(System.getProperty("N")).isEqualTo("each sys N");
		assertThat(System.getProperty("O")).isEqualTo("each sys O");

		System.setProperty("X", "method X");	// SHOULDN'T BE VISIBLE IN NEXT TEST
	}

	@Test @Order(3)
	@DisplayName("Property X from the previous test should have been reset")
	void shouldNotSeeChangesFromPreviousTest() {
		assertThat(System.getProperty("X")).isNull();
	}

	@Nested
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@DisplayName("Nested tests should inherit restore behavior and be able to override")
	class NestedTests {

		@BeforeEach
		void methodSetUp() {
			System.setProperty("M", "each sys M Nest");
		}

		@Test @Order(1)
		@DisplayName("initial state from nested BeforeAll & BeforeEach and set prop")
		void verifyInitialState() {
			assertThat(System.getProperty("A")).isEqualTo("all sys A");
			assertThat(System.getProperty("B")).isEqualTo("all sys B");
			assertThat(System.getProperty("C")).isEqualTo("all sys C");

			assertThat(System.getProperty("M")).isEqualTo("each sys M Nest");
			assertThat(System.getProperty("N")).isEqualTo("each sys N");
			assertThat(System.getProperty("O")).isEqualTo("each sys O");

			System.setProperty("X", "method X");
		}

		@Test @Order(2)
		@DisplayName("Property X from the previous test should have been reset")
		void shouldNotSeeChangesFromPreviousTest() {
			assertThat(System.getProperty("X")).isNull();
		}
	}

	/**
	 * Extension that checks the before and after state of SysProps.
	 * <p>
	 * Must be registered before RestoreSystemProperties.
	 * To avoid replicating the system being tested w/ the test itself, this class
	 * uses static state rather than the extension store.  As a result, this test
	 * class is marked as single threaded.
	 */
	protected static class VerifySysPropsExtension
			implements BeforeEachCallback, AfterEachCallback, BeforeAllCallback, AfterAllCallback {

		/* Nested tests will push additional copies */
		private static ArrayDeque<Properties> beforeAllState = new ArrayDeque<>();

		/* Only one test method happens at a time */
		private static Properties beforeEachState;

		@Override
		public void beforeAll(final ExtensionContext context) throws Exception {
			beforeAllState.push(deepClone(System.getProperties()));
		}

		@Override
		public void afterAll(final ExtensionContext context) throws Exception {
			Properties preTest = beforeAllState.pop();
			Properties actual = System.getProperties();

			assertThat(preTest).isNotNull();
			PropertiesAssert.assertThat(actual).isEffectivelyTheSameAs(preTest);
			PropertiesAssert.assertThat(actual).isStrictlyTheSameAs(preTest);
		}

		@Override
		public void beforeEach(final ExtensionContext context) throws Exception {
			beforeEachState = deepClone(System.getProperties());
		}

		@Override
		public void afterEach(final ExtensionContext context) throws Exception {
			Properties preTest = beforeEachState;
			Properties actual = System.getProperties();

			assertThat(preTest).isNotNull();
			PropertiesAssert.assertThat(actual).isEffectivelyTheSameAs(preTest);
			PropertiesAssert.assertThat(actual).isStrictlyTheSameAs(preTest);
			beforeEachState = null;
		}

	}


	/**
	 * This 'deep' clone method uses reflection to do a clone that preserves the structure
	 * (i.e. nested defaults) and potential non-string values of Properties.
	 * This method is only used to ensure we have a 100% complete clone of original Sys Props for
	 * comparison after restore.
	 * <p>
	 * The actual SystemProperties extension does an 'effective' clone which is simpler and doesn't
	 * require reflection.
	 *
	 * @param original Props to be cloned
	 * @return A detached clone of the original Props
	 * @throws Exception Possibly due to reflection access
	 */
	static public Properties deepClone(Properties original) throws Exception {

		Properties clonedDefaults = null;
		Properties defaults = getDefaultPropertiesInstance(original);

		if (defaults != null) {
			clonedDefaults = deepClone(defaults);
		}

		final Properties clone = new Properties(clonedDefaults);

		// Copy just the values directly in Map backing the Properties
		original.keySet().forEach(k -> {
			clone.put(k, original.get(k));
		});

		return clone;
	}

	/**
	 * Helper method for 'deepClone' that uses reflection to grab the 'defaults' properties field
	 * within a Properties object.
	 *
	 * @param parent Instance to grab the 'defaults' field from
	 * @return A Properties object, which may be null if the actual defaults are null.
	 * @throws Exception Possibly due to reflection access
	 */
	static public Properties getDefaultPropertiesInstance(Properties parent) throws Exception {
		Field field = ReflectionSupport
				.findFields(Properties.class, f -> f.getName().equals("defaults"), HierarchyTraversalMode.BOTTOM_UP)
				.stream().findFirst().get();

		field.setAccessible(true);
		Properties theDefault = (Properties) ReflectionSupport.tryToReadFieldValue(field, parent).get();

		return theDefault;
	}

}