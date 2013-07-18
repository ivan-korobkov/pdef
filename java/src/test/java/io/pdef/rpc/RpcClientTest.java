package io.pdef.rpc;

import com.google.common.base.Function;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Matchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RpcClientTest {
	@Mock private Function<RpcRequest, RpcResponse> sender;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProxy_parseResult() throws Exception {
		when(sender.apply(any(RpcRequest.class))).thenReturn(
				RpcResponses.ok(ImmutableMap.of("aString", "hello")));

		Function<Invocation, InvocationResult> protocol = RpcClient.function(sender);
		Invocation remote = mockInvocation();
		when(remote.getMethod().getResult()).thenReturn((Descriptor) TestMessage.DESCRIPTOR);

		TestMessage result = (TestMessage) protocol.apply(remote).getResult();
		assertEquals(TestMessage.builder()
				.setAnEnum(TestEnum.ONE)
				.setAString("hello").build(), result);
	}

	@Test
	public void testProxy_parseError() throws Exception {
		RpcError error = RpcError.builder()
				.setCode(RpcErrorCode.SERVER_ERROR)
				.setText("Internal server error")
				.build();

		when(sender.apply(any(RpcRequest.class))).thenReturn(RpcResponses.error(error));
		Function<Invocation, InvocationResult> protocol = RpcClient.function(sender);
		try {
			protocol.apply(mockInvocation());
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.SERVER_ERROR, e.getCode());
			assertEquals(error.getText(), e.getText());
		}
	}

	@Test
	public void testSerializeInvocation() throws Exception {
		Invocation invocation = TestInterface.DESCRIPTOR.getMethod("interface0")
				.capture(Invocation.root());
		Invocation invocation1 = TestInterface1.DESCRIPTOR.getMethod("hello")
				.capture(invocation, "John", "Doe");

		RpcRequest request = RpcClient.request(invocation1);
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

	private Invocation mockInvocation() {
		Invocation invocation = mock(Invocation.class);
		MethodDescriptor method = mock(MethodDescriptor.class);
		when(invocation.isRemote()).thenReturn(true);
		when(invocation.getMethod()).thenReturn(method);
		return invocation;
	}
}
