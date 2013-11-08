package io.pdef.rest;

import com.google.common.collect.ImmutableList;
import io.pdef.test.inheritance.Base;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.TestForm;
import io.pdef.test.messages.TestMessage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RestIntegrationTest {
	Server server;
	Thread serverThread;
	String address;

	@Before
	public void setUp() throws Exception {
		server = new Server(0);

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/testapp");
		context.addServlet(TestServlet.class, "/");
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
		TestForm form = new TestForm()
				.setFormString("Привет, как дела?")
				.setFormList(ImmutableList.of(1, 2, 3))
				.setFormBool(true);

		TestInterface client = client();

		assertEquals(3, (int) client.testIndex(1, 2));
		assertEquals(7, (int) client.testRemote(3, 4));
		assertEquals(11, (int) client.testPost(5, 6));
		assertEquals(message, client.testMessage(message));
		assertEquals(form, client.testForm(form));
		client.testVoid(); // No Exception.
		assertEquals("Привет", client.testString("Привет"));
		assertEquals(7, (int) client.testInterface(1, 2).testIndex(3, 4));

		try {
			client.testExc();
			fail();
		} catch (TestException e) {
			TestException exc = new TestException().setText("Application exception");
			assertEquals(exc, e);
		}
	}

	private TestInterface client() {
		return new RestClient<TestInterface>(TestInterface.DESCRIPTOR, address).proxy();
	}

	public static class TestServlet extends HttpServlet {
		private final RestServlet<TestInterface> delegate = new RestHandler<TestInterface>(
				TestInterface.DESCRIPTOR, new TestService()).servlet();

		@Override
		protected void service(final HttpServletRequest req, final HttpServletResponse resp)
				throws ServletException, IOException {
			delegate.service(req, resp);
		}
	}

	public static class TestService implements TestInterface {

		@Override
		public Integer testIndex(final Integer arg0, final Integer arg1) {
			return arg0 + arg1;
		}

		@Override
		public Integer testPost(final Integer arg0, final Integer arg1) {
			return arg0 + arg1;
		}

		@Override
		public Integer testRemote(final Integer arg0, final Integer arg1) {
			return arg0 + arg1;
		}

		@Override
		public void testVoid() {}

		@Override
		public void testExc() {
			throw new TestException().setText("Application exception");
		}

		@Override
		public String testString(final String text) {
			return text;
		}

		@Override
		public TestForm testForm(final TestForm form) {
			return form.copy();
		}

		@Override
		public TestMessage testMessage(final TestMessage msg) {
			return msg.copy();
		}

		@Override
		public Base testPolymorphic(final Base msg) {
			return msg.copy();
		}

		@Override
		public Integer testCollections(final List<Integer> list0, final Set<Integer> set0,
				final Map<Integer, Integer> map0) {
			return list0.size() + set0.size() + map0.size();
		}

		@Override
		public TestInterface testInterface(final Integer arg0, final Integer arg1) {
			return this;
		}
	}
}
