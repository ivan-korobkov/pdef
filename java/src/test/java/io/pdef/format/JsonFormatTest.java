package io.pdef.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.Descriptors;
import io.pdef.test.inheritance.Base;
import io.pdef.test.inheritance.MultiLevelSubtype;
import io.pdef.test.inheritance.Subtype;
import io.pdef.test.inheritance.Subtype2;
import io.pdef.test.messages.TestDataTypes;
import io.pdef.test.messages.TestMessage;
import io.pdef.test.messages.TestEnum;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class JsonFormatTest {
	private JsonFormat format = JsonFormat.instance();

	private <T> void test(final DataDescriptor<T> descriptor, final T parsed,
			final String serialized) {
		assertEquals(serialized, format.serialize(parsed, descriptor, false));
		assertEquals(parsed, format.parse(serialized, descriptor));

		// Nulls.
		assertEquals("null", format.serialize(null, descriptor, false));
		assertNull(format.parse((String) null, descriptor));
		assertNull(format.parse("null", descriptor));
	}

	@Test
	public void testBoolean() throws Exception {
		test(Descriptors.bool, Boolean.TRUE, "true");
		test(Descriptors.bool, Boolean.FALSE, "false");
	}

	@Test
	public void testInt16() throws Exception {
		test(Descriptors.int16, (short) -16, "-16");
	}

	@Test
	public void testInt32() throws Exception {
		test(Descriptors.int32, -32, "-32");
	}

	@Test
	public void testInt64() throws Exception {
		test(Descriptors.int64, Long.MIN_VALUE, String.valueOf(Long.MIN_VALUE));
	}

	@Test
	public void testFloat() throws Exception {
		test(Descriptors.float0, -1.5f, "-1.5");
	}

	@Test
	public void testDouble() throws Exception {
		test(Descriptors.double0, -2.5, "-2.5");
	}

	@Test
	public void testString() throws Exception {
		test(Descriptors.string, "привет", "\"привет\"");
	}

	@Test
	public void testEnum() throws Exception {
		test(TestEnum.DESCRIPTOR, TestEnum.THREE, "\"three\"");
		assertEquals(TestEnum.TWO, format.parse("\"tWo\"", TestEnum.DESCRIPTOR));
	}

	@Test
	public void testMessage() throws Exception {
		test(TestDataTypes.DESCRIPTOR, createComplexMessage(), MESSAGE_JSON);
	}

	@Test
	public void testVoid() throws Exception {
		test(Descriptors.void0, null, "null");
	}

	@Test
	public void testPolymorphicMessage() throws Exception {
		Base base = new Base().setField("field");
		Subtype subtype = new Subtype().setField("field").setSubfield("subfield");
		Subtype2 subtype2 = new Subtype2().setField("field").setSubfield2("subfield2");
		MultiLevelSubtype msubtype = new MultiLevelSubtype()
				.setField("field")
				.setSubfield("subfield")
				.setMfield("mfield");

		test(Base.DESCRIPTOR, base, "{\"field\":\"field\"}");
		test(Base.DESCRIPTOR, subtype,
				"{\"type\":\"subtype\",\"field\":\"field\",\"subfield\":\"subfield\"}");
		test(Base.DESCRIPTOR, subtype2,
				"{\"type\":\"subtype2\",\"field\":\"field\",\"subfield2\":\"subfield2\"}");
		test(Base.DESCRIPTOR, msubtype,
				"{\"type\":\"multilevel_subtype\",\"field\":\"field\",\"subfield\":\"subfield\","
						+ "\"mfield\":\"mfield\"}");
	}

	private TestDataTypes createComplexMessage() {
		return new TestDataTypes()
				.setEnum0(TestEnum.THREE)
				.setBool0(true)
				.setShort0((short) 16)
				.setInt0(32)
				.setLong0(64L)
				.setFloat0(1.5f)
				.setDouble0(2.5d)
				.setString0("hello")
				.setList0(ImmutableList.of(1, 2))
				.setSet0(ImmutableSet.of(1, 2))
				.setMap0(ImmutableMap.<Integer, Float>of(1, 1.5f))
				.setMessage0(new TestMessage()
						.setBool0(true)
						.setShort0((short) 16)
						.setString0("hello"))
				.setPolymorphic(new MultiLevelSubtype()
						.setField("field")
						.setSubfield("subfield")
						.setMfield("mfield"));
	}

	private final String MESSAGE_JSON = "{"
			+ "\"string0\":\"hello\","
			+ "\"bool0\":true,"
			+ "\"short0\":16,"
			+ "\"int0\":32,"
			+ "\"long0\":64,"
			+ "\"float0\":1.5,"
			+ "\"double0\":2.5,"
			+ "\"list0\":[1,2],"
			+ "\"set0\":[1,2],"
			+ "\"map0\":{\"1\":1.5},"
			+ "\"enum0\":\"three\","
			+ "\"message0\":{\"string0\":\"hello\",\"bool0\":true,\"short0\":16},"
			+ "\"polymorphic\":{\"type\":\"multilevel_subtype\",\"field\":\"field\","
			+ "\"subfield\":\"subfield\",\"mfield\":\"mfield\"}}";
}
