package io.pdef;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.*;
import io.pdef.test.TestEnum;
import io.pdef.test.TestInterface;
import io.pdef.test.TestInterface1;
import io.pdef.test.TestMessage;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ClientRpcProtocolTest {
	@Mock private Function<Request, Response> sender;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProxy_parseResult() throws Exception {
		when(sender.apply(any(Request.class))).thenReturn(Response.builder()
				.setStatus(ResponseStatus.OK)
				.setResult(ImmutableMap.of("aString", "hello"))
				.build());

		ClientRpcProtocol protocol = new ClientRpcProtocol(sender);
		Invocation remote = mockInvocation();
		when(remote.getResult()).thenReturn((Descriptor) TestMessage.DESCRIPTOR);

		TestMessage result = (TestMessage) protocol.apply(remote);
		assertEquals(TestMessage.builder()
				.setAnEnum(TestEnum.ONE)
				.setAString("hello").build(), result);
	}

	@Test
	public void testProxy_parseError() throws Exception {
		when(sender.apply(any(Request.class))).thenReturn(Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(RpcError.builder()
						.setCode(RpcErrorCode.SERVER_ERROR)
						.setText("Internal server error")
						.build()
						.serialize())
				.build());

		ClientRpcProtocol protocol = new ClientRpcProtocol(sender);
		try {
			protocol.apply(mockInvocation());
			fail();
		} catch (RpcError e) {
			assertEquals(RpcErrorCode.SERVER_ERROR, e.getCode());
			assertEquals("Internal server error", e.getText());
		}
	}

	@Test
	public void testSerializeInvocation() throws Exception {
		Invocation invocation = TestInterface.DESCRIPTOR.getMethod("interface0")
				.capture(Invocation.root());
		Invocation invocation1 = TestInterface1.DESCRIPTOR.getMethod("hello")
				.capture(invocation, "John", "Doe");

		Request request = ClientRpcProtocol.createRequest(invocation1);
		assertEquals(Request.builder()
				.setCalls(ImmutableList.of(
						MethodCall.builder()
								.setMethod("interface0")
								.setArgs(ImmutableMap.<String, Object>of())
								.build(),
						MethodCall.builder()
								.setMethod("hello")
								.setArgs(ImmutableMap.<String, Object>of(
										"firstName", "John", "lastName", "Doe"))
								.build()
				)).build(), request
		);
	}

	private Invocation mockInvocation() {
		Invocation invocation = mock(Invocation.class);
		when(invocation.isRemote()).thenReturn(true);
		return invocation;
	}
}
