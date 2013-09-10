package pdef.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import pdef.Clients;
import pdef.Servers;
import pdef.test.interfaces.NextTestInterface;
import pdef.test.interfaces.TestException;
import pdef.test.interfaces.TestInterface;
import pdef.test.messages.SimpleForm;
import pdef.test.messages.SimpleMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

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
		SimpleMessage message = SimpleMessage.builder()
				.setAString("Привет, как дела?")
				.setABool(false)
				.setAnInt16((short) 123)
				.build();
		SimpleForm form = SimpleForm.builder()
				.setText("Привет, как дела?")
				.setNumbers(ImmutableList.of(1, 2, 3))
				.setFlag(true)
				.build();

		TestInterface client = client();

		assert client.indexMethod(1, 2) == 3;
		assert client.remoteMethod(10, 2) == 5;
		assert client.postMethod(ImmutableList.of(1, 2, 3), ImmutableMap.of(4, 5)).equals(
				ImmutableList.of(1, 2, 3, 4, 5));
		assert client.messageMethod(message).equals(message);
		assert client.formMethod(form).equals(form);
		assert client.voidMethod() == null;
		assert client.stringMethod("Привет").equals("Привет");
		assert client.interfaceMethod(1, 2).indexMethod().equals("chained call 1 2");

		try {
			client.excMethod();
			fail();
		} catch (TestException e) {
			TestException exc = TestException.builder()
					.setText("Application exception")
					.build();
			assert e.equals(exc);
		}
	}

	private TestInterface client() {
		return Clients.client(TestInterface.class, address);
	}

	public static class TestServlet extends HttpServlet {
		@Override
		protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
				throws ServletException, IOException {
			handle(req, resp);
		}

		@Override
		protected void doPost(final HttpServletRequest req, final HttpServletResponse resp)
				throws ServletException, IOException {
			handle(req, resp);
		}

		private void handle(final HttpServletRequest req, final HttpServletResponse resp)
				throws IOException {
			RestServer server = Servers.server(TestInterface.class, new TestService());
			server.handle(req, resp);
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
		public Void voidMethod() {
			return null;
		}

		@Override
		public Void excMethod() {
			throw TestException.builder()
					.setText("Application exception")
					.build();
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
					return SimpleMessage.builder()
							.setAString("hello")
							.setABool(true)
							.setAnInt16((short) 123)
							.build();
				}

				@Override
				public String stringMethod(final String text) {
					return text;
				}
			};
		}
	}
}
