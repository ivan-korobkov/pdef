package io.pdef.rpc;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import com.google.common.util.concurrent.ListenableFuture;
import io.pdef.Client;
import io.pdef.Invocation;
import io.pdef.InvocationsHandler;
import io.pdef.descriptors.DefaultDescriptorPool;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.fixtures.App;
import io.pdef.fixtures.Calc;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class AsyncDispatcherTest {
	private DescriptorPool pool;

	@Before
	public void setUp() throws Exception {
		pool = new DefaultDescriptorPool();
	}

	@Test
	public void test() throws Exception {
		App app = new TestApp();
		final AsyncDispatcher<App> asyncDispatcher = new AsyncDispatcher<App>(app);
		Client<App> client = Client.of(App.class, pool,
				new InvocationsHandler() {
					@Override
					public Object handle(final List<Invocation> invocations) {
						return asyncDispatcher.apply(invocations);
					}
				});

		App proxy = client.proxy();
		int result = proxy.calc().sum(123, 456).get();
		assertEquals(579, result);

		String echo = proxy.echo("Hello, world").get();
		assertEquals("Hello, world", echo);
	}

	private static class TestApp implements App {
		@Override
		public Calc calc() {
			return new Calc() {
				@Override
				public ListenableFuture<Integer> sum(final int i0, final int i1) {
					return immediateFuture(i0 + i1);
				}
			};
		}

		@Override
		public ListenableFuture<String> echo(final String text) {
			return immediateFuture(text);
		}
	}
}
