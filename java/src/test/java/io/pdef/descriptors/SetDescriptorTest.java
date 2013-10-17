package io.pdef.descriptors;

import com.google.common.collect.ImmutableSet;
import io.pdef.test.messages.TestMessage;
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
		SetDescriptor<TestMessage> descriptor = Descriptors.set(TestMessage.DESCRIPTOR);
		Set<TestMessage> set0 = ImmutableSet.of(new TestMessage().setString0("hello"));
		Set<TestMessage> set1 = descriptor.copy(set0);


		assertNull(descriptor.copy(null));
		assertEquals(set0, set1);
		assertTrue(set0.toArray()[0] != set1.toArray()[0]);
	}
}
