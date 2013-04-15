package io.pdef.descriptors;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EnumDescriptorTest {

	@Test
	public void testValues() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		EnumDescriptor d = new EnumDescriptor(TestEnum.class, pool);
		assertEquals(
				ImmutableMap.of("ONE", TestEnum.ONE, "TWO", TestEnum.TWO, "THREE", TestEnum.THREE),
				d.getValues());
	}

	public static enum TestEnum {
		ONE, TWO, THREE
	}
}
