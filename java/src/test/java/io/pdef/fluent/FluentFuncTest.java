package io.pdef.fluent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class FluentFuncTest {
	@Test
	public void testOf() throws Exception {
		FluentFunc<String, String> func = FluentFunc.of(new Function<String, FluentFuture<String>>() {
			@Override
			public FluentFuture<String> apply(@Nullable final String input) {
				return FluentFutures.of(input);
			}
		});

		String result = func.apply("hello, world").getUnchecked();
		assertEquals("hello, world", result);
	}

	/** Should wrap an exception into a failed future. */
	@Test
	public void testApply_exception() throws Exception {
		FluentFunc<String, String> func = new FluentFunc<String, String>() {
			@Override
			protected FluentFuture<String> doApply(final String s) {
				throw new IllegalStateException();
			}
		};

		final AtomicBoolean called = new AtomicBoolean();
		func.apply("hello, world").addCallback(new FutureCallback<String>() {
			@Override
			public void onSuccess(final String result) {
				called.set(true);
				fail();
			}

			@Override
			public void onFailure(final Throwable t) {
				called.set(true);
				assertTrue(t instanceof IllegalStateException);
			}
		});

		assertTrue(called.get());
	}

	/** Should wrap a null result into an immediate future. */
	@Test
	public void testApply_null() throws Exception {
		FluentFunc<String, String> func = new FluentFunc<String, String>() {
			@Override
			protected FluentFuture<String> doApply(final String s) {
				return null;
			}
		};

		FluentFuture<String> future = func.apply("hello, world");
		assertNotNull(future);
		assertNull(future.getUnchecked());
	}

	@Test
	public void testWrap() throws Exception {
		FluentFunc<Integer, Integer> square = new FluentFunc<Integer, Integer>() {
			@Override
			protected FluentFuture<Integer> doApply(final Integer integer) {
				return FluentFutures.of(integer * integer);
			}
		};

		FluentFunc<String, String> func = square.wrap(
				new FluentWrapper<String, String, Integer, Integer>() {
					@Override
					public FluentFuture<String> apply(final String s,
							final FluentFunc<Integer, Integer> next) {
						Integer value = Integer.parseInt(s);
						return next.apply(value).then(new Function<Integer, String>() {
							@Override
							public String apply(final Integer input) {
								return Integer.toString(input);
							}
						});
					}
		});

		String result = func.apply("12").getUnchecked();
		assertEquals("144", result);
	}

	/** Should wrap an exceptioin in a wrapper into a failed future. */
	@Test
	public void testWrap_exception() throws Exception {
		FluentFunc<Integer, Integer> same = new FluentFunc<Integer, Integer>() {
			@Override
			protected FluentFuture<Integer> doApply(final Integer integer) {
				return FluentFutures.of(integer);
			}
		};

		FluentFunc<String, String> func = same.wrap(
				new FluentWrapper<String, String, Integer, Integer>() {
					@Override
					public FluentFuture<String> apply(final String s,
							final FluentFunc<Integer, Integer> next) {
						throw new UnsupportedOperationException();
					}
				});


		final AtomicBoolean called = new AtomicBoolean();
		func.apply("hello, world").addCallback(new FutureCallback<String>() {
			@Override
			public void onSuccess(final String result) {
				called.set(true);
				fail();
			}

			@Override
			public void onFailure(final Throwable t) {
				called.set(true);
				assertTrue(t instanceof UnsupportedOperationException);
			}
		});

		assertTrue(called.get());
	}
}
