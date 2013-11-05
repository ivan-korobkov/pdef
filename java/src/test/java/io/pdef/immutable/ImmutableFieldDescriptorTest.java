package io.pdef.immutable;

import io.pdef.Descriptors;
import io.pdef.FieldDescriptor;
import io.pdef.test.inheritance.Base;
import io.pdef.test.inheritance.PolymorphicType;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class ImmutableFieldDescriptorTest {
	@Test
	public void test() throws Exception {
		FieldDescriptor<TestMessage, Boolean> bool0 = TestMessage.BOOL0_FIELD;
		FieldDescriptor<TestMessage, String> string0 = TestMessage.STRING0_FIELD;

		assertEquals("bool0", bool0.getName());
		assertEquals(Descriptors.bool, bool0.getType());
		assertFalse(bool0.isDiscriminator());

		assertEquals("string0", string0.getName());
		assertEquals(Descriptors.string, string0.getType());
		assertFalse(string0.isDiscriminator());
	}

	@Test
	public void testDiscriminator() throws Exception {
		FieldDescriptor<Base, PolymorphicType> field = Base.TYPE_FIELD;

		assertEquals("type", field.getName());
		assertEquals(PolymorphicType.DESCRIPTOR, field.getType());
		assertTrue(field.isDiscriminator());
	}

	@Test
	public void testGetSet() throws Exception {
		TestMessage msg = new TestMessage();
		TestMessage.STRING0_FIELD.set(msg, "Hello, world");
		String s = TestMessage.STRING0_FIELD.get(msg);

		assertEquals("Hello, world", s);
	}

	@Test
	public void testCopy() throws Exception {
		TestMessage msg0 = new TestMessage().setShort0((short) -16);
		TestMessage msg1 = new TestMessage();

		TestMessage.SHORT0_FIELD.copy(msg0, msg1);
		assertEquals((short) -16, msg0.getShort0());
		assertEquals((short) -16, msg1.getShort0());
	}
}
