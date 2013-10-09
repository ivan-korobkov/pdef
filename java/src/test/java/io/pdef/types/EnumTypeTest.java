package io.pdef.types;

import com.google.common.collect.ImmutableList;
import io.pdef.test.messages.TestEnum;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class EnumTypeTest {
	private final EnumType<TestEnum> type = TestEnum.TYPE;

	@Test
	public void testGetValues() throws Exception {
		List<TestEnum> values = type.getValues();
		assertEquals(ImmutableList.<TestEnum>of(TestEnum.ONE, TestEnum.TWO, TestEnum.THREE), values);
	}

	@Test
	public void testParseObject() throws Exception {
		String s = "thrEE";
		TestEnum result = type.parseNative(s);
		assertEquals(TestEnum.THREE, result);
	}

	@Test
	public void testToObject() throws Exception {
		Object object = type.toNative(TestEnum.THREE);
		assertEquals("three", object);
	}

	@Test
	public void testParseString() throws Exception {
		String s = "three";
		TestEnum result = type.parseString(s);
		assertEquals(TestEnum.THREE, result);
	}

	@Test
	public void testToString() throws Exception {
		String s = type.toString(TestEnum.THREE);
		assertEquals("three", s);
	}
}
