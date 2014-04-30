/*
 * Copyright: 2013 Pdef <http://pdef.io/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.pdef;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.TestInterface;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PdefServletTest {
	@Mock
	PdefServer<TestInterface> handler;
	PdefServlet<TestInterface> servlet;

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
		servlet = new PdefServlet<TestInterface>(handler);
	}

	@Test
	public void testHandle() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getServletPath()).thenReturn("/get");
		when(request.getRequestURI()).thenReturn("/get");
		when(handler.handle(any(PdefRequest.class)))
				.thenReturn(new PdefResponse<Object>().setData("hello, world"));

		HttpServletResponse response = mock(HttpServletResponse.class, RETURNS_DEEP_STUBS);
		servlet.service(request, response);
		verify(response).setStatus(200);
		verify(response).setContentType(PdefServlet.JSON_CONTENT_TYPE);
	}

	@Test
	public void testReadRequest() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getMethod()).thenReturn("GET");
		when(request.getContextPath()).thenReturn("/context");
		when(request.getServletPath()).thenReturn("/app");
		when(request.getPathInfo()).thenReturn("/method1/method2");
		when(request.getRequestURI()).thenReturn("/context/app/method1/method2");
		when(request.getParameterMap()).thenReturn(ImmutableMap.of(
				"key0", new String[]{"value0"},
				"key1", new String[]{"value1", "value11"}));

		PdefRequest req = servlet.readRequest(request);
		assertEquals("GET", req.getMethod());
		assertEquals("/method1/method2", req.getRelativePath());
		assertEquals(ImmutableMap.of("key0", "value0", "key1", "value1"), req.getQuery());
		assertEquals(ImmutableMap.of("key0", "value0", "key1", "value1"), req.getPost());
	}

	@Test
	public void testGetRelativePath() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getPathInfo()).thenReturn("/method/1/2");
		when(request.getContextPath()).thenReturn("/context");
		when(request.getServletPath()).thenReturn("/app");
		when(request.getRequestURI()).thenReturn("/context/app/method/1/2");

		String relativePath = servlet.getRelativePath(request);
		assertEquals("/method/1/2", relativePath);
	}

	@Test
	public void testGetRelativePath_noPathInfo() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getPathInfo()).thenReturn(null);
		when(request.getContextPath()).thenReturn("/context");
		when(request.getServletPath()).thenReturn("/method/1/2");
		when(request.getRequestURI()).thenReturn("/context/method/1/2");

		String relativePath = servlet.getRelativePath(request);
		assertEquals("/method/1/2", relativePath);
	}
}
