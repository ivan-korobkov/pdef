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
	public void testPipeline() throws Exception {
		Function<Invocation, InvocationResult> invoker = new Function<Invocation, InvocationResult>() {
			@Override
			public InvocationResult apply(final Invocation input) {
				return InvocationResult.success(1, input.getMethod());
			}
		};
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder()
								.setMethod("sum")
								.setArgs(ImmutableMap.<String, Object>of("i0", 1))
								.build()))
				.build();

		Function<RpcRequest, RpcResponse> server = RpcServer
				.requestReader(TestInterface.DESCRIPTOR)
				.then(invoker)
				.then(RpcServer.responseWriter());
		RpcResponse response = server.apply(request);
		RpcResponse expected = RpcResponse.builder()
				.setStatus(RpcResponseStatus.OK)
				.setResult(1)
				.build();
		assertEquals(expected, response);
	}

	@Test
	public void testPipeline_error() throws Exception {
		Function<Invocation, InvocationResult> invoker = new Function<Invocation, InvocationResult>() {
			@Override
			public InvocationResult apply(final Invocation input) {
				throw new RuntimeException();
			}
		};
		Function<RpcRequest, RpcResponse> server = RpcServer
				.requestReader(TestInterface.DESCRIPTOR)
				.then(invoker)
				.then(RpcServer.responseWriter())
				.onError(RpcServer.errorHandler());

		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder()
								.setMethod("sum")
								.setArgs(ImmutableMap.<String, Object>of("i0", 1))
								.build()))
				.build();
		RpcResponse response = server.apply(request);
		RpcResponse expected = RpcResponse.builder()
				.setStatus(RpcResponseStatus.ERROR)
				.setResult(RpcError.builder()
						.setCode(RpcErrorCode.SERVER_ERROR)
						.setText("Internal server error")
						.build()
						.serialize())
				.build();
		assertEquals(expected, response);
	}

	@Test
	public void testReadRequest_ok() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder().setMethod("interface0").build(),
						RpcCall.builder()
								.setMethod("hello")
								.setArgs(ImmutableMap.<String, Object>of("firstName", "John"))
								.build())
				).build();

		List<Invocation> result = RpcServer.readRequest(request, TestInterface.DESCRIPTOR).toList();
		assertEquals(2, result.size());

		Invocation invocation0 = result.get(0);
		Invocation invocation1 = result.get(1);
		assertEquals(TestInterface.DESCRIPTOR.getMethod("interface0"), invocation0.getMethod());
		assertEquals(TestInterface1.DESCRIPTOR.getMethod("hello"), invocation1.getMethod());
		Assert.assertArrayEquals(new Object[]{"John", null}, invocation1.getArgs());
	}

	@Test
	public void testReadRequest_methodNotFound() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder()
								.setMethod("notFound")
								.build()))
				.build();
		try {
			RpcServer.readRequest(request, TestInterface.DESCRIPTOR);
			Assert.fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.CLIENT_ERROR, e.getCode());
			Assert.assertTrue(e.getText().contains("Method not found"));
		}
	}

	@Test
	public void testReadRequest_wrongArgs() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(RpcCall.builder()
						.setMethod("hello")
						.setArgs(ImmutableMap.<String, Object>of("firstName", 1234))
						.build()))
				.build();

		try {
			RpcServer.readRequest(request, TestInterface1.DESCRIPTOR);
			Assert.fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.CLIENT_ERROR, e.getCode());
			Assert.assertTrue(e.getText().contains("Wrong method arguments"));
		}
	}

	@Test
	public void testReadRequest_notRemoteMethod() throws Exception {
		RpcRequest request = RpcRequest.builder()
				.setCalls(ImmutableList.of(RpcCall.builder()
						.setMethod("interface0")
						.build()))
				.build();
		try {
			RpcServer.readRequest(request, TestInterface.DESCRIPTOR);
			Assert.fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.CLIENT_ERROR, e.getCode());
			Assert.assertTrue(e.getText().contains("Not a remote method"));
		}
	}

	@Test
	public void testWriteResponse_ok() throws Exception {
		TestMessage msg = TestMessage.builder().setAString("hello, world").build();
		MethodDescriptor method = TestInterface.DESCRIPTOR.getMethod("message0");
		InvocationResult result = InvocationResult.success(msg, method);

		RpcResponse response = RpcServer.writeResponse(result);
		assertEquals(RpcResponse.builder()
				.setStatus(RpcResponseStatus.OK)
				.setResult(msg.serialize())
				.build(), response);
	}

	@Test
	public void testWriteError() throws Exception {
		RuntimeException e = new RuntimeException();
		RpcResponse response = RpcServer.writeError(e);

		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();
		RpcResponse expected = RpcResponses.error(error);
		assertEquals(expected, response);
	}
}
