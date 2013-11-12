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
import static org.mockito.Mockito.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RpcTest {
	TestInterface service;
	String address;
	Server server;
	Thread serverThread;

	@Before
	public void setUp() throws Exception {
		service = mock(TestInterface.class);

		RpcHandler<TestInterface> handler = new RpcHandler<TestInterface>(
				TestInterface.DESCRIPTOR, service);
		HttpServlet servlet = new Servlet(handler.servlet());
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
				.setShort0((short) -123);

		when(service.method(1, 2)).thenReturn(3);
		when(service.query(3, 4)).thenReturn(7);
		when(service.post(5, 6)).thenReturn(11);
		when(service.string0("Привет")).thenReturn("Привет");
		when(service.message0(message.copy())).thenReturn(message.copy());
		when(service.interface0(1, 2)).thenReturn(service);
		doThrow(new TestException().setText("Application exception")).when(service).exc0();
		doThrow(new RuntimeException("Test server exception")).when(service).serverError();

		TestInterface client = new RpcClient<TestInterface>(
				TestInterface.DESCRIPTOR, address).proxy();
		assertEquals(3, (int) client.method(1, 2));
		assertEquals(7, (int) client.query(3, 4));
		assertEquals(11, (int) client.post(5, 6));
		assertEquals("Привет", client.string0("Привет"));
		assertEquals(message, client.message0(message));
		assertEquals(7, (int) client.interface0(1, 2).query(3, 4));

		client.void0(); // No Exception.

		try {
			client.exc0();
			fail();
		} catch (TestException e) {
			TestException exc = new TestException().setText("Application exception");
			assertEquals(exc, e);
		}

		try {
			client.serverError();
			fail();
		} catch (RpcException e) {
			// Ignore exception.
		}
	}

	static class Servlet extends HttpServlet {
		private final RpcServlet<TestInterface> delegate;

		Servlet(final RpcServlet<TestInterface> delegate) {
			this.delegate = delegate;
		}

		@Override
		protected void service(final HttpServletRequest req, final HttpServletResponse resp)
				throws ServletException, IOException {
			try {
				delegate.service(req, resp);
			} catch (RuntimeException e) {
				resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				// Ignore in tests.
			}
		}
	}
}
