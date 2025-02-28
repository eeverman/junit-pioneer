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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.params.provider.ArgumentsSource;

/**
 * {@code @IntRangeSource} is an {@link ArgumentsSource} that provides access to a range of {@code int} values.
 *
 * <p>The supplied values will be provided as arguments to the annotated {@code @ParameterizedTest} method.</p>
 *
 * <p>For more details and examples, see
 * <a href="https://junit-pioneer.org/docs/range-sources/" target="_top">the documentation on <code>Range Sources</code></a>
 * </p>
 *
 * <p>This annotation is {@link Repeatable}, to make it usable with {@link org.junitpioneer.jupiter.CartesianProductTest}.
 * If used with {@link org.junit.jupiter.params.ParameterizedTest}, it can only be used once (because {@code ParameterizedTest}
 * can only take a single {@link ArgumentsSource}). Using it more than once will throw an {@link IllegalArgumentException}.
 * If used with {@link org.junitpioneer.jupiter.CartesianProductTest}, it can be repeated to provide arguments to
 * more than one parameter.
 *
 * <p>This annotation can be used on a method parameter, to make it usable with
 * {@link org.junitpioneer.jupiter.cartesian.CartesianTest}. If used with {@link org.junit.jupiter.params.ParameterizedTest},
 * the annotation has to be on the method itself as any other {@link ArgumentsSource}.
 *
 * <p>
 * For more details and examples, see
 * <a href="https://junit-pioneer.org/docs/cartesian-product/" target="_top">the documentation on <code>Cartesian product tests</code></a>
 * </p>
 *
 * @since 0.5
 * @see ArgumentsSource
 * @see org.junit.jupiter.params.ParameterizedTest
 * @see org.junitpioneer.jupiter.CartesianProductTest
 * @see org.junitpioneer.jupiter.cartesian.CartesianTest
 */
@Target({ ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ArgumentsSource(RangeSourceArgumentsProvider.class)
@RangeClass(IntRange.class)
@Repeatable(IntRangeSource.IntRangeSources.class)
public @interface IntRangeSource {

	/**
	 * The starting point of the range, inclusive.
	 */
	int from();

	/**
	 * The end point of the range, exclusive.
	 */
	int to();

	/**
	 * The size of the step between the {@code from} and the {@code to}.
	 */
	int step() default 1;

	/**
	 * Whether the range is closed (inclusive of the {@link #to()}) or not.
	 */
	boolean closed() default false;

	/**
	 * Containing annotation of repeatable {@code IntRangeSource}.
	 */
	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@interface IntRangeSources {

		IntRangeSource[] value();

	}

}
