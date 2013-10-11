package io.pdef.format;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.descriptors.DataDescriptor;
import io.pdef.descriptors.Descriptors;
import org.junit.Test;

public class NativeFormatTest {
	private NativeFormat format = NativeFormat.instance();

	private <T> void testPrimitive(final DataDescriptor<T> descriptor, final String s, final T expected) {
		assert format.parse(descriptor, null) == null;
		assert format.parse(descriptor, s).equals(expected);
		assert format.parse(descriptor, expected).equals(expected);

		assert format.serialize(descriptor, null) == null;
		assert format.serialize(descriptor, expected).equals(expected);
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

	private <T> void testData(final DataDescriptor<T> descriptor, final T objectToParse,
			final T expected) {
		assert format.parse(descriptor, objectToParse).equals(expected);
		assert format.parse(descriptor, null) == null;
		assert format.serialize(descriptor, null) == null;
		assert format.serialize(descriptor, expected).equals(expected);
	}

	@Test
	public void testList() throws Exception {
		DataDescriptor descriptor = Descriptors.list(Descriptors.int32);
		testData(descriptor, ImmutableList.of("123", "456"), ImmutableList.of(123, 456));
	}

	@Test
	public void testSet() throws Exception {
		DataDescriptor descriptor = Descriptors.set(Descriptors.int32);
		testData(descriptor, ImmutableSet.of("123", "456"), ImmutableSet.of(123, 456));
	}

	@Test
	public void testMap() throws Exception {
		DataDescriptor descriptor = Descriptors.map(Descriptors.int32, Descriptors.int32);
		testData(descriptor, ImmutableMap.of("123", "456"), ImmutableMap.of(123, 456));
	}
}
