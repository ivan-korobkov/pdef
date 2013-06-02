package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.Atomics;
import io.pdef.fluent.FluentFuture;
import io.pdef.fluent.FluentFutures;
import io.pdef.rpc.MethodCall;
import io.pdef.test.AsyncTestInterface;
import io.pdef.test.TestInterface;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PdefProxyTest {
	@Test
	public void testPerf() throws Exception {
		Pdef pdef = new Pdef();
		PdefProxy<TestInterface> client = new PdefProxy<TestInterface>(TestInterface.class, pdef,
				new Function<List<MethodCall>, Object>() {
					@Override
					public Object apply(final List<MethodCall> input) {
						return "hello";
					}
				});

		TestInterface app = client.proxy();

		for (int i = 0; i < 200 * 1000; i++) {
			app.interface0().hello("John", "Doe");
		}

		int n = 10 * 1000 * 1000;
		Stopwatch sw = new Stopwatch().start();
		for (int i = 0; i < n; i++) {
			app.interface0().hello("John", "Doe");
		}
		System.out.println("10M two-method chain calls in " + sw);
	}

	/** Should capture all method calls with arguments. */
	@Test
	public void testInvoke_calls() throws Exception {
		Pdef pdef = new Pdef();
		final AtomicReference<List<MethodCall>> ref = Atomics.newReference();
		PdefProxy<TestInterface> client = new PdefProxy<TestInterface>(TestInterface.class, pdef,
				new Function<List<MethodCall>, Object>() {
					@Override
					public Object apply(@Nullable final List<MethodCall> input) {
						ref.set(input);
						return "Hello";
					}
				});

		TestInterface app = client.proxy();
		app.interface0().hello("John", "Doe");

		assertEquals(ImmutableList.of(
				MethodCall.builder().setMethod("interface0")
						.setArgs(ImmutableMap.<String, Object>of()).build(),
				MethodCall.builder().setMethod("hello").setArgs(
						ImmutableMap.<String, Object>of("firstName", "John", "lastName", "Doe"))
						.build()),
				ref.get());
	}

	/** Should invoke a sync invocation chain handler. */
	@Test
	public void testInvoke() throws Exception {
		Pdef pdef = new Pdef();
		PdefProxy<TestInterface> client = new PdefProxy<TestInterface>(TestInterface.class, pdef,
				new Function<List<MethodCall>, Object>() {
					@Override
					public Object apply(@Nullable final List<MethodCall> input) {
						return 11;
					}
				});

		TestInterface app = client.proxy();
		Integer result = app.sum(1, 10);
		assertEquals(11, (int) result);
	}

	/** Should invoke an async invocation chain handler. */
	@Test
	public void testInvoke_future() throws Exception {
		Pdef pdef = new Pdef();
		PdefProxy<AsyncTestInterface> client = new PdefProxy<AsyncTestInterface>(
				AsyncTestInterface.class, pdef, new Function<List<MethodCall>, Object>() {
			@Nullable
			@Override
			public Object apply(@Nullable final List<MethodCall> input) {
				return FluentFutures.of(11);
			}
		});

		AsyncTestInterface app = client.proxy();
		FluentFuture<Integer> future = app.sum(1, 10);
		Integer result = future.get();
		assertEquals(11, (int) result);
	}
}
