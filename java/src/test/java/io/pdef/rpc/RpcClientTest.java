package io.pdef.rpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.Invocation;
import io.pdef.InvocationResult;
import io.pdef.descriptors.Descriptor;
import io.pdef.descriptors.MethodDescriptor;
import io.pdef.test.TestEnum;
import io.pdef.test.TestInterface;
import io.pdef.test.TestInterface1;
import io.pdef.test.TestMessage;
import static org.junit.Assert.*;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RpcClientTest {
	@Test
	public void testWriteRequest() throws Exception {
		Invocation invocation = TestInterface.DESCRIPTOR.getMethod("interface0")
				.capture(Invocation.root());
		Invocation invocation1 = TestInterface1.DESCRIPTOR.getMethod("hello")
				.capture(invocation, "John", "Doe");

		RpcRequest request = RpcClient.writeRequest(invocation1);
		assertEquals(RpcRequest.builder()
				.setCalls(ImmutableList.of(
						RpcCall.builder()
								.setMethod("interface0")
								.setArgs(ImmutableMap.<String, Object>of())
								.build(),
						RpcCall.builder()
								.setMethod("hello")
								.setArgs(ImmutableMap.<String, Object>of(
										"firstName", "John", "lastName", "Doe"))
								.build()
				)).build(), request
		);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReadResponse_ok() throws Exception {
		MethodDescriptor method = mock(MethodDescriptor.class);
		when(method.getResult()).thenReturn((Descriptor) TestMessage.DESCRIPTOR);

		RpcResponse response = RpcResponse.builder()
				.setStatus(RpcResponseStatus.OK)
				.setResult(ImmutableMap.of("aString", "hello"))
				.build();

		InvocationResult result = RpcClient.readResponse(response, method);
		assertTrue(result.isSuccess());

		TestMessage expected = TestMessage.builder()
				.setAnEnum(TestEnum.ONE)
				.setAString("hello").build();
		assertEquals(expected, result.getResult());
	}

	@Test
	public void testReadResponse_error() throws Exception {
		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();

		MethodDescriptor method = mock(MethodDescriptor.class);
		RpcResponse response = RpcResponse.builder()
				.setStatus(RpcResponseStatus.ERROR)
				.setResult(error.serialize())
				.build();

		try {
			RpcClient.readResponse(response, method);
			fail();
		} catch (RpcError e) {
			assertEquals(error, e);
		}
	}
}
