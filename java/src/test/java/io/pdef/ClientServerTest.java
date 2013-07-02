package io.pdef;

import com.google.common.base.Supplier;
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

		Server<TestInterface1> server = new Server<TestInterface1>(TestInterface1.DESCRIPTOR,
				new Supplier<TestInterface1>() {
					@Override
					public TestInterface1 get() {
						return impl;
					}
				});

		TestInterface1 client = Client.create(TestInterface1.DESCRIPTOR, server).proxy();
		int q = 0;
		int n = 1000 * 1000;
		for (int i = 0; i < n; i++) {
			String result = client.hello("John", "Doe");
			if (result != null) q++;
		}
		
		long t0 = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			String result = client.hello("John", "Doe");
			if (result != null) q++;
		}
		long t1 = System.currentTimeMillis();
		System.out.println("1M calls in " + (t1 - t0) + "ms");
	}
}
