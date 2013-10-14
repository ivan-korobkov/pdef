package io.pdef.format;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.pdef.descriptors.*;
import io.pdef.test.inheritance.Base;
import io.pdef.test.inheritance.MultiLevelSubtype;
import io.pdef.test.inheritance.PolymorphicType;
import io.pdef.test.messages.SimpleMessage;
import io.pdef.test.messages.TestEnum;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class NativeFormatTest {
	private NativeFormat format = NativeFormat.instance();

	private <T> void testPrimitive(final DataDescriptor<T> descriptor, final String s,
			final T expected) {
		assert format.parse(null, descriptor) == null;
		assert format.parse(s, descriptor).equals(expected);
		assert format.parse(expected, descriptor).equals(expected);

		assert format.serialize(null, descriptor) == null;
		assert format.serialize(expected, descriptor).equals(expected);
	}

	@Test
	public void testBool() throws Exception {
		testPrimitive(Descriptors.bool, "TRUE", true);
		testPrimitive(Descriptors.bool, "False", false);
	}

	@Test
	public void testInt16() throws Exception {
		testPrimitive(Descriptors.int16, "16", (short) 16);
	}

	@Test
	public void testInt32() throws Exception {
		testPrimitive(Descriptors.int32, "32", 32);
	}

	@Test
	public void testInt64() throws Exception {
		testPrimitive(Descriptors.int64, "64", 64L);
	}

	@Test
	public void testFloat() throws Exception {
		testPrimitive(Descriptors.float0, "1.5", 1.5f);
	}

	@Test
	public void testDouble() throws Exception {
		testPrimitive(Descriptors.double0, "2.5", 2.5d);
	}

	private <T> void testData(final DataDescriptor<T> descriptor, final Object serialized,
			final T parsed) {
		assert format.parse(serialized, descriptor).equals(parsed);
		assert format.parse(null, descriptor) == null;
		assert format.serialize(null, descriptor) == null;
		assert format.serialize(parsed, descriptor).equals(serialized);
	}

	@Test
	public void testList() throws Exception {
		List<Map<String, Object>> serialized = Lists.newArrayList();
		serialized.add(fixtureMap());

		List<SimpleMessage> parsed = Lists.newArrayList();
		parsed.add(fixtureMessage());

		ListDescriptor<SimpleMessage> descriptor = Descriptors.list(SimpleMessage.DESCRIPTOR);
		testData(descriptor, serialized, parsed);
	}

	@Test
	public void testSet() throws Exception {
		Set<Map<String, Object>> serialized = Sets.newHashSet();
		serialized.add(fixtureMap());

		Set<SimpleMessage> parsed = Sets.newHashSet();
		parsed.add(fixtureMessage());

		SetDescriptor<SimpleMessage> descriptor = Descriptors.set(SimpleMessage.DESCRIPTOR);
		testData(descriptor, serialized, parsed);
	}

	@Test
	public void testMap() throws Exception {
		Map<Integer, Map<String, Object>> serialized = Maps.newHashMap();
		serialized.put(123, fixtureMap());

		Map<Integer, SimpleMessage> parsed = Maps.newHashMap();
		parsed.put(123, fixtureMessage());

		MapDescriptor<Integer, SimpleMessage> descriptor = Descriptors
				.map(Descriptors.int32, SimpleMessage.DESCRIPTOR);
		testData(descriptor, serialized, parsed);
	}

	@Test
	public void testEnum() throws Exception {
		EnumDescriptor<TestEnum> descriptor = TestEnum.DESCRIPTOR;

		testData(descriptor, TestEnum.TWO, TestEnum.TWO);
		assertEquals(TestEnum.TWO, format.parse("two", descriptor));
	}

	@Test
	public void testMessage() throws Exception {
		Map<String, Object> serialized = fixtureMap();
		SimpleMessage parsed = fixtureMessage();

		testData(SimpleMessage.DESCRIPTOR, serialized, parsed);
	}

	@Test
	public void testPolymorphicMessage() throws Exception {
		MultiLevelSubtype parsed = new MultiLevelSubtype()
				.setField("field")
				.setSubfield("subfield")
				.setMfield("multi-level-field");
		ImmutableMap<String, Object> serialized = ImmutableMap.<String, Object>of(
				"type", PolymorphicType.MULTILEVEL_SUBTYPE,
				"field", "field",
				"subfield", "subfield",
				"mfield", "multi-level-field");

		testData(Base.DESCRIPTOR, serialized, parsed);
	}

	private SimpleMessage fixtureMessage() {
		return new SimpleMessage()
				.setABool(true)
				.setAnInt16((short) 123)
				.setAString("hello");
	}

	private Map<String, Object> fixtureMap() {
		return ImmutableMap.<String, Object>of(
				"aBool", true,
				"anInt16", (short) 123,
				"aString", "hello");
	}
}
