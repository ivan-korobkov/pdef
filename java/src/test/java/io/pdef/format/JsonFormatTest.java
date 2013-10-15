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
import io.pdef.test.messages.ComplexMessage;
import io.pdef.test.messages.SimpleMessage;
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
		test(Descriptors.int64, -64L, "-64");
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
		test(ComplexMessage.DESCRIPTOR, createComplexMessage(), MESSAGE_JSON);
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

	private ComplexMessage createComplexMessage() {
		return new ComplexMessage()
				.setAnEnum(TestEnum.THREE)
				.setABool(true)
				.setAnInt16((short) 16)
				.setAnInt32(32)
				.setAnInt64(64L)
				.setAFloat(1.5f)
				.setADouble(2.5d)
				.setAString("hello")
				.setAList(ImmutableList.of(1, 2))
				.setASet(ImmutableSet.of(1, 2))
				.setAMap(ImmutableMap.<Integer, Float>of(1, 1.5f))
				.setAMessage(new SimpleMessage()
						.setABool(true)
						.setAnInt16((short) 16)
						.setAString("hello"))
				.setAPolymorphicMessage(new MultiLevelSubtype()
						.setField("field")
						.setSubfield("subfield")
						.setMfield("mfield"));
	}

	private final String MESSAGE_JSON = "{"
			+ "\"aString\":\"hello\","
			+ "\"aBool\":true,"
			+ "\"anInt16\":16,"
			+ "\"anInt32\":32,"
			+ "\"anInt64\":64,"
			+ "\"aFloat\":1.5,"
			+ "\"aDouble\":2.5,"
			+ "\"aList\":[1,2],"
			+ "\"aSet\":[1,2],"
			+ "\"aMap\":{\"1\":1.5},"
			+ "\"anEnum\":\"three\","
			+ "\"aMessage\":{\"aString\":\"hello\",\"aBool\":true,\"anInt16\":16},"
			+ "\"aPolymorphicMessage\":{\"type\":\"multilevel_subtype\",\"field\":\"field\","
			+ "\"subfield\":\"subfield\",\"mfield\":\"mfield\"}}";
}
