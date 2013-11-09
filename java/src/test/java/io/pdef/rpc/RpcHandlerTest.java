package io.pdef.rpc;

import io.pdef.descriptors.Descriptors;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RpcHandlerTest {
	TestInterface service;
	RpcHandler<TestInterface> handler;

	@Before
	public void setUp() throws Exception {
		service = mock(TestInterface.class);
		handler = new RpcHandler<TestInterface>(TestInterface.DESCRIPTOR, service);
	}

	@Test(expected = RpcException.class)
	public void testHandle_rpcException() throws Exception {
		RpcRequest request = new RpcRequest().setPath("/hello/world/wrong/path");
		handler.handle(request);
	}

	@Test
	public void testHandle_ok() throws Exception {
		when(service.method(1, 2)).thenReturn(3);
		RpcRequest request = getRequest();

		RpcResult<?> result = handler.handle(request);
		assertTrue(result.isOk());
		assertEquals(3, result.getData());
		assertEquals(Descriptors.int32, result.getDescriptor());
	}

	@Test
	public void testHandle_applicationException() throws Exception {
		TestException e = new TestException().setText("Hello, world");
		when(service.method(1, 2)).thenThrow(e);
		RpcRequest request = getRequest();

		RpcResult<?> result = handler.handle(request);
		assertFalse(result.isOk());
		assertEquals(e, result.getData());
		assertEquals(TestException.DESCRIPTOR, result.getDescriptor());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHandle_unexpectedException() throws Exception {
		when(service.method(1, 2)).thenThrow(new IllegalArgumentException());
		RpcRequest request = getRequest();

		handler.handle(request);
	}

	private RpcRequest getRequest() {
		return new RpcRequest().setPath("/method/1/2");
	}
}
