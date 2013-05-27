package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Atomics;
import io.pdef.fluent.FluentFuture;
import io.pdef.fluent.FluentFutures;
import io.pdef.test.interfaces.App;
import io.pdef.test.interfaces.AsyncApp;
import io.pdef.test.interfaces.Calc;
import org.junit.Test;

import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class ClientProxyTest {
	@Test
	public void testPerf() throws Exception {
		Pdef pdef = new Pdef();
		ClientProxy<App> client = new ClientProxy<App>(App.class, pdef,
				new Function<List<Pdef.Invocation>, Object>() {
					@Override
					public Object apply(final List<Pdef.Invocation> input) {
						return 11;
					}
				});

		App app = client.proxy();

		int q = 0;
		for (int i = 0; i < 200 * 1000; i++) {
			q += app.calc().sum(1, 10);
		}

		int n = 10 * 1000 * 1000;
		Stopwatch sw = new Stopwatch().start();
		for (int i = 0; i < n; i++) {
			q += app.calc().sum(1, 10);
		}
		System.out.println(sw);
		System.out.println(q);
	}

	/** Should capture all method invocations with arguments. */
	@Test
	public void testInvoke_invocations() throws Exception {
		Pdef pdef = new Pdef();
		final AtomicReference<List<Pdef.Invocation>> ref = Atomics.newReference();
		ClientProxy<App> client = new ClientProxy<App>(App.class, pdef,
				new Function<List<Pdef.Invocation>, Object>() {
					@Override
					public Object apply(@Nullable final List<Pdef.Invocation> input) {
						ref.set(input);
						return 11;
					}
				});

		App app = client.proxy();
		app.calc().sum(1, 10);

		Pdef.InterfaceInfo appInfo = (Pdef.InterfaceInfo) pdef.get(App.class);
		Pdef.InterfaceInfo calcInfo = (Pdef.InterfaceInfo) pdef.get(Calc.class);
		assertEquals(ImmutableList.of(
				new Pdef.Invocation(appInfo.getMethods().get("calc"), null),
				new Pdef.Invocation(calcInfo.getMethods().get("sum"), new Object[] {1, 10})),
				ref.get());
	}

	/** Should invoke a sync invocation chain handler. */
	@Test
	public void testInvoke() throws Exception {
		Pdef pdef = new Pdef();
		ClientProxy<App> client = new ClientProxy<App>(App.class, pdef,
				new Function<List<Pdef.Invocation>, Object>() {
					@Override
					public Object apply(@Nullable final List<Pdef.Invocation> input) {
						return 11;
					}
				});

		App app = client.proxy();
		Integer result = app.calc().sum(1, 10);
		assertEquals(11, (int) result);
	}

	/** Should invoke an async invocation chain handler. */
	@Test
	public void testInvoke_future() throws Exception {
		Pdef pdef = new Pdef();
		ClientProxy<AsyncApp> client = new ClientProxy<AsyncApp>(AsyncApp.class, pdef,
				new Function<List<Pdef.Invocation>, Object>() {
					@Nullable
					@Override
					public Object apply(@Nullable final List<Pdef.Invocation> input) {
						return FluentFutures.of(11);
					}
				});

		AsyncApp app = client.proxy();
		FluentFuture<Integer> future = app.calc().sum(1, 10);
		Integer result = future.get();
		assertEquals(11, (int) result);
	}
}
