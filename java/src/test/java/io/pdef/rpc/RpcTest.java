package io.pdef.rpc;

import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.TestMessage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class RpcTest {
	Server server;
	Thread serverThread;
	String address;

	@Before
	public void setUp() throws Exception {
		RpcServlet<TestInterface> servlet = new RpcHandler<TestInterface>(
				TestInterface.DESCRIPTOR, new TestService()).servlet();

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/testapp");
		context.addServlet(new ServletHolder(servlet), "/");

		server = new Server(0);
		server.setHandler(context);
		server.start();
		address = "http://localhost:" + server.getConnectors()[0].getLocalPort() + "/testapp";

		serverThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					server.join();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		serverThread.start();
	}

	@After
	public void tearDown() throws Exception {
		server.stop();
		serverThread.interrupt();
	}

	@Test
	public void test() throws Exception {
		TestMessage message = new TestMessage()
				.setString0("Привет, как дела?")
				.setBool0(false)
				.setShort0((short) 123);

		TestInterface client = new RpcClient<TestInterface>(TestInterface.DESCRIPTOR, address)
				.proxy();
		assertEquals(3, (int) client.method(1, 2));
		assertEquals(7, (int) client.query(3, 4));
		assertEquals(11, (int) client.post(5, 6));
		assertEquals(message, client.message0(message));
		client.void0(); // No Exception.
		assertEquals("Привет", client.string0("Привет"));
		assertEquals(7, (int) client.interface0(1, 2).method(3, 4));

		try {
			client.exc0();
			fail();
		} catch (TestException e) {
			TestException exc = new TestException().setText("Application exception");
			assertEquals(exc, e);
		}
	}

	public static class TestService implements TestInterface {
		@Override
		public Integer method(final Integer arg0, final Integer arg1) {
			return arg0 + arg1;
		}

		@Override
		public Integer query(final Integer arg0, final Integer arg1) {
			return arg0 + arg1;
		}

		@Override
		public Integer post(final Integer arg0, final Integer arg1) {
			return arg0 + arg1;
		}

		@Override
		public void void0() {}

		@Override
		public void exc0() {
			throw new TestException().setText("Application exception");
		}

		@Override
		public String string0(final String text) {
			return text;
		}

		@Override
		public TestMessage message0(final TestMessage msg) {
			return msg.copy();
		}

		@Override
		public Integer collections(final List<Integer> list0, final Set<Integer> set0,
				final Map<Integer, Integer> map0) {
			return list0.size() + set0.size() + map0.size();
		}

		@Override
		public TestInterface interface0(final Integer arg0, final Integer arg1) {
			return this;
		}
	}
}
