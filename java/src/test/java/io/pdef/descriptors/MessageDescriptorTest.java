package io.pdef.descriptors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.pdef.test.inheritance.*;
import io.pdef.test.messages.ComplexMessage;
import io.pdef.test.messages.SimpleForm;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class MessageDescriptorTest {
	@Test
	public void test() throws Exception {
		MessageDescriptor<SimpleMessage> descriptor = SimpleMessage.DESCRIPTOR;

		assertEquals(SimpleMessage.class, descriptor.getJavaClass());
		assertNull(descriptor.getBase());
		assertNull(descriptor.getDiscriminator());
		assertNull(descriptor.getDiscriminatorValue());
		assertEquals(3, descriptor.getFields().size());
		assertEquals(0, descriptor.getSubtypes().size());
	}

	@Test
	public void test_form() throws Exception {
		MessageDescriptor<SimpleMessage> message = SimpleMessage.DESCRIPTOR;
		MessageDescriptor<SimpleForm> form = SimpleForm.DESCRIPTOR;

		assertFalse(message.isForm());
		assertTrue(form.isForm());
	}

	@Test
	public void test_nonpolymorphicInheritance() throws Exception {
		MessageDescriptor<SimpleMessage> base = SimpleMessage.DESCRIPTOR;
		MessageDescriptor<ComplexMessage> message = ComplexMessage.DESCRIPTOR;

		assertEquals(ComplexMessage.class, message.getJavaClass());
		assertEquals(base, message.getBase());

		List<FieldDescriptor<? super ComplexMessage, ?>> fields = Lists.newArrayList();
		fields.addAll(base.getFields());
		fields.addAll(message.getDeclaredFields());
		assertEquals(fields, message.getFields());
		assertEquals(0, message.getSubtypes().size());
	}

	@Test
	public void test__polymorphicInheritance() throws Exception {
		MessageDescriptor<Base> base = Base.DESCRIPTOR;
		MessageDescriptor<Subtype> subtype = Subtype.DESCRIPTOR;
		MessageDescriptor<Subtype2> subtype2 = Subtype2.DESCRIPTOR;
		MessageDescriptor<MultiLevelSubtype> msubtype = MultiLevelSubtype.DESCRIPTOR;
		FieldDescriptor<? super Base, ?> discriminator = base.getFieldMap().get("type");

		assertNull(base.getBase());
		assertEquals(base, subtype.getBase());
		assertEquals(base, subtype2.getBase());
		assertEquals(subtype, msubtype.getBase());

		assertEquals(discriminator, base.getDiscriminator());
		assertEquals(discriminator, subtype.getDiscriminator());
		assertEquals(discriminator, subtype2.getDiscriminator());
		assertEquals(discriminator, msubtype.getDiscriminator());

		assertNull(base.getDiscriminatorValue());
		assertEquals(PolymorphicType.SUBTYPE, subtype.getDiscriminatorValue());
		assertEquals(PolymorphicType.SUBTYPE2, subtype2.getDiscriminatorValue());
		assertEquals(PolymorphicType.MULTILEVEL_SUBTYPE, msubtype.getDiscriminatorValue());

		assertEquals(ImmutableSet.of(subtype, subtype2, msubtype), base.getSubtypes());
		assertEquals(ImmutableSet.of(msubtype), subtype.getSubtypes());
		assertTrue(subtype2.getSubtypes().isEmpty());
		assertTrue(msubtype.getSubtypes().isEmpty());

		assertEquals(base, base.findSubtypeOrThis(null));
		assertEquals(subtype, base.findSubtypeOrThis(PolymorphicType.SUBTYPE));
		assertEquals(subtype2, base.findSubtypeOrThis(PolymorphicType.SUBTYPE2));
		assertEquals(msubtype, base.findSubtypeOrThis(PolymorphicType.MULTILEVEL_SUBTYPE));
		assertEquals(msubtype, subtype.findSubtypeOrThis(PolymorphicType.MULTILEVEL_SUBTYPE));
	}
}
