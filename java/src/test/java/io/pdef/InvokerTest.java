package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Atomics;
import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.descriptors.DefaultDescriptorPool;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.fixtures.App;
import io.pdef.fixtures.Calc;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;

public class InvokerTest {
	@Test
	public void testInvoke() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		InterfaceDescriptor app = (InterfaceDescriptor) pool.getDescriptor(App.class);

		AtomicReference<List<Invocation>> ref = Atomics.newReference();
		Invoker invoker = Invoker.of(app, new Capture(ref));

		App proxy = (App) invoker.toProxy();
		ListenableFuture<String> future = proxy.echo("Hello, world");
		assertNull(future);

		List<Invocation> invocations = ref.get();
		assertNotNull(invocations);
		assertEquals(1, invocations.size());

		Invocation invocation = invocations.get(0);
		assertEquals(app.getMethods().get("echo"), invocation.getMethod());
		assertEquals(ImmutableList.of("Hello, world"), invocation.getArgs());
	}

	@Test
	public void testInvokeChained() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		InterfaceDescriptor app = (InterfaceDescriptor) pool.getDescriptor(App.class);
		InterfaceDescriptor calc = (InterfaceDescriptor) pool.getDescriptor(Calc.class);

		AtomicReference<List<Invocation>> ref = Atomics.newReference();
		Invoker invoker = Invoker.of(app, new Capture(ref));

		App proxy = (App) invoker.toProxy();
		ListenableFuture<Integer> future = proxy.calc().sum(1, 2);
		assertNull(future);

		List<Invocation> invocations = ref.get();
		assertNotNull(invocations);
		assertEquals(2, invocations.size());

		Invocation i0 = invocations.get(0);
		Invocation i1 = invocations.get(1);
		assertEquals(app.getMethods().get("calc"), i0.getMethod());
		assertEquals(calc.getMethods().get("sum"), i1.getMethod());

		assertEquals(ImmutableList.of(), i0.getArgs());
		assertEquals(ImmutableList.of(1, 2), i1.getArgs());
	}

	private static class Capture implements InvocationListHandler {
		private final AtomicReference<List<Invocation>> ref;

		private Capture(final AtomicReference<List<Invocation>> ref) {
			this.ref = checkNotNull(ref);
		}

		@Override
		public Object handle(final List<Invocation> invocations) {
			ref.set(invocations);
			return null;
		}
	}
}
