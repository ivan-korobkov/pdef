package io.pdef;

import com.google.common.base.Function;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.*;
import io.pdef.test.TestInterface;
import io.pdef.test.TestInterface1;
import io.pdef.test.TestMessage;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerTest {
	@Test
	public void testApply() throws Exception {
		TestInterface impl = mock(TestInterface.class);
		when(impl.sum(1, 0)).thenReturn(1);
		Function<Request, Response> server = Server.create(TestInterface.DESCRIPTOR, impl);

		Request request = Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder()
								.setMethod("sum")
								.setArgs(ImmutableMap.<String, Object>of("i0", 1))
								.build()))
				.build();

		Response response = server.apply(request);
		assertEquals(Response.builder()
				.setStatus(ResponseStatus.OK)
				.setResult(1)
				.build(), response);
	}

	@Test
	public void testApply_internalServerError() throws Exception {
		TestInterface impl = mock(TestInterface.class);
		when(impl.sum(1, 0)).thenThrow(new RuntimeException());
		Function<Request, Response> server = Server.create(TestInterface.DESCRIPTOR, impl);

		Request request = Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder()
								.setMethod("sum")
								.setArgs(ImmutableMap.<String, Object>of("i0", 1))
								.build()))
				.build();

		Response response = server.apply(request);
		assertEquals(Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(RpcError.builder()
						.setCode(RpcErrorCode.SERVER_ERROR)
						.setText("Internal server error")
						.build()
						.serialize())
				.build(), response);
	}

	@Test
	public void testParseRequest_ok() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder().setMethod("interface0").build(),
						MethodCall.builder()
								.setMethod("hello")
								.setArgs(ImmutableMap.<String, Object>of("firstName", "John"))
								.build())
				).build();
		List<Invocation> result = Server.parseRequest(TestInterface.DESCRIPTOR, request).toList();
		assertEquals(2, result.size());

		Invocation invocation0 = result.get(0);
		Invocation invocation1 = result.get(1);
		assertEquals(TestInterface.DESCRIPTOR.getMethod("interface0"), invocation0.getMethod());
		assertEquals(TestInterface1.DESCRIPTOR.getMethod("hello"), invocation1.getMethod());
		assertArrayEquals(new Object[]{"John", null}, invocation1.getArgs());
	}

	@Test
	public void testParseRequest_methodNotFound() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder()
								.setMethod("notFound")
								.build()))
				.build();
		try {
			Server.parseRequest(TestInterface.DESCRIPTOR, request);
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.BAD_REQUEST, e.getCode());
			assertTrue(e.getText().contains("Method not found"));
		}
	}

	@Test
	public void testParseRequest_wrongArgs() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(MethodCall.builder()
						.setMethod("hello")
						.setArgs(ImmutableMap.<String, Object>of("firstName", 1234))
						.build()))
				.build();

		try {
			Server.parseRequest(TestInterface1.DESCRIPTOR, request);
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.BAD_REQUEST, e.getCode());
			assertTrue(e.getText().contains("Wrong method arguments"));
		}
	}

	@Test
	public void testParseRequest_notRemoteMethod() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(MethodCall.builder()
						.setMethod("interface0")
						.build()))
				.build();
		try {
			Server.parseRequest(TestInterface.DESCRIPTOR, request);
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.BAD_REQUEST, e.getCode());
			assertTrue(e.getText().contains("Not a remote method"));
		}
	}

	@Test
	public void testInvoke() throws Exception {
		Invocation invocation = TestInterface.DESCRIPTOR.getMethod("camelCase")
				.capture(Invocation.root(), "hello", "world");

		TestInterface impl = mock(TestInterface.class);
		when(impl.camelCase("hello", "world")).thenReturn("Hello, World");

		Object result = Server.invoke(Suppliers.ofInstance(impl), invocation);
		assertEquals("Hello, World", result);
	}

	@Test
	public void testInvoke_chained() throws Exception {
		TestInterface iface0 = mock(TestInterface.class);
		TestInterface1 iface1 = mock(TestInterface1.class);
		when(iface0.interface0()).thenReturn(iface1);
		when(iface1.hello("John", "Doe")).thenReturn("Hello, John Doe");

		Request request = Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder().setMethod("interface0").build(),
						MethodCall.builder()
								.setMethod("hello")
								.setArgs(ImmutableMap.<String, Object>of(
										"firstName", "John", "lastName", "Doe"))
								.build())
				).build();
		Invocation invocation = Server.parseRequest(TestInterface.DESCRIPTOR, request);
		Object result = Server.invoke(Suppliers.ofInstance(iface0), invocation);
		assertEquals("Hello, John Doe", result);
	}

	@Test
	public void testSerializeResult() throws Exception {
		TestMessage msg = TestMessage.builder().setAString("hello, world").build();
		Invocation invocation = TestInterface.DESCRIPTOR.getMethod("message0")
				.capture(Invocation.root(), msg);
		Response response = Server.serializeResult(invocation, msg);
		assertEquals(Response.builder()
				.setStatus(ResponseStatus.OK)
				.setResult(msg.serialize())
				.build(), response);
	}

	@Test
	public void testSerializeError_rpcException() throws Exception {
		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.SERVICE_UNAVAILABLE)
				.setText("Service unavailable")
				.build();
		Response response = Server.serializeError(error);
		assertEquals(Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(error.serialize())
				.build(), response);
	}

	@Test
	public void testSerializeError_internalServerError() throws Exception {
		RuntimeException e = new RuntimeException();
		Response response = Server.serializeError(e);
		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();
		assertEquals(Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(error.serialize())
				.build(), response);
	}
}
