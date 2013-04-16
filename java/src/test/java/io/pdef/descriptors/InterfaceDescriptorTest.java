package io.pdef.descriptors;

import io.pdef.Interface;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InterfaceDescriptorTest {

	@Test
	public void testConstruct() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		InterfaceDescriptor descriptor = new InterfaceDescriptor(App.class, pool);
		assertEquals(App.class, descriptor.getJavaType());
	}

	@Test
	public void testLink() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		InterfaceDescriptor descriptor = new InterfaceDescriptor(App.class, pool);
		descriptor.link();

		Map<String, MethodDescriptor> methods = descriptor.getDeclaredMethods();
		assertEquals(1, methods.size());
		assertTrue(methods.containsKey("calc"));

		InterfaceDescriptor calc = (InterfaceDescriptor) pool.getDescriptor(Calc.class);
		assertTrue(calc ==  methods.get("calc").getResult());

		assertEquals(descriptor.getDeclaredMethods(), descriptor.getMethods());
	}

	@Test
	public void testBases() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		InterfaceDescriptor descriptor = new InterfaceDescriptor(ExtendedApp.class, pool);
		descriptor.link();

		Map<String, MethodDescriptor> declared = descriptor.getDeclaredMethods();
		assertEquals(1, declared.size());

		Map<String, MethodDescriptor> methods = descriptor.getMethods();
		assertEquals(2, methods.size());
	}

	@Test
	public void testOverriding() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		InterfaceDescriptor base = new InterfaceDescriptor(OverridingBase.class, pool);
		InterfaceDescriptor overriden = new InterfaceDescriptor(OverridenClass.class, pool);
		base.link();
		overriden.link();

		Descriptor app = pool.getDescriptor(App.class);
		Descriptor extended = pool.getDescriptor(ExtendedApp.class);

		assertEquals(app, base.getMethods().get("app").getResult());
		assertEquals(extended, overriden.getMethods().get("app").getResult());
	}

	private static interface App extends Interface {
		Calc calc();
	}

	private static interface Calc extends Interface {
		int sum(int i0, int i1);
	}

	private static interface ExtendedApp extends App {
		String echo(String text);
	}

	private static interface OverridingBase extends Interface {
		App app();
	}

	private static interface OverridenClass extends OverridingBase {
		ExtendedApp app();
	}
}
