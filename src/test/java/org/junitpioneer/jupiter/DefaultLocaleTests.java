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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junitpioneer.testkit.PioneerTestKit.executeTestClass;
import static org.junitpioneer.testkit.PioneerTestKit.executeTestMethod;
import static org.junitpioneer.testkit.assertion.PioneerAssert.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junitpioneer.testkit.ExecutionResults;

// On Java 19+, `Locale` constructors are deprecated.
// We ignore them until they're removed in #658
@SuppressWarnings("deprecation")
@DisplayName("DefaultLocale extension")
class DefaultLocaleTests {

	private static Locale TEST_DEFAULT_LOCALE;
	private static Locale DEFAULT_LOCALE_BEFORE_TEST;

	@BeforeAll
	static void globalSetUp() {
		DEFAULT_LOCALE_BEFORE_TEST = Locale.getDefault();
		TEST_DEFAULT_LOCALE = new Locale("custom");
		Locale.setDefault(TEST_DEFAULT_LOCALE);
	}

	@AfterAll
	static void globalTearDown() {
		Locale.setDefault(DEFAULT_LOCALE_BEFORE_TEST);
	}

	@Nested
	@DisplayName("applied on the method level")
	class MethodLevelTests {

		@Test
		@ReadsDefaultLocale
		@DisplayName("does nothing when annotation is not present")
		void testDefaultLocaleNoAnnotation() {
			assertThat(Locale.getDefault()).isEqualTo(TEST_DEFAULT_LOCALE);
		}

		@Test
		@DefaultLocale("zh-Hant-TW")
		@DisplayName("sets the default locale using a language tag")
		void setsLocaleViaLanguageTag() {
			assertThat(Locale.getDefault()).isEqualTo(Locale.forLanguageTag("zh-Hant-TW"));
		}

		@Test
		@DefaultLocale(language = "en_EN")
		@DisplayName("sets the default locale using a language")
		void setsLanguage() {
			assertThat(Locale.getDefault()).isEqualTo(new Locale("en_EN"));
		}

		@Test
		@DefaultLocale(language = "en", country = "EN")
		@DisplayName("sets the default locale using a language and a country")
		void setsLanguageAndCountry() {
			assertThat(Locale.getDefault()).isEqualTo(new Locale("en", "EN"));
		}

		@Test
		@DefaultLocale(language = "en", country = "EN", variant = "gb")
		@DisplayName("sets the default locale using a language, a country and a variant")
		void setsLanguageAndCountryAndVariant() {
			assertThat(Locale.getDefault()).isEqualTo(new Locale("en", "EN", "gb"));
		}

	}

	@Test
	@WritesDefaultLocale
	@DisplayName("applied on the class level, should execute tests with configured Locale")
	void shouldExecuteTestsWithConfiguredLocale() {
		ExecutionResults results = executeTestClass(ClassLevelTestCases.class);

		assertThat(results).hasNumberOfSucceededTests(2);
	}

	@DefaultLocale(language = "fr", country = "FR")
	static class ClassLevelTestCases {

		@Test
		@ReadsDefaultLocale
		void shouldExecuteWithClassLevelLocale() {
			assertThat(Locale.getDefault()).isEqualTo(new Locale("fr", "FR"));
		}

		@Test
		@DefaultLocale(language = "de", country = "DE")
		void shouldBeOverriddenWithMethodLevelLocale() {
			assertThat(Locale.getDefault()).isEqualTo(new Locale("de", "DE"));
		}

	}

	@Nested
	@DefaultLocale(language = "en")
	@DisplayName("with nested classes")
	class NestedDefaultLocaleTests {

		@Nested
		@DisplayName("without DefaultLocale annotation")
		class NestedClass {

			@Test
			@DisplayName("DefaultLocale should be set from enclosed class when it is not provided in nested")
			void shouldSetLocaleFromEnclosedClass() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");
			}

		}

		@Nested
		@DefaultLocale(language = "de")
		@DisplayName("with DefaultLocale annotation")
		class AnnotatedNestedClass {

			@Test
			@DisplayName("DefaultLocale should be set from nested class when it is provided")
			void shouldSetLocaleFromNestedClass() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("de");
			}

			@Test
			@DefaultLocale(language = "ch")
			@DisplayName("DefaultLocale should be set from method when it is provided")
			void shouldSetLocaleFromMethodOfNestedClass() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("ch");
			}

		}

	}

	@Nested
	@DefaultLocale(language = "fi")
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@DisplayName("correctly sets/resets before/after each/all extension points")
	class ResettingDefaultLocaleTests {

		@Nested
		@DefaultLocale(language = "de")
		@TestInstance(TestInstance.Lifecycle.PER_CLASS)
		class ResettingDefaultLocaleNestedTests {

			@Test
			@DefaultLocale(language = "en")
			void setForTestMethod() {
				// only here to set the locale, so another test can verify whether it was reset;
				// still, better to assert the value was actually set
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");
			}

			@AfterAll
			@ReadsDefaultTimeZone
			void resetAfterTestMethodExecution() {
				assertThat(Locale.getDefault().getLanguage()).isEqualTo("custom");
			}

		}

		@AfterAll
		@ReadsDefaultTimeZone
		void resetAfterTestMethodExecution() {
			assertThat(Locale.getDefault().getLanguage()).isEqualTo("custom");
		}

	}

	@DefaultLocale(language = "en")
	static class ClassLevelResetTestCase {

		@Test
		void setForTestMethod() {
			// only here to set the locale, so another test can verify whether it was reset;
			// still, better to assert the value was actually set
			assertThat(Locale.getDefault().getLanguage()).isEqualTo("en");
		}

	}

	@Nested
	@DisplayName("when configured incorrect")
	class ConfigurationFailureTests {

		@Nested
		@DisplayName("on the method level")
		class MethodLevel {

			@Test
			@DisplayName("should fail when nothing is configured")
			void shouldFailWhenNothingIsConfigured() {
				ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
					"shouldFailMissingConfiguration");

				assertThat(results)
						.hasSingleFailedTest()
						.withExceptionInstanceOf(ExtensionConfigurationException.class);
			}

			@Test
			@DisplayName("should fail when variant is set but country is not")
			void shouldFailWhenVariantIsSetButCountryIsNot() {
				ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
					"shouldFailMissingCountry");

				assertThat(results)
						.hasSingleFailedTest()
						.withExceptionInstanceOf(ExtensionConfigurationException.class);
			}

			@Test
			@DisplayName("should fail when languageTag and language is set")
			void shouldFailWhenLanguageTagAndLanguageIsSet() {
				ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
					"shouldFailLanguageTagAndLanguage");

				assertThat(results)
						.hasSingleFailedTest()
						.withExceptionInstanceOf(ExtensionConfigurationException.class);
			}

			@Test
			@DisplayName("should fail when languageTag and country is set")
			void shouldFailWhenLanguageTagAndCountryIsSet() {
				ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
					"shouldFailLanguageTagAndCountry");

				assertThat(results)
						.hasSingleFailedTest()
						.withExceptionInstanceOf(ExtensionConfigurationException.class);
			}

			@Test
			@DisplayName("should fail when languageTag and variant is set")
			void shouldFailWhenLanguageTagAndVariantIsSet() {
				ExecutionResults results = executeTestMethod(MethodLevelInitializationFailureTestCases.class,
					"shouldFailLanguageTagAndVariant");

				assertThat(results)
						.hasSingleFailedTest()
						.withExceptionInstanceOf(ExtensionConfigurationException.class);
			}

		}

		@Nested
		@DisplayName("on the class level")
		class ClassLevel {

			@Test
			@DisplayName("should fail when variant is set but country is not")
			void shouldFailWhenVariantIsSetButCountryIsNot() {
				ExecutionResults results = executeTestClass(ClassLevelInitializationFailureTestCases.class);

				assertThat(results)
						.hasSingleFailedTest()
						.withExceptionInstanceOf(ExtensionConfigurationException.class);
			}

		}

	}

	static class MethodLevelInitializationFailureTestCases {

		@Test
		@DefaultLocale
		void shouldFailMissingConfiguration() {
		}

		@Test
		@DefaultLocale(language = "de", variant = "ch")
		void shouldFailMissingCountry() {
		}

		@Test
		@DefaultLocale(value = "Something", language = "de")
		void shouldFailLanguageTagAndLanguage() {
		}

		@Test
		@DefaultLocale(value = "Something", country = "DE")
		void shouldFailLanguageTagAndCountry() {
		}

		@Test
		@DefaultLocale(value = "Something", variant = "ch")
		void shouldFailLanguageTagAndVariant() {
		}

	}

	@DefaultLocale(language = "de", variant = "ch")
	static class ClassLevelInitializationFailureTestCases {

		@Test
		void shouldFail() {
		}

	}

	@Nested
	@DisplayName("used with inheritance")
	class InheritanceTests extends InheritanceBaseTest {

		@Test
		@DisplayName("should inherit default locale annotation")
		void shouldInheritClearAndSetProperty() {
			assertThat(Locale.getDefault()).isEqualTo(new Locale("fr", "FR"));
		}

	}

	@DefaultLocale(language = "fr", country = "FR")
	static class InheritanceBaseTest {

	}

}
