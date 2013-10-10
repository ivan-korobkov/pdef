package io.pdef.meta;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class MetaTypesTest {
	private void testPrimitive(final DataType type, final String stringToParse,
			final Object expected, final String expectedString) {
		assert type.parseFromNative(stringToParse).equals(expected);
		assert type.parseFromNative(stringToParse).getClass() == expected.getClass();
		assert type.parseFromNative(expected).equals(expected);
		assert type.parseFromNative(expected).getClass() == expected.getClass();
		assert type.parseFromNative(null) == null;
		assert type.serializeToString(null) == null;
		assert type.serializeToString(expected).equals(expectedString);
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

	private void testData(final DataType type, final Object objectToParse,
			final Object expected) {
		assert type.parseFromNative(objectToParse).equals(expected);
		assert type.parseFromNative(null) == null;
		assert type.serializeToNative(null) == null;
		assert type.serializeToNative(expected).equals(expected);
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
