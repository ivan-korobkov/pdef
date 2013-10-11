package io.pdef.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.meta.DataType;
import io.pdef.meta.MetaTypes;
import org.junit.Test;

public class NativeFormatTest {
	private NativeFormat format = NativeFormat.instance();

	private <T> void testPrimitive(final DataType<T> type, final String stringToParse,
			final T expected, final String expectedString) {
		assert format.parse(type, stringToParse).equals(expected);
		assert format.parse(type, expected).equals(expected);
		assert format.parse(type, null) == null;
		assert format.parse(type, stringToParse).getClass() == expected.getClass();
		assert format.parse(type, expected).getClass() == expected.getClass();

		assert format.serialize(type, expected).equals(expected);
		assert format.serialize(type, null) == null;

		assert format.serialize(type, null).equals("");
		assert format.serialize(type, expected).equals(expectedString);

		assert format.parse(type, "") == null;
		assert format.parse(type, expectedString).equals(expected);
	}

	@Test
	public void testBool() throws Exception {
		testPrimitive(MetaTypes.bool, "TRUE", true, "true");
		testPrimitive(MetaTypes.bool, "False", false, "false");
	}

	@Test
	public void testInt16() throws Exception {
		testPrimitive(MetaTypes.int16, "16", (short) 16, "16");
	}

	@Test
	public void testInt32() throws Exception {
		testPrimitive(MetaTypes.int32, "32", 32, "32");
	}

	@Test
	public void testInt64() throws Exception {
		testPrimitive(MetaTypes.int64, "64", 64L, "64");
	}

	@Test
	public void testFloat() throws Exception {
		testPrimitive(MetaTypes.float0, "1.5", 1.5f, "1.5");
	}

	@Test
	public void testDouble() throws Exception {
		testPrimitive(MetaTypes.double0, "2.5", 2.5d, "2.5");
	}

	private <T> void testData(final DataType<T> type, final T objectToParse,
			final T expected) {
		assert format.parse(type, objectToParse).equals(expected);
		assert format.parse(type, null) == null;
		assert format.serialize(type, null) == null;
		assert format.serialize(type, expected).equals(expected);
	}

	@Test
	public void testList() throws Exception {
		DataType type = MetaTypes.list(MetaTypes.int32);
		testData(type, ImmutableList.of("123", "456"), ImmutableList.of(123, 456));
	}

	@Test
	public void testSet() throws Exception {
		DataType type = MetaTypes.set(MetaTypes.int32);
		testData(type, ImmutableSet.of("123", "456"), ImmutableSet.of(123, 456));
	}

	@Test
	public void testMap() throws Exception {
		DataType type = MetaTypes.map(MetaTypes.int32, MetaTypes.int32);
		testData(type, ImmutableMap.of("123", "456"), ImmutableMap.of(123, 456));
	}
}
