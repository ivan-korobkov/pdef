package io.pdef.descriptors;

import com.google.common.collect.ImmutableSet;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Set;

public class SetDescriptorTest {
	@Test
	public void test() throws Exception {
		SetDescriptor<Integer> descriptor = Descriptors.set(Descriptors.int32);

		assertEquals(Set.class, descriptor.getJavaClass());
		assertEquals(Descriptors.int32, descriptor.getElement());
	}

	@Test
	public void testCopy() throws Exception {
		SetDescriptor<SimpleMessage> descriptor = Descriptors.set(SimpleMessage.DESCRIPTOR);
		Set<SimpleMessage> set0 = ImmutableSet.of(new SimpleMessage().setAString("hello"));
		Set<SimpleMessage> set1 = descriptor.copy(set0);


		assertNull(descriptor.copy(null));
		assertEquals(set0, set1);
		assertTrue(set0.toArray()[0] != set1.toArray()[0]);
	}
}
