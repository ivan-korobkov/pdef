package io.pdef.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.pdef.test.interfaces.NextTestInterface;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import io.pdef.test.messages.SimpleForm;
import io.pdef.test.messages.SimpleMessage;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
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
		SimpleMessage message = new SimpleMessage()
				.setAString("Привет, как дела?")
				.setABool(false)
				.setAnInt16((short) 123);
		SimpleForm form = new SimpleForm()
				.setText("Привет, как дела?")
				.setNumbers(ImmutableList.of(1, 2, 3))
				.setFlag(true);

		TestInterface client = client();

		assert client.indexMethod(1, 2) == 3;
		assert client.remoteMethod(10, 2) == 5;
		assert client.postMethod(ImmutableList.of(1, 2, 3), ImmutableMap.of(4, 5)).equals(
				ImmutableList.of(1, 2, 3, 4, 5));
		assert client.messageMethod(message).equals(message);
		assert client.formMethod(form).equals(form);
		client.voidMethod(); // No Exception.
		assert client.stringMethod("Привет").equals("Привет");
		assert client.interfaceMethod(1, 2).indexMethod().equals("chained call 1 2");

		try {
			client.excMethod();
			fail();
		} catch (TestException e) {
			TestException exc = new TestException()
					.setText("Application exception");
			assert e.equals(exc);
		}
	}

	private TestInterface client() {
		return RestClient.create(TestInterface.class, address);
	}

	public static class TestServlet extends HttpServlet {
		private final HttpServlet delegate = new RestServlet(
				RestServer.create(TestInterface.class, new TestService()));

		@Override
		protected void service(final HttpServletRequest req, final HttpServletResponse resp)
				throws ServletException, IOException {
			delegate.service(req, resp);
		}
	}

	public static class TestService implements TestInterface {
		@Override
		public Integer indexMethod(final Integer a, final Integer b) {
			return a + b;
		}

		@Override
		public Integer remoteMethod(final Integer a, final Integer b) {
			return a / b;
		}

		@Override
		public List<Integer> postMethod(final List<Integer> aList,
				final Map<Integer, Integer> aMap) {
			List<Integer> result = Lists.newArrayList();
			result.addAll(aList);
			result.addAll(aMap.keySet());
			result.addAll(aMap.values());
			return result;
		}

		@Override
		public SimpleMessage messageMethod(final SimpleMessage msg) {
			return msg;
		}

		@Override
		public SimpleForm formMethod(final SimpleForm form) {
			return form;
		}

		@Override
		public void voidMethod() {}

		@Override
		public void excMethod() {
			throw new TestException().setText("Application exception");
		}

		@Override
		public String stringMethod(final String text) {
			return text;
		}

		@Override
		public NextTestInterface interfaceMethod(final Integer a, final Integer b) {
			return new NextTestInterface() {
				@Override
				public String indexMethod() {
					return "chained call " + a + " " + b;
				}

				@Override
				public SimpleMessage remoteMethod() {
					return new SimpleMessage()
							.setAString("hello")
							.setABool(true)
							.setAnInt16((short) 123);
				}

				@Override
				public String stringMethod(final String text) {
					return text;
				}
			};
		}
	}
}
