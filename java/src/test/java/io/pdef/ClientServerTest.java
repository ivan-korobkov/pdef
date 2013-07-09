package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import io.pdef.rpc.Request;
import io.pdef.rpc.Response;
import io.pdef.test.TestInterface1;
import org.junit.Test;

public class ClientServerTest {
	@Test
	public void test() throws Exception {
		final TestInterface1 impl = new TestInterface1() {
			@Override
			public String hello(final String firstName, final String lastName) {
				return "Hello, ";
			}
		};

		Function<Request, Response> server = Server
				.create(TestInterface1.DESCRIPTOR, new Supplier<TestInterface1>() {
					@Override
					public TestInterface1 get() {
						return impl;
					}
				});

		TestInterface1 client = Client.createFromRpcHandler(TestInterface1.DESCRIPTOR, server);
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
