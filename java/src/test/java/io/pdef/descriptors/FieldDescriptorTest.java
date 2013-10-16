package io.pdef.descriptors;

import io.pdef.test.inheritance.Base;
import io.pdef.test.inheritance.PolymorphicType;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FieldDescriptorTest {
	@Test
	public void test() throws Exception {
		FieldDescriptor<SimpleMessage, String> aString = SimpleMessage.ASTRING_FIELD;
		FieldDescriptor<SimpleMessage, Boolean> aBool = SimpleMessage.ABOOL_FIELD;

		assertEquals("aString", aString.getName());
		assertEquals(Descriptors.string, aString.getType());
		assertFalse(aString.isDiscriminator());

		assertEquals("aBool", aBool.getName());
		assertEquals(Descriptors.bool, aBool.getType());
		assertFalse(aBool.isDiscriminator());
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
		SimpleMessage msg = new SimpleMessage();
		SimpleMessage.ASTRING_FIELD.set(msg, "Hello, world");
		String s = SimpleMessage.ASTRING_FIELD.get(msg);

		assertEquals("Hello, world", s);
	}

	@Test
	public void testCopy() throws Exception {
		SimpleMessage msg0 = new SimpleMessage().setAnInt16((short) -16);
		SimpleMessage msg1 = new SimpleMessage();

		SimpleMessage.ANINT16_FIELD.copy(msg0, msg1);
		assertEquals((short) -16, msg0.getAnInt16());
		assertEquals((short) -16, msg1.getAnInt16());
	}
}
