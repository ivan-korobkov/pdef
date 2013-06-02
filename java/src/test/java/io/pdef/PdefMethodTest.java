package io.pdef;

import com.google.common.collect.ImmutableMap;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.RpcException;
import io.pdef.rpc.RpcExceptionCode;
import io.pdef.test.TestInterface;
import io.pdef.test.TestInterface1;
import io.pdef.test.TestMessage;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class PdefMethodTest {
	private Pdef pdef;

	@Before
	public void setUp() throws Exception {
		pdef = new Pdef();
	}

	@Test
	public void testConstructor_void() throws Exception {
		PdefMethod method = getTestMethod("void0");

		assertTrue(method.isVoid());
		assertTrue(method.getArgs().isEmpty());
	}

	@Test
	public void testConstructor_datatypeResult() throws Exception {
		PdefMethod method = getTestMethod("message0");
		PdefDescriptor result = method.getResult();

		assertTrue(method.isDatatype());
		assertTrue(result == pdef.get(TestMessage.class));
	}

	@Test
	public void testConstructor_interfaceResult() throws Exception {
		PdefMethod method = getTestMethod("interface0");
		PdefDescriptor result = method.getResult();

		assertTrue(method.isInterface());
		assertTrue(result == pdef.get(TestInterface1.class));
	}

	@Test
	public void testConstructor_lowercaseNameArgs() throws Exception {
		PdefMethod method = getTestMethod("camelcase");

		assertEquals("camelcase", method.getName());
		assertEquals(ImmutableMap.<String, PdefDatatype>of(
				"firstarg", pdef.get(String.class).asDatatype(),
				"secondarg", pdef.get(String.class).asDatatype()), method.getArgs());
	}

	@Test
	public void testCreateCall() throws Exception {
		PdefMethod method = getTestMethod("sum");
		MethodCall call = method.createCall(new Object[] {1, 2});

		assertEquals("sum", call.getMethod());
		assertEquals(ImmutableMap.<String, Object>of("i0", 1, "i1", 2), call.getArgs());
	}

	@Test
	public void testCreateCall_skipNulls() throws Exception {
		PdefMethod method = getTestMethod("sum");
		MethodCall call = method.createCall(new Object[]{null, null});

		assertEquals(ImmutableMap.<String, Object>of(), call.getArgs());
	}

	@Test
	public void testCreateCall_wrongNumberOfArgs() throws Exception {
		PdefMethod method = getTestMethod("sum");

		try {
			method.createCall(new Object[0]);
			fail();
		} catch (RpcException e) {
			assertEquals(RpcExceptionCode.WRONG_METHOD_ARGUMENTS, e.getCode());
		}
	}

	@Test
	public void testInvoke() throws Exception {
		PdefMethod method = getTestMethod("sum");

		TestInterface test = mock(TestInterface.class);
		method.invoke(test, 1, 2);

		verify(test).sum(1, 2);
	}

	@Test
	public void testInvoke_WrongNumberOfArgs() throws Exception {
		PdefMethod method = getTestMethod("sum");
		TestInterface test = mock(TestInterface.class);
		try {
			method.invoke(test, 1, 2, 3, 4);
			fail();
		} catch (RpcException e) {
			assertEquals(RpcExceptionCode.WRONG_METHOD_ARGUMENTS, e.getCode());
		}
	}

	@Test
	public void testInvoke_propagateException() throws Exception {
		PdefMethod method = getTestMethod("void0");
		TestInterface test = mock(TestInterface.class);
		NullPointerException exc = new NullPointerException();
		doThrow(exc).when(test).void0();

		try {
			method.invoke(test);
			fail();
		} catch (RuntimeException e) {
			assertTrue(e == exc);
		}
	}

	@Test
	public void testInvoke_replaceNullsWithDefaults() throws Exception {
		PdefMethod method = getTestMethod("sum");
		TestInterface test = mock(TestInterface.class);

		method.invoke(test, null, null);
		verify(test).sum(0, 0);
	}

	@Test
	public void testInvoke_map() throws Exception {
		PdefMethod method = getTestMethod("sum");
		TestInterface test = mock(TestInterface.class);

		method.invoke(test, ImmutableMap.<String, Object>of("i0", 1, "i1", 2));
		verify(test).sum(1, 2);
	}

	@Test
	public void testInvoke_mapReplaceNullsWithDefaults() throws Exception {
		PdefMethod method = getTestMethod("sum");
		TestInterface test = mock(TestInterface.class);

		method.invoke(test, ImmutableMap.<String, Object>of("i0", 1));
		verify(test).sum(1, 0);
	}

	@Test
	public void testInvoke_mapCaseInsensitive() throws Exception {
		PdefMethod method = getTestMethod("camelCase");
		TestInterface test = mock(TestInterface.class);

		method.invoke(test,
				ImmutableMap.<String, Object>of("FIRSTARG", "hello", "secondarg", "world"));
		verify(test).camelCase("hello", "world");
	}

	private PdefMethod getTestMethod(final String name) {
		PdefInterface descriptor = new PdefInterface(TestInterface.class, pdef);
		return descriptor.getMethod(name);
	}
}
