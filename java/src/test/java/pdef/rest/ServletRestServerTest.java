package pdef.rest;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.Ignore;
import org.junit.Test;
import pdef.Servers;
import pdef.test.interfaces.NextTestInterface;
import pdef.test.interfaces.TestException;
import pdef.test.interfaces.TestInterface;
import pdef.test.messages.SimpleMessage;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ServletRestServerTest {

	@Ignore
	@Test
	public void testHandle() throws Exception {
		Server server = new Server(8080);

		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/hello");
		context.addServlet(TestServlet.class, "/");

		HandlerCollection handlers = new HandlerCollection();
		handlers.setHandlers(new Handler[]{context, new DefaultHandler()});
		server.setHandler(handlers);

		server.start();
		server.join();
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
			ServletRestServer server = Servers
					.servletRestServer(TestInterface.class, new TestService());
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
			return a + b;
		}

		@Override
		public List<Integer> postMethod(final List<Integer> aList,
				final Map<Integer, Integer> aMap) {
			return Arrays.asList(1, 2, 3);
		}

		@Override
		public SimpleMessage formMethod(final SimpleMessage msg) {
			return msg;
		}

		@Override
		public Void voidMethod() {
			return null;
		}

		@Override
		public Void excMethod() {
			throw TestException.builder()
					.setText("Exception!")
					.build();
		}

		@Override
		public String stringMethod(final String text) {
			return text;
		}

		@Override
		public NextTestInterface interfaceMethod(final Integer a, final Integer b) {
			return null;
		}
	}
}
