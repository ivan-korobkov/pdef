package io.pdef.descriptors;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Map;

public class MapDescriptorTest {
	@Test
	public void test() throws Exception {
		MapDescriptor<Integer, String> descriptor = Descriptors
				.map(Descriptors.int32, Descriptors.string);

		assertEquals(Map.class, descriptor.getJavaClass());
		assertEquals(Descriptors.int32, descriptor.getKey());
		assertEquals(Descriptors.string, descriptor.getValue());
	}

	@Test
	public void testCopy() throws Exception {
		MapDescriptor<String, TestMessage> descriptor = Descriptors
				.map(Descriptors.string, TestMessage.DESCRIPTOR);
		Map<String, TestMessage> map0 = ImmutableMap.of("hello",
				new TestMessage().setString0("hello"));
		Map<String, TestMessage> map1 = descriptor.copy(map0);

		assertNull(descriptor.copy(null));
		assertEquals(map0, map1);
		assertTrue(map0.get("hello") != map1.get("hello"));
	}
}
