package io.pdef.rest;

import com.google.common.collect.ImmutableMap;
import io.pdef.descriptors.Descriptors;
import io.pdef.test.interfaces.TestException;
import io.pdef.test.interfaces.TestInterface;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RestHandlerTest {
	TestInterface service;
	RestHandler<TestInterface> handler;

	@Before
	public void setUp() throws Exception {
		service = mock(TestInterface.class);
		handler = new RestHandler<TestInterface>(TestInterface.DESCRIPTOR, service);
	}

	@Test(expected = RestException.class)
	public void testHandle_restException() throws Exception {
		RestRequest request = new RestRequest().setPath("/hello/world/wrong/path");
		handler.handle(request);
	}

	@Test
	public void testHandle_ok() throws Exception {
		when(service.testIndex(1, 2)).thenReturn(3);
		RestRequest request = getRequest();

		RestResult<?> result = handler.handle(request);
		assertTrue(result.isOk());
		assertEquals(3, result.getData());
		assertEquals(Descriptors.int32, result.getDescriptor());
	}

	@Test
	public void testHandle_applicationException() throws Exception {
		TestException e = new TestException().setText("Hello, world");
		when(service.testIndex(1, 2)).thenThrow(e);
		RestRequest request = getRequest();

		RestResult<?> result = handler.handle(request);
		assertFalse(result.isOk());
		assertEquals(e, result.getData());
		assertEquals(TestException.DESCRIPTOR, result.getDescriptor());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHandle_unexpectedException() throws Exception {
		when(service.testIndex(1, 2)).thenThrow(new IllegalArgumentException());
		RestRequest request = getRequest();

		handler.handle(request);
	}

	private RestRequest getRequest() {
		return new RestRequest()
				.setPath("/")
				.setQuery(ImmutableMap.of("arg0", "1", "arg1", "2"));
	}
}
