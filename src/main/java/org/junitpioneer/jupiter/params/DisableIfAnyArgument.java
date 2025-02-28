/*
 * Copyright 2016-2022 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junitpioneer.jupiter.params;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.extension.ExtendWith;

/**
 * {@code @DisableIfAnyArgument} is a JUnit Jupiter extension that selectively disables
 * a {@link org.junit.jupiter.params.ParameterizedTest} execution if at least one
 * argument (as defined by {@link Object#toString()}) satisfies the specified condition.
 *
 * <p>The extension uses Jupiter's {@link org.junit.jupiter.api.extension.InvocationInterceptor}.
 * It's important to note that since it's marked as {@link org.apiguardian.api.API.Status#EXPERIMENTAL}
 * it might be removed without prior notice.
 * Unlike {@link org.junit.jupiter.api.Disabled} annotations, this extension doesn't disable the whole test method.
 * With {@code DisableIfAnyArgument}, it is possible to selectively disable tests out of the plethora
 * of dynamically registered parameterized tests.</p>
 *
 * <p>The extension requires that exactly one of {@link DisableIfAnyArgument#contains() contains} or
 * {@link DisableIfAnyArgument#matches() matches} is configured.</p>
 *
 * <p>For more information how the extension resolves the annotations, check
 * <a href="https://junit-pioneer.org/docs/disable-parameterized-tests/" target="_top">the documentation.</a></p>
 *
 * @see DisableIfArgumentExtension
 * @since 1.5.0
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DisableIfArgumentExtension.class)
public @interface DisableIfAnyArgument {

	/**
	 * Disable test cases if any argument (converted to String with {@link Object#toString()})
	 * contains any of the specified strings (according to {@link String#contains(CharSequence)}).
	 */
	String[] contains() default {};

	/**
	 * Disable test cases if any argument (converted to String with {@link Object#toString()})
	 * matches any of the specified regular expressions (according to {@link String#matches(String)}).
	 */
	String[] matches() default {};

}
