package io.pdef.descriptors;

import com.google.common.collect.ImmutableList;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class ListDescriptorTest {
	@Test
	public void test() throws Exception {
		ListDescriptor<Integer> descriptor = Descriptors.list(Descriptors.int32);

		assertEquals(List.class, descriptor.getJavaClass());
		assertEquals(Descriptors.int32, descriptor.getElement());
	}

	@Test
	public void testCopy() throws Exception {
		ListDescriptor<SimpleMessage> descriptor = Descriptors.list(SimpleMessage.DESCRIPTOR);
		List<SimpleMessage> list0 = ImmutableList.of(new SimpleMessage().setAString("hello"));
		List<SimpleMessage> list1 = descriptor.copy(list0);


		assertNull(descriptor.copy(null));
		assertEquals(list0, list1);
		assertTrue(list0.get(0) != list1.get(0));
	}
}
