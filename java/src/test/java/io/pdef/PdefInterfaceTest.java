package io.pdef;

import com.google.common.collect.ImmutableSet;
import io.pdef.rpc.MethodCall;
import io.pdef.rpc.RpcException;
import io.pdef.rpc.RpcExceptionCode;
import io.pdef.test.InterfaceTree0;
import io.pdef.test.InterfaceTree1;
import io.pdef.test.InterfaceTree2;
import io.pdef.test.TestInterface;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class PdefInterfaceTest {

	private Pdef pdef;

	@Before
	public void setUp() throws Exception {
		pdef = new Pdef();
	}

	/** Should return a set of interface bases. */
	@Test
	public void testBases() throws Exception {
		PdefInterface d2 = new PdefInterface(InterfaceTree2.class, pdef);
		PdefInterface d1 = (PdefInterface) pdef.get(InterfaceTree1.class);
		PdefInterface d0 = (PdefInterface) pdef.get(InterfaceTree0.class);

		assertEquals(ImmutableSet.of(d0, d1), d2.getBases());
	}

	/** Should return a set of declared methods. */
	@Test
	public void testMethods_declared() throws Exception {
		PdefInterface d = new PdefInterface(TestInterface.class, pdef);
		assertEquals(ImmutableSet.of(
				"void0",
				"camelCase",
				"message0",
				"sum",
				"interface0"),
				d.getDeclaredMethodMap().keySet());
	}

	/** Should return a set of all inherited and declared methods. */
	@Test
	public void testMethods_declaredPlusInherited() throws Exception {
		PdefInterface d2 = new PdefInterface(InterfaceTree2.class, pdef);

		assertEquals(ImmutableSet.of("method0", "method1", "method2"), d2.getMethodMap().keySet());
	}

	/** Should invoke method on a given object. */
	@Test
	public void testInvoke_invokeMethod() throws Exception {
		PdefInterface d = new PdefInterface(TestInterface.class, pdef);

		TestInterface test = mock(TestInterface.class);
		d.invoke(test, MethodCall.builder().setMethod("void0").build());

		verify(test).void0();
	}

	/** Should raise an RpcException when a method is not found. */
	@Test
	public void testInvoke_methodNotFound() throws Exception {
		PdefInterface d = new PdefInterface(TestInterface.class, pdef);

		TestInterface test = mock(TestInterface.class);
		try {
			d.invoke(test, MethodCall.builder().setMethod("notPresentMethod").build());
			fail();
		} catch (RpcException e) {
			assertEquals(RpcExceptionCode.METHOD_NOT_FOUND, e.getCode());
		}
	}

	/** Should raise an RpcException when a call chain ends with an interface method. */
	@Test
	public void testInvoke_dataMethodCallRequired() throws Exception {
		Pdef pdef = new Pdef();
		PdefInterface d = new PdefInterface(TestInterface.class, pdef);

		TestInterface test = mock(TestInterface.class);
		try {
			d.invoke(test, MethodCall.builder().setMethod("interface0").build());
			fail();
		} catch (RpcException e) {
			assertEquals(RpcExceptionCode.DATA_METHOD_CALL_REQUIRED, e.getCode());
		}
	}

	/** Should raise an RpcException when invoking methods on data types. */
	@Test
	public void testInvoke_dataMethodReachedNoMoCalls() throws Exception {
		PdefInterface d = new PdefInterface(TestInterface.class, pdef);

		TestInterface test = mock(TestInterface.class);
		try {
			d.invoke(test, MethodCall.builder().setMethod("sum").build(),
					MethodCall.builder().setMethod("impossibleMethod").build());
			fail();
		} catch (RpcException e) {
			assertEquals(RpcExceptionCode.DATA_METHOD_REACHED_NO_MORE_CALLS, e.getCode());
		}
	}
}
