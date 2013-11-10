package io.pdef.descriptors;

import io.pdef.test.inheritance.Base;
import io.pdef.test.inheritance.PolymorphicType;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class FieldDescriptorTest {
	@Test
	public void test() throws Exception {
		FieldDescriptor<?, ?> bool0 = TestMessage.DESCRIPTOR.getField("bool0");
		FieldDescriptor<?, ?> string0 = TestMessage.DESCRIPTOR.getField("string0");

		assertEquals("bool0", bool0.getName());
		assertEquals(Descriptors.bool, bool0.getType());
		assertFalse(bool0.isDiscriminator());

		assertEquals("string0", string0.getName());
		assertEquals(Descriptors.string, string0.getType());
		assertFalse(string0.isDiscriminator());
	}

	@Test
	public void testDiscriminator() throws Exception {
		FieldDescriptor<?, ?> field = Base.DESCRIPTOR.getField("type");

		assertEquals("type", field.getName());
		assertEquals(PolymorphicType.DESCRIPTOR, field.getType());
		assertTrue(field.isDiscriminator());
	}

	@Test
	public void testGetSet() throws Exception {
		TestMessage msg = new TestMessage();
		FieldDescriptor<? super TestMessage, String> field = stringField();

		field.set(msg, "Hello, world");
		String s = field.get(msg);

		assertEquals("Hello, world", s);
	}

	@SuppressWarnings("unchecked")
	private FieldDescriptor<? super TestMessage, String> stringField() {
		return (FieldDescriptor<? super TestMessage, String>)
				TestMessage.DESCRIPTOR.getField("string0");
	}
}
