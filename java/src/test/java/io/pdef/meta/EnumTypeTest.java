package io.pdef.meta;

import com.google.common.collect.ImmutableList;
import io.pdef.test.messages.TestEnum;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class EnumTypeTest {
	private final EnumType<TestEnum> metaType = TestEnum.META_TYPE;

	@Test
	public void testGetValues() throws Exception {
		List<TestEnum> values = metaType.getValues();
		assertEquals(ImmutableList.<TestEnum>of(TestEnum.ONE, TestEnum.TWO, TestEnum.THREE), values);
	}

	@Test
	public void testCopy() throws Exception {
		TestEnum value = TestEnum.ONE;
		assert metaType.copy(value) == value;
	}
}
