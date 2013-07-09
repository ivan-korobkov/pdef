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
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ClientTest {
	@Mock Function<Request, Response> handler;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
	}

	@Test
	public void testApply() throws Exception {
		when(handler.apply(any(Request.class))).thenReturn(Response.builder()
				.setStatus(ResponseStatus.OK)
				.setResult("good, bye")
				.build());

		TestInterface client = Client.createFromRpcHandler(TestInterface.DESCRIPTOR, handler);
		String result = client.camelCase("hello", "world");
		assertEquals("good, bye", result);
	}

	@Test
	public void testApply_parseResult() throws Exception {
		when(handler.apply(any(Request.class))).thenReturn(Response.builder()
				.setStatus(ResponseStatus.OK)
				.setResult(ImmutableMap.of("aString", "hello"))
				.build());

		TestInterface client = Client.createFromRpcHandler(TestInterface.DESCRIPTOR, handler);
		TestMessage msg = client.message0(TestMessage.instance());
		assertEquals(TestMessage.builder()
				.setAnEnum(TestEnum.ONE)
				.setAString("hello").build(), msg);
	}

	@Test
	public void testApply_parseError() throws Exception {
		when(handler.apply(any(Request.class))).thenReturn(Response.builder()
				.setStatus(ResponseStatus.ERROR)
				.setResult(RpcError.builder()
						.setCode(RpcErrorCode.SERVER_ERROR)
						.setText("Internal server error")
						.build()
						.serialize())
				.build());

		TestInterface client = Client.createFromRpcHandler(TestInterface.DESCRIPTOR, handler);
		try {
			client.void0();
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

		Request request = Client.serializeInvocation(invocation1);
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
}
