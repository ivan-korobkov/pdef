package io.pdef.rpc;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.Invocation;
import io.pdef.InvocationResult;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.test.TestInterface;
import io.pdef.test.TestInterface1;
import io.pdef.test.TestMessage;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class RpcServerTest {
	@Test
	public void testFunction() throws Exception {
		Function<Invocation, InvocationResult> invoker = new Function<Invocation, InvocationResult>() {
			@Override
			public InvocationResult apply(final Invocation input) {
				return InvocationResult.success(1, input.getMethod());
			}
		};
		Function<RpcRequest, RpcResponse> server = RpcServer.function(TestInterface.DESCRIPTOR, invoker);

		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder()
								.setMethod("sum")
								.setArgs(ImmutableMap.<String, Object>of("i0", 1))
								.build()))
				.build();

		RpcResponse response = server.apply(request);
		assertEquals(RpcResponse.builder()
				.setStatus(RpcResponseStatus.OK)
				.setResult(1)
				.build(), response);
	}

	@Test
	public void testFunction_internalServerError() throws Exception {
		Function<Invocation, InvocationResult> invoker = new Function<Invocation, InvocationResult>() {
			@Override
			public InvocationResult apply(final Invocation input) {
				throw new RuntimeException();
			}
		};
		Function<RpcRequest, RpcResponse> server = RpcServer.function(TestInterface.DESCRIPTOR, invoker);

		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder()
								.setMethod("sum")
								.setArgs(ImmutableMap.<String, Object>of("i0", 1))
								.build()))
				.build();

		RpcResponse response = server.apply(request);
		assertEquals(RpcResponse.builder()
				.setStatus(RpcResponseStatus.ERROR)
				.setResult(RpcError.builder()
						.setCode(RpcErrorCode.SERVER_ERROR)
						.setText("Internal server error")
						.build()
						.serialize())
				.build(), response);
	}

	@Test
	public void testParse_ok() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder().setMethod("interface0").build(),
						RpcCall.builder()
								.setMethod("hello")
								.setArgs(ImmutableMap.<String, Object>of("firstName", "John"))
								.build())
				).build();

		List<Invocation> result = RpcServer.parse(TestInterface.DESCRIPTOR, request).toList();
		assertEquals(2, result.size());

		Invocation invocation0 = result.get(0);
		Invocation invocation1 = result.get(1);
		assertEquals(TestInterface.DESCRIPTOR.getMethod("interface0"),
				invocation0.getMethod());
		assertEquals(TestInterface1.DESCRIPTOR.getMethod("hello"), invocation1.getMethod());
		Assert.assertArrayEquals(new Object[]{"John", null}, invocation1.getArgs());
	}

	@Test
	public void testParse_methodNotFound() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder()
								.setMethod("notFound")
								.build()))
				.build();
		try {
			RpcServer.parse(TestInterface.DESCRIPTOR, request);
			Assert.fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.CLIENT_ERROR, e.getCode());
			Assert.assertTrue(e.getText().contains("Method not found"));
		}
	}

	@Test
	public void testParse_wrongArgs() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(RpcCall.builder()
						.setMethod("hello")
						.setArgs(ImmutableMap.<String, Object>of("firstName", 1234))
						.build()))
				.build();

		try {
			RpcServer.parse(TestInterface1.DESCRIPTOR, request);
			Assert.fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.CLIENT_ERROR, e.getCode());
			Assert.assertTrue(e.getText().contains("Wrong method arguments"));
		}
	}

	@Test
	public void testParse_notRemoteMethod() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(RpcCall.builder()
						.setMethod("interface0")
						.build()))
				.build();
		try {
			RpcServer.parse(TestInterface.DESCRIPTOR, request);
			Assert.fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.CLIENT_ERROR, e.getCode());
			Assert.assertTrue(e.getText().contains("Not a remote method"));
		}
	}

	@Test
	public void testSerialize_ok() throws Exception {
		TestMessage msg = TestMessage.builder().setAString("hello, world").build();
		MethodDescriptor method = TestInterface.DESCRIPTOR.getMethod("message0");
		InvocationResult result = InvocationResult.success(msg, method);
		RpcResponse response = RpcServer.response(result);
		assertEquals(RpcResponse.builder()
				.setStatus(RpcResponseStatus.OK)
				.setResult(msg.serialize())
				.build(), response);
	}

	@Test
	public void testSerialize_rpcException() throws Exception {
		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.NETWORK_ERROR)
				.setText("Service unavailable")
				.build();

		RpcResponse response = RpcResponses.error(error);
		assertEquals(RpcResponse.builder()
				.setStatus(RpcResponseStatus.ERROR)
				.setResult(error.serialize())
				.build(), response);
	}

	@Test
	public void testRpcSerializeError_internalServerError() throws Exception {
		RuntimeException e = new RuntimeException();
		RpcResponse response = RpcResponses.error(e);
		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();
		assertEquals(RpcResponse.builder()
				.setStatus(RpcResponseStatus.ERROR)
				.setResult(error.serialize())
				.build(), response);
	}
}
