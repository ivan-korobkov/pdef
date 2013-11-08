package io.pdef.descriptors;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import io.pdef.test.inheritance.*;
import io.pdef.test.messages.TestValues;
import io.pdef.test.messages.TestForm;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;

public class ImmutableMessageDescriptorTest {
	@Test
	public void test() throws Exception {
		MessageDescriptor<TestMessage> descriptor = TestMessage.DESCRIPTOR;

		assertEquals(TestMessage.class, descriptor.getJavaClass());
		assertNull(descriptor.getBase());
		assertNull(descriptor.getDiscriminator());
		assertNull(descriptor.getDiscriminatorValue());
		assertEquals(3, descriptor.getFields().size());
		assertEquals(0, descriptor.getSubtypes().size());
	}

	@Test
	public void test_form() throws Exception {
		MessageDescriptor<TestMessage> message = TestMessage.DESCRIPTOR;
		MessageDescriptor<TestForm> form = TestForm.DESCRIPTOR;

		assertFalse(message.isForm());
		assertTrue(form.isForm());
	}

	@Test
	public void test_nonpolymorphicInheritance() throws Exception {
		MessageDescriptor<TestMessage> base = TestMessage.DESCRIPTOR;
		MessageDescriptor<TestValues> message = TestValues.DESCRIPTOR;

		assertEquals(TestValues.class, message.getJavaClass());
		assertEquals(base, message.getBase());

		List<FieldDescriptor<? super TestValues, ?>> fields = Lists.newArrayList();
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
		FieldDescriptor<? super Base, ?> discriminator = base.getField("type");

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

		assertNull(base.getSubtype(null));
		assertEquals(subtype, base.getSubtype(PolymorphicType.SUBTYPE));
		assertEquals(subtype2, base.getSubtype(PolymorphicType.SUBTYPE2));
		assertEquals(msubtype, base.getSubtype(PolymorphicType.MULTILEVEL_SUBTYPE));
		assertEquals(msubtype, subtype.getSubtype(PolymorphicType.MULTILEVEL_SUBTYPE));
	}
}
