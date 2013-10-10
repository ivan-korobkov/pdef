package io.pdef.types;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class TypesTest {
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
		testPrimitive(Types.bool, "TRUE", true, "true");
		testPrimitive(Types.bool, "False", false, "false");
	}

	@Test
	public void testInt16() throws Exception {
		testPrimitive(Types.int16, "16", (short) 16, "16");
	}

	@Test
	public void testInt32() throws Exception {
		testPrimitive(Types.int32, "32", 32, "32");
	}

	@Test
	public void testInt64() throws Exception {
		testPrimitive(Types.int64, "64", 64L, "64");
	}

	@Test
	public void testFloat() throws Exception {
		testPrimitive(Types.float0, "1.5", 1.5f, "1.5");
	}

	@Test
	public void testDouble() throws Exception {
		testPrimitive(Types.double0, "2.5", 2.5d, "2.5");
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
		DataType type = Types.list(Types.int32);
		testData(type, ImmutableList.of("123", "456"), ImmutableList.of(123, 456));
	}

	@Test
	public void testSet() throws Exception {
		DataType type = Types.set(Types.int32);
		testData(type, ImmutableSet.of("123", "456"), ImmutableSet.of(123, 456));
	}

	@Test
	public void testMap() throws Exception {
		DataType type = Types.map(Types.int32, Types.int32);
		testData(type, ImmutableMap.of("123", "456"), ImmutableMap.of(123, 456));
	}
}
