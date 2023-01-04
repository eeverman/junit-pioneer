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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.parallel.Execution;
import org.junitpioneer.testkit.ExecutionResults;
import org.junitpioneer.testkit.assertion.PropertiesAssert;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD;
import static org.junitpioneer.testkit.PioneerTestKit.executeTestMethod;
import static org.junitpioneer.testkit.assertion.PioneerAssert.assertThat;

@DisplayName("SystemProperty extension")
class SystemPropertyExtensionTests {

	@BeforeAll
	static void globalSetUp() {
		System.setProperty("A", "old A");
		System.setProperty("B", "old B");
		System.setProperty("C", "old C");

		System.clearProperty("clear prop D");
		System.clearProperty("clear prop E");
		System.clearProperty("clear prop F");
	}

	@AfterAll
	static void globalTearDown() {
		System.clearProperty("A");
		System.clearProperty("B");
		System.clearProperty("C");

		assertThat(System.getProperty("clear prop D")).isNull();
		assertThat(System.getProperty("clear prop E")).isNull();
		assertThat(System.getProperty("clear prop F")).isNull();
	}

	@Nested
	@DisplayName("used with ClearSystemProperty")
	@ClearSystemProperty(key = "A")
	class ClearSystemPropertyTests {

		@Test
		@DisplayName("should clear system property")
		@ClearSystemProperty(key = "B")
		void shouldClearSystemProperty() {
			assertThat(System.getProperty("A")).isNull();
			assertThat(System.getProperty("B")).isNull();
			assertThat(System.getProperty("C")).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isNull();
			assertThat(System.getProperty("clear prop E")).isNull();
			assertThat(System.getProperty("clear prop F")).isNull();
		}

		@Test
		@DisplayName("should be repeatable")
		@ClearSystemProperty(key = "B")
		@ClearSystemProperty(key = "C")
		void shouldBeRepeatable() {
			assertThat(System.getProperty("A")).isNull();
			assertThat(System.getProperty("B")).isNull();
			assertThat(System.getProperty("C")).isNull();

			assertThat(System.getProperty("clear prop D")).isNull();
			assertThat(System.getProperty("clear prop E")).isNull();
			assertThat(System.getProperty("clear prop F")).isNull();
		}

	}

	@Nested
	@DisplayName("used with SetSystemProperty")
	@SetSystemProperty(key = "A", value = "new A")
	class SetSystemPropertyTests {

		@Test
		@DisplayName("should set system property to value")
		@SetSystemProperty(key = "B", value = "new B")
		void shouldSetSystemPropertyToValue() {
			assertThat(System.getProperty("A")).isEqualTo("new A");
			assertThat(System.getProperty("B")).isEqualTo("new B");
			assertThat(System.getProperty("C")).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isNull();
			assertThat(System.getProperty("clear prop E")).isNull();
			assertThat(System.getProperty("clear prop F")).isNull();
		}

		@Test
		@DisplayName("should be repeatable")
		@SetSystemProperty(key = "B", value = "new B")
		@SetSystemProperty(key = "clear prop D", value = "new D")
		void shouldBeRepeatable() {
			assertThat(System.getProperty("A")).isEqualTo("new A");
			assertThat(System.getProperty("B")).isEqualTo("new B");
			assertThat(System.getProperty("C")).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isEqualTo("new D");
			assertThat(System.getProperty("clear prop E")).isNull();
			assertThat(System.getProperty("clear prop F")).isNull();
		}

	}

	@Nested
	@DisplayName("used with both ClearSystemProperty and SetSystemProperty")
	@ClearSystemProperty(key = "A")
	@SetSystemProperty(key = "clear prop D", value = "new D")
	class CombinedClearAndSetTests {

		@Test
		@DisplayName("should be combinable")
		@ClearSystemProperty(key = "B")
		@SetSystemProperty(key = "clear prop E", value = "new E")
		void clearAndSetSystemPropertyShouldBeCombinable() {
			assertThat(System.getProperty("A")).isNull();
			assertThat(System.getProperty("B")).isNull();
			assertThat(System.getProperty("C")).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isEqualTo("new D");
			assertThat(System.getProperty("clear prop E")).isEqualTo("new E");
			assertThat(System.getProperty("clear prop F")).isNull();
		}

		@Test
		@DisplayName("method level should overwrite class level")
		@ClearSystemProperty(key = "clear prop D")
		@SetSystemProperty(key = "A", value = "new A")
		void methodLevelShouldOverwriteClassLevel() {
			assertThat(System.getProperty("A")).isEqualTo("new A");
			assertThat(System.getProperty("B")).isEqualTo("old B");
			assertThat(System.getProperty("C")).isEqualTo("old C");

			assertThat(System.getProperty("clear prop D")).isNull();
			assertThat(System.getProperty("clear prop E")).isNull();
			assertThat(System.getProperty("clear prop F")).isNull();
		}

		@Test
		@Issue("473")
		@DisplayName("method level should not clash (in terms of duplicate entries) with class level")
		@SetSystemProperty(key = "A", value = "new A")
		void methodLevelShouldNotClashWithClassLevel() {
			assertThat(System.getProperty("A")).isEqualTo("new A");
			assertThat(System.getProperty("B")).isEqualTo("old B");
			assertThat(System.getProperty("C")).isEqualTo("old C");
			assertThat(System.getProperty("clear prop D")).isEqualTo("new D");

			assertThat(System.getProperty("clear prop E")).isNull();
			assertThat(System.getProperty("clear prop F")).isNull();
		}

	}

	@Nested
	@DisplayName("used with Clear, Set and Restore")
	@WritesSystemProperty	// Many of these tests write, many also access
	@Execution(SAME_THREAD)	// Uses instance state
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)	// Uses instance state
	@TestClassOrder(ClassOrderer.OrderAnnotation.class)
	class CombinedClearSetRestoreTests {

		Properties initialState;		// stateful

		@BeforeAll
		public void beforeAll() {
			initialState = System.getProperties();
		}

		@Nested @Order(1)
		@DisplayName("Set, Clear & Restore on class")
		@ClearSystemProperty(key = "A")
		@SetSystemProperty(key = "clear prop D", value = "new D")
		@RestoreSystemProperties
		@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)	// Uses instance state
		class SetClearRestoreOnClass {

			@AfterAll
			public void afterAll() {
				System.setProperties(new Properties());		// Really blow it up after this class
			}

			@Test	@Order(1)
			@DisplayName("Set, Clear on method w/ direct set Sys Prop")
			@ClearSystemProperty(key = "B")
			@SetSystemProperty(key = "clear prop E", value = "new E")
			void clearSetRestoreShouldBeCombinable() {

				assertThat(System.getProperties())
						.withFailMessage("Restore should swap out the Sys Properties instance")
						.isNotSameAs(initialState);

				// Direct modification
				System.setProperty("Restore", "Restore Me");	// SHOULDN'T BE VISIBLE IN NEXT TEST
				System.getProperties().put("XYZ", this);			// SHOULDN'T BE VISIBLE IN NEXT TEST

				assertThat(System.getProperty("Restore")).isEqualTo("Restore Me");
				assertThat(System.getProperties().get("XYZ")).isSameAs(this);

				// All the others
				assertThat(System.getProperty("A")).isNull();
				assertThat(System.getProperty("B")).isNull();
				assertThat(System.getProperty("C")).isEqualTo("old C");

				assertThat(System.getProperty("clear prop D")).isEqualTo("new D");
				assertThat(System.getProperty("clear prop E")).isEqualTo("new E");
				assertThat(System.getProperty("clear prop F")).isNull();
			}

			@Test
			@DisplayName("Restore from class should restore direct mods")
			@Order(2)
			void restoreShouldHaveRevertedDirectModification() {
				assertThat(System.getProperty("Restore")).isNull();
				assertThat(System.getProperties().get("XYZ")).isNull();
			}
		}

		@Nested @Order(2)
		@DisplayName("Prior nested class changes should be restored}")
		class priorNestedChangesRestored {
			@Test
			@DisplayName("Restore from class should restore direct mods")
			void restoreShouldHaveRevertedDirectModification() {
				PropertiesAssert.assertThat(System.getProperties()).isStrictlyTheSameAs(initialState);
			}
		}

		@Nested @Order(3)
		@DisplayName("Set & Clear on class, Restore on method")
		@ClearSystemProperty(key = "A")
		@SetSystemProperty(key = "clear prop D", value = "new D")
		@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)	// Uses instance state
		class SetAndClearOnClass {

			Properties initialState;		// stateful

			@BeforeAll
			public void beforeAll() { initialState = System.getProperties(); }


			@Test	@Order(1)
			@DisplayName("Set, Clear & Restore on method w/ direct set Sys Prop")
			@ClearSystemProperty(key = "B")
			@SetSystemProperty(key = "clear prop E", value = "new E")
			@RestoreSystemProperties
			void clearSetRestoreShouldBeCombinable() {

				assertThat(System.getProperties())
						.withFailMessage("Restore should swap out the Sys Properties instance")
						.isNotSameAs(initialState);

				// Direct modification
				System.setProperty("Restore", "Restore Me");	// SHOULDN'T BE VISIBLE IN NEXT TEST
				System.getProperties().put("XYZ", this);			// SHOULDN'T BE VISIBLE IN NEXT TEST

				// All the others
				assertThat(System.getProperty("A")).isNull();
				assertThat(System.getProperty("B")).isNull();
				assertThat(System.getProperty("C")).isEqualTo("old C");

				assertThat(System.getProperty("clear prop D")).isEqualTo("new D");
				assertThat(System.getProperty("clear prop E")).isEqualTo("new E");
				assertThat(System.getProperty("clear prop F")).isNull();
			}

			@Test
			@DisplayName("Restore from prior method should restore direct mods")
			@Order(2)
			void restoreShouldHaveRevertedDirectModification() {
				assertThat(System.getProperty("Restore")).isNull();
				assertThat(System.getProperties().get("XYZ")).isNull();
				PropertiesAssert.assertThat(System.getProperties()).isStrictlyTheSameAs(initialState);
			}

		}
	}

	@Nested
	@DisplayName("with nested classes")
	@ClearSystemProperty(key = "A")
	@SetSystemProperty(key = "B", value = "new B")
	class NestedSystemPropertyTests {

		@Nested
		@TestMethodOrder(OrderAnnotation.class)
		@DisplayName("without SystemProperty annotations")
		class NestedClass {

			@Test
			@Order(1)
			@ReadsSystemProperty
			@DisplayName("system properties should be set from enclosed class when they are not provided in nested")
			void shouldSetSystemPropertyFromEnclosedClass() {
				assertThat(System.getProperty("A")).isNull();
				assertThat(System.getProperty("B")).isEqualTo("new B");
			}

			@Test
			@Issue("480")
			@Order(2)
			@ReadsSystemProperty
			@DisplayName("system properties should be set from enclosed class after restore")
			void shouldSetSystemPropertyFromEnclosedClassAfterRestore() {
				assertThat(System.getProperty("A")).isNull();
				assertThat(System.getProperty("B")).isEqualTo("new B");
			}

		}

		@Nested
		@SetSystemProperty(key = "B", value = "newer B")
		@DisplayName("with SetSystemProperty annotation")
		class AnnotatedNestedClass {

			@Test
			@ReadsSystemProperty
			@DisplayName("system property should be set from nested class when it is provided")
			void shouldSetSystemPropertyFromNestedClass() {
				assertThat(System.getProperty("B")).isEqualTo("newer B");
			}

			@Test
			@SetSystemProperty(key = "B", value = "newest B")
			@DisplayName("system property should be set from method when it is provided")
			void shouldSetSystemPropertyFromMethodOfNestedClass() {
				assertThat(System.getProperty("B")).isEqualTo("newest B");
			}

		}

	}

	@Nested
	@SetSystemProperty(key = "A", value = "new A")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	class ResettingSystemPropertyTests {

		@Nested
		@SetSystemProperty(key = "A", value = "newer A")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class ResettingSystemPropertyAfterEachNestedTests {

			@BeforeEach
			void changeShouldBeVisible() {
				// we already see "newest A" because BeforeEachCallBack is invoked before @BeforeEach
				// see https://junit.org/junit5/docs/current/user-guide/#extensions-execution-order-overview
				assertThat(System.getProperty("A")).isEqualTo("newest A");
			}

			@Test
			@SetSystemProperty(key = "A", value = "newest A")
			void setForTestMethod() {
				assertThat(System.getProperty("A")).isEqualTo("newest A");
			}

			@AfterEach
			@ReadsSystemProperty
			void resetAfterTestMethodExecution() {
				// we still see "newest A" because AfterEachCallBack is invoked after @AfterEach
				// see https://junit.org/junit5/docs/current/user-guide/#extensions-execution-order-overview
				assertThat(System.getProperty("A")).isEqualTo("newest A");
			}

		}

		@Nested
		@SetSystemProperty(key = "A", value = "newer A")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class ResettingSystemPropertyAfterAllNestedTests {

			@BeforeAll
			void changeShouldBeVisible() {
				assertThat(System.getProperty("A")).isEqualTo("newer A");
			}

			@Test
			@SetSystemProperty(key = "A", value = "newest A")
			void setForTestMethod() {
				assertThat(System.getProperty("A")).isEqualTo("newest A");
			}

			@AfterAll
			@ReadsSystemProperty
			void resetAfterTestMethodExecution() {
				assertThat(System.getProperty("A")).isEqualTo("newer A");
			}

		}

		@AfterAll
		@ReadsSystemProperty
		void resetAfterTestContainerExecution() {
			assertThat(System.getProperty("A")).isEqualTo("new A");
		}

	}

	@Nested
	@DisplayName("used with incorrect configuration")
	class ConfigurationFailureTests {

		@Test
		@DisplayName("should fail when clear and set same system property")
		void shouldFailWhenClearAndSetSameSystemProperty() {
			ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
				"shouldFailWhenClearAndSetSameSystemProperty");

			assertThat(results).hasSingleFailedTest().withExceptionInstanceOf(ExtensionConfigurationException.class);
		}

		@Test
		@DisplayName("should fail when clear same system property twice")
		@Disabled("This can't happen at the moment, because Jupiter's annotation tooling "
				+ "deduplicates identical annotations like the ones required for this test: "
				+ "https://github.com/junit-team/junit5/issues/2131")
		void shouldFailWhenClearSameSystemPropertyTwice() {
			ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
				"shouldFailWhenClearSameSystemPropertyTwice");

			assertThat(results).hasSingleFailedTest().withExceptionInstanceOf(ExtensionConfigurationException.class);
		}

		@Test
		@DisplayName("should fail when set same system property twice")
		void shouldFailWhenSetSameSystemPropertyTwice() {
			ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
				"shouldFailWhenSetSameSystemPropertyTwice");
			assertThat(results).hasSingleFailedTest().withExceptionInstanceOf(ExtensionConfigurationException.class);
		}


	}
	static class MethodLevelInitializationFailureTestCases {

		@Test
		@DisplayName("clearing and setting the same property")
		@ClearSystemProperty(key = "A")
		@SetSystemProperty(key = "A", value = "new A")
		void shouldFailWhenClearAndSetSameSystemProperty() {
		}

		@Test
		@ClearSystemProperty(key = "A")
		@ClearSystemProperty(key = "A")
		void shouldFailWhenClearSameSystemPropertyTwice() {
		}

		@Test
		@SetSystemProperty(key = "A", value = "new A")
		@SetSystemProperty(key = "A", value = "new B")
		void shouldFailWhenSetSameSystemPropertyTwice() {
		}

	}

	@Nested
	@DisplayName("used with inheritance")
	class InheritanceClearAndSetTests extends InheritanceClearAndSetBaseTest {

		@Test
		@Issue("448")
		@DisplayName("should inherit clear and set annotations")
		void shouldInheritClearAndSetProperty() {
			assertThat(System.getProperty("A")).isNull();
			assertThat(System.getProperty("B")).isNull();
			assertThat(System.getProperty("clear prop D")).isEqualTo("new D");
			assertThat(System.getProperty("clear prop E")).isEqualTo("new E");
		}

	}

	@Nested
	@DisplayName("used with inheritance")
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	@Execution(SAME_THREAD)	// Uses instance state
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)	// Uses instance state
	class InheritanceClearSetAndRestoreTests extends InheritanceClearSetAndRestoreBaseTest {

		Properties initialState;		// stateful

		@BeforeAll
		public void beforeAll() { initialState = System.getProperties(); }

		@Test		@Order(1)
		@DisplayName("should inherit clear and set annotations")
		void shouldInheritClearAndSetProperty() {

			// Direct modification
			System.setProperty("Restore", "Restore Me");	// SHOULDN'T BE VISIBLE IN NEXT TEST
			System.getProperties().put("XYZ", this);			// SHOULDN'T BE VISIBLE IN NEXT TEST

			assertThat(System.getProperty("A")).isNull();
			assertThat(System.getProperty("B")).isNull();
			assertThat(System.getProperty("clear prop D")).isEqualTo("new D");
			assertThat(System.getProperty("clear prop E")).isEqualTo("new E");
		}

		@Test
		@DisplayName("Restore from class should restore direct mods")
		@Order(2)
		void restoreShouldHaveRevertedDirectModification() {
			assertThat(System.getProperty("Restore")).isNull();
			assertThat(System.getProperties().get("XYZ")).isNull();
			PropertiesAssert.assertThat(System.getProperties()).isStrictlyTheSameAs(initialState);
		}

	}

	@Nested
	@DisplayName("Unit tests of individual methods")
	@WritesSystemProperty	// Many of these tests write, many also access
	class UnitTests {
		SystemPropertyExtension spe;

		@BeforeEach
		public void beforeEach() {
			spe = new SystemPropertyExtension();
		}

		@Nested
		@DisplayName("Attributes of RestoreSystemProperties")
		class BasicAttributesOfRestoreSystemProperties {

			@Test
			@DisplayName("Restore ann has correct markers")
			public void restoreHasCorrectMarkers() {
				assertThat(RestoreSystemProperties.class)
						.hasAnnotations(Inherited.class, WritesSystemProperty.class);
			}

			@Test
			@DisplayName("Restore ann has correct retention")
			public void restoreHasCorrectRetention() {
				assertThat(RestoreSystemProperties.class.getAnnotation(Retention.class).value())
						.isEqualTo(RetentionPolicy.RUNTIME);
			}

			@Test
			@DisplayName("Restore ann has correct targets")
			public void restoreHasCorrectTargets() {
				assertThat(RestoreSystemProperties.class.getAnnotation(Target.class).value())
						.containsExactlyInAnyOrder(ElementType.METHOD, ElementType.TYPE);
			}

		}
		@Nested
		@DisplayName("cloneProperties Tests")
		class ClonePropertiesTests {

			Properties inner;
			Properties outer;	//Created w/ inner as nested defaults

			@BeforeEach
			public void beforeEach() {
				inner = new Properties();
				outer = new Properties(inner);

				inner.setProperty("A", "is A");
				outer.setProperty("B", "is B");
			}

			@Test
			@DisplayName("Nested defaults handled")
			void nestedDefaultsHandled() {
				Properties cloned = SystemPropertyExtension.createEffectiveClone(outer);
				PropertiesAssert.assertThat(cloned).isEffectivelyTheSameAs(outer);
			}

			@Test
			@DisplayName("Object values are skipped")
			void objectValuesAreSkipped() {
				inner.put("inner_obj", new Object());
				outer.put("outer_obj", new Object());
				Properties cloned = SystemPropertyExtension.createEffectiveClone(outer);

				PropertiesAssert.assertThat(cloned).isEffectivelyTheSameAs(outer);
				assertThat(cloned.contains("inner_obj")).isFalse();
				assertThat(cloned.contains("outer_obj")).isFalse();
			}

		}

		@Nested
		@DisplayName("RestorableContext Workflow Tests")
		class RestorableContextWorkflowTests {
			@Test
			@DisplayName("Workflow of RestorableContext")
			void workflowOfRestorableContexts() {
				Properties initialState = System.getProperties();	//This is a live reference

				try {
					Properties returnedFromPrepareToEnter = spe.prepareToEnterRestorableContext();
					Properties postPrepareToEnterSysProps = System.getProperties();
					spe.prepareToExitRestorableContext(initialState);
					Properties postPrepareToExitSysProps = System.getProperties();

					PropertiesAssert.assertThat(returnedFromPrepareToEnter)
							.withFailMessage("prepareToEnterRestorableContext should return actual original or deep copy")
							.isStrictlyTheSameAs(initialState);

					assertThat(returnedFromPrepareToEnter)
							.withFailMessage("prepareToEnterRestorableContext should replace the actual Sys Props")
							.isNotSameAs(postPrepareToEnterSysProps);

					PropertiesAssert.assertThat(postPrepareToEnterSysProps).isEffectivelyTheSameAs(initialState);

					// Could assert isSameAs, but a deep copy would also be allowed
					PropertiesAssert.assertThat(postPrepareToExitSysProps).isStrictlyTheSameAs(initialState);

				} finally {
					System.setProperties(initialState);	// Ensure complete recovery
				}
			}
		}

	}

	@ClearSystemProperty(key = "A")
	@ClearSystemProperty(key = "B")
	@SetSystemProperty(key = "clear prop D", value = "new D")
	@SetSystemProperty(key = "clear prop E", value = "new E")
	static class InheritanceClearAndSetBaseTest {

	}

	@ClearSystemProperty(key = "A")
	@ClearSystemProperty(key = "B")
	@SetSystemProperty(key = "clear prop D", value = "new D")
	@SetSystemProperty(key = "clear prop E", value = "new E")
	@RestoreSystemProperties
	static class InheritanceClearSetAndRestoreBaseTest {

	}

}
