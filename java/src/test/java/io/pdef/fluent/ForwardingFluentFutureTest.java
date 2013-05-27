package io.pdef.fluent;

import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ForwardingFluentFutureTest {

	@Test
	public void testThen() throws Exception {
		FluentFuture<Integer> future = FluentFutures.of(12)
				.then(new Function<Integer, Integer>() {
					@Override
					public Integer apply(final Integer input) {
						return input * input;
					}
				})
				.then(new Function<Integer, Integer>() {
					@Override
					public Integer apply(final Integer input) {
						return input * 2;
					}
				});

		int result = future.getUnchecked();
		assertEquals(288, result);
	}

	@Test
	public void testUse() throws Exception {
		Executor executor = mock(Executor.class);
		Function<Integer, Integer> func = new Function<Integer, Integer>() {
			@Override
			public Integer apply(@Nullable final Integer input) {
				return input;
			}
		};

		FluentFutures.of(10)
				.use(executor)
				.then(func);
		verify(executor).execute(any(Runnable.class));
	}

	@Test
	public void testAddCallback() throws Exception {
		final AtomicReference<Object> ref = new AtomicReference<Object>();

		FluentFutures.of(10).addCallback(new FutureCallback<Integer>() {
			@Override
			public void onSuccess(final Integer result) {
				ref.set(result);
			}

			@Override
			public void onFailure(final Throwable t) {
				ref.set(t);
			}
		});

		assertEquals(10, ref.get());
	}

	@Test
	public void testAddCallback_executor() throws Exception {
		Executor executor = mock(Executor.class);
		FluentFutures.of(10)
				.use(executor)
				.addCallback(new FutureCallback<Integer>() {
					@Override
					public void onSuccess(final Integer result) {
					}

					@Override
					public void onFailure(final Throwable t) {
					}
				});
		verify(executor).execute(any(Runnable.class));
	}
}
