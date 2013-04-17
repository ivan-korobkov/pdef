package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.pdef.descriptors.DefaultDescriptorPool;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.fixtures.App;
import io.pdef.fixtures.Calc;
import io.pdef.invocation.Invocation;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonMapInvocationParserTest {
	private DescriptorPool pool;
	private JsonMapInvocationParser parser;

	@Before
	public void setUp() throws Exception {
		pool = new DefaultDescriptorPool();
		parser = new JsonMapInvocationParser(new ObjectMapper(), pool);
	}

	@Test
	public void testParse() throws Exception {
		String s = "{\"calc\": [], \"sum\": [10, 11]}";

		List<Invocation> invocations = parser.parse(App.class, s);
		InterfaceDescriptor app = (InterfaceDescriptor) pool.getDescriptor(App.class);
		InterfaceDescriptor calc = (InterfaceDescriptor) pool.getDescriptor(Calc.class);

		assertEquals(app.getMethods().get("calc"), invocations.get(0).getMethod());
		assertEquals(calc.getMethods().get("sum"), invocations.get(1).getMethod());

		assertTrue(invocations.get(0).getArgs().isEmpty());
		assertEquals(ImmutableList.of(10, 11), invocations.get(1).getArgs());
	}
}
