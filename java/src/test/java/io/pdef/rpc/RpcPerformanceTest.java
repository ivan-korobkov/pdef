package io.pdef.rpc;

import com.google.common.base.Function;
import io.pdef.Invocation;
import io.pdef.InvocationResult;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.test.TestInterface1;
import org.junit.Test;

public class RpcPerformanceTest {
	private final InterfaceDescriptor<TestInterface1> iface = TestInterface1.DESCRIPTOR;
	private final TestInterface1 service = new TestInterface1() {
		@Override
		public String hello(final String firstName, final String lastName) {
			return "Hello, " + firstName + " " + lastName;
		}
	};

	@Test
	public void testPerf() throws Exception {
		Function<RpcRequest, RpcResponse> server = RpcServer
				.filter(iface)
				.then(RpcInvoker.from(service));
		Function<Invocation, InvocationResult> handler = RpcClient
				.filter()
				.then(server);

		TestInterface1 client = iface.client(handler);

		int q = 0;
		int n = 1000 * 1000;
		for (int i = 0; i < n; i++) {
			String result = client.hello("John", "Doe");
			if (result != null) q++;
		}

		q = 0;
		long t0 = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			String result = client.hello("John", "Doe");
			if (result != null) q++;
		}
		long t1 = System.currentTimeMillis();
		System.out.println(q + " calls in " + (t1 - t0) + "ms");
	}
}
