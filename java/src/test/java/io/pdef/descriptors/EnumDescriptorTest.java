package io.pdef.descriptors;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import io.pdef.test.messages.TestEnum;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class EnumDescriptorTest {
	private final EnumDescriptor descriptor = TestEnum.descriptor();

	@Test
	public void testGetValues() throws Exception {
		List<Enum<?>> values = descriptor.getValues();
		assertEquals(ImmutableList.<Enum<?>>of(TestEnum.ONE, TestEnum.TWO, TestEnum.THREE), values);
	}

	@Test
	public void testParseObject() throws Exception {
		String s = "thrEE";
		TestEnum result = (TestEnum) descriptor.parseObject(s);
		assertEquals(TestEnum.THREE, result);
	}

	@Test
	public void testToObject() throws Exception {
		Object object = descriptor.toObject(TestEnum.THREE);
		assertEquals("three", object);
	}

	@Test
	public void testParseString() throws Exception {
		String s = "three";
		TestEnum result = (TestEnum) descriptor.parseString(s);
		assertEquals(TestEnum.THREE, result);
	}

	@Test
	public void testToString() throws Exception {
		String s = descriptor.toString(TestEnum.THREE);
		assertEquals("three", s);
	}
}
