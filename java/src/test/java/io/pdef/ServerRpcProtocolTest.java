package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.*;
import io.pdef.test.TestInterface;
import io.pdef.test.TestInterface1;
import io.pdef.test.TestMessage;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ServerRpcProtocolTest {
	@Test
	public void testRpcHandler() throws Exception {
		Function<Invocation, Object> invocationHandler = new Function<Invocation, Object>() {
			@Override
			public Object apply(final Invocation input) {
				return 1;
			}
		};

		Function<Request, Response> server = ServerRpcProtocol
				.handler(TestInterface.DESCRIPTOR, invocationHandler);
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
	public void testRpcHandler_internalServerError() throws Exception {
		Function<Invocation, Object> invocationHandler = new Function<Invocation, Object>() {
			@Override
			public Object apply(final Invocation input) {
				throw new RuntimeException();
			}
		};
		Function<Request, Response> server = ServerRpcProtocol
				.handler(TestInterface.DESCRIPTOR, invocationHandler);

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
	public void testRpcParseRequest_ok() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder().setMethod("interface0").build(),
						MethodCall.builder()
								.setMethod("hello")
								.setArgs(ImmutableMap.<String, Object>of("firstName", "John"))
								.build())
				).build();
		List<Invocation> result = ServerRpcProtocol
				.parseRequest(TestInterface.DESCRIPTOR, request).toList();
		assertEquals(2, result.size());

		Invocation invocation0 = result.get(0);
		Invocation invocation1 = result.get(1);
		assertEquals(TestInterface.DESCRIPTOR.getMethod("interface0"), invocation0.getMethod());
		assertEquals(TestInterface1.DESCRIPTOR.getMethod("hello"), invocation1.getMethod());
		assertArrayEquals(new Object[]{"John", null}, invocation1.getArgs());
	}

	@Test
	public void testRpcParseRequest_methodNotFound() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder()
								.setMethod("notFound")
								.build()))
				.build();
		try {
			ServerRpcProtocol.parseRequest(TestInterface.DESCRIPTOR, request);
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.BAD_REQUEST, e.getCode());
			assertTrue(e.getText().contains("Method not found"));
		}
	}

	@Test
	public void testRpcParseRequest_wrongArgs() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(MethodCall.builder()
						.setMethod("hello")
						.setArgs(ImmutableMap.<String, Object>of("firstName", 1234))
						.build()))
				.build();

		try {
			ServerRpcProtocol.parseRequest(TestInterface1.DESCRIPTOR, request);
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.BAD_REQUEST, e.getCode());
			assertTrue(e.getText().contains("Wrong method arguments"));
		}
	}

	@Test
	public void testRpcParseRequest_notRemoteMethod() throws Exception {
		Request request = Request.builder()
				.setCalls(ImmutableList.of(MethodCall.builder()
						.setMethod("interface0")
						.build()))
				.build();
		try {
			ServerRpcProtocol.parseRequest(TestInterface.DESCRIPTOR, request);
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.BAD_REQUEST, e.getCode());
			assertTrue(e.getText().contains("Not a remote method"));
		}
	}

	@Test
	public void testRpcSerializeResult() throws Exception {
		TestMessage msg = TestMessage.builder().setAString("hello, world").build();
		Invocation invocation = TestInterface.DESCRIPTOR.getMethod("message0")
				.capture(Invocation.root(), msg);
		Response response = ServerRpcProtocol.serializeResult(invocation, msg);
		assertEquals(Response.builder()
				.setStatus(ResponseStatus.OK)
				.setResult(msg.serialize())
				.build(), response);
	}

	@Test
	public void testRpcSerializeError_rpcException() throws Exception {
		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.SERVICE_UNAVAILABLE)
				.setText("Service unavailable")
				.build();
		Response response = ServerRpcProtocol.serializeError(error);
		assertEquals(Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(error.serialize())
				.build(), response);
	}

	@Test
	public void testRpcSerializeError_internalServerError() throws Exception {
		RuntimeException e = new RuntimeException();
		Response response = ServerRpcProtocol.serializeError(e);
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
