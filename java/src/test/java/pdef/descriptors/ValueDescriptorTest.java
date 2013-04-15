package pdef.descriptors;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ValueDescriptorTest {

	@Test
	public void testConstruct() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		ValueDescriptor descriptor = new ValueDescriptor(String.class, pool);
		assertEquals(String.class, descriptor.getJavaType());
	}
}
