package io.pdef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class PdefTest {
	/** Should create and return a descriptor for a java type. */
	@Test
	public void testGet() throws Exception {
		Pdef pdef = new Pdef();
		PdefDescriptor d0 = pdef.get(int.class);
		PdefDescriptor d1 = pdef.get(int.class);

		assertTrue(d0 == d1);
		assertTrue(d0.getJavaType() == int.class);
	}

	/** Should cache a proxy class and return a proxy instance. */
	@Test
	public void testProxy() throws Exception {
		final AtomicBoolean called = new AtomicBoolean(false);
		Pdef pdef = new Pdef();
		Iterable proxy = pdef.proxy(Iterable.class, new InvocationHandler() {
			@Override
			public Object invoke(final Object proxy, final Method method, final Object[] args)
					throws Throwable {
				called.set(true);
				return null;
			}
		});

		proxy.iterator();
		assertTrue(called.get());
	}

	/** Should add a new descriptor to this pdef pool. */
	@Test
	public void testAdd() throws Exception {
		Pdef pdef = new Pdef();
		PdefDescriptor d0 = new PdefPrimitive(PdefType.INT32, int.class, 0, pdef);
		PdefDescriptor d1 = pdef.get(int.class);

		assertTrue(d0 == d1);
	}

	public Map<String, Integer> mapField;

	@Test
	public void testActualTypeArgs() throws Exception {
		Type type = PdefTest.class.getField("mapField").getGenericType();
		Type arg0 = Pdef.actualTypeArgs(type)[0];
		Type arg1 = Pdef.actualTypeArgs(type)[1];

		assertEquals(String.class, arg0);
		assertEquals(Integer.class, arg1);
	}
}
