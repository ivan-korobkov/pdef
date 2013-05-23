package io.pdef;

public class ClientProxyTest {
//	@Test
//	public void testInvoke() throws Exception {
//		Pdef pdef = new Pdef();
//		AtomicReference<List<Pdef.Invocation>> ref = Atomics.newReference();
//		ClientProxy<App> client = ClientProxy.of(App.class, pdef, new Capture(ref));
//
//		App proxy = client.proxy();
//		ListenableFuture<String> future = proxy.echoText("Hello, world");
//		assertNull(future);
//
//		List<Pdef.Invocation> invocations = ref.get();
//		assertNotNull(invocations);
//		assertEquals(1, invocations.size());
//
//		Pdef.Invocation invocation = invocations.get(0);
//		InterfaceDescriptor descriptor = (InterfaceDescriptor) pdef.getDescriptor(App.class);
//		assertEquals(descriptor.getMethods().get("echotext"), invocation.getMethod());
//		assertArrayEquals(new Object[]{"Hello, world"}, invocation.getArgs());
//	}
//
//	@Test
//	public void testInvokeChained() throws Exception {
//		DescriptorPool pdef = new DefaultDescriptorPool();
//		InterfaceDescriptor app = (InterfaceDescriptor) pdef.getDescriptor(App.class);
//		InterfaceDescriptor calc = (InterfaceDescriptor) pdef.getDescriptor(Calc.class);
//
//		AtomicReference<List<Pdef.Invocation>> ref = Atomics.newReference();
//		ClientProxy<App> client = ClientProxy.of(App.class, pdef, new Capture(ref));
//
//		App proxy = client.proxy();
//		ListenableFuture<Integer> future = proxy.calc().sum(1, 2);
//		assertNull(future);
//
//		List<Pdef.Invocation> invocations = ref.get();
//		assertNotNull(invocations);
//		assertEquals(2, invocations.size());
//
//		Pdef.Invocation i0 = invocations.get(0);
//		Pdef.Invocation i1 = invocations.get(1);
//		assertEquals(app.getMethods().get("calc"), i0.getMethod());
//		assertEquals(calc.getMethods().get("sum"), i1.getMethod());
//
//		assertArrayEquals(new Object[0], i0.getArgs());
//		assertArrayEquals(new Object[]{1, 2}, i1.getArgs());
//	}
//
//	private static class Capture implements InvocationsHandler {
//		private final AtomicReference<List<Pdef.Invocation>> ref;
//
//		private Capture(final AtomicReference<List<Pdef.Invocation>> ref) {
//			this.ref = checkNotNull(ref);
//		}
//
//		@Override
//		public Object handle(final List<Pdef.Invocation> invocations) {
//			ref.set(invocations);
//			return null;
//		}
//	}
}
