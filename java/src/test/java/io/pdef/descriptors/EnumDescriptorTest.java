package io.pdef.descriptors;

import com.google.common.collect.ImmutableList;
import io.pdef.test.messages.TestEnum;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;

public class EnumDescriptorTest {
	@Test
	public void testGetValues() throws Exception {
		List<TestEnum> values = TestEnum.DESCRIPTOR.getValues();
		assertEquals(ImmutableList.<TestEnum>of(TestEnum.ONE, TestEnum.TWO, TestEnum.THREE), values);
	}
}
