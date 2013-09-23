package pdef.descriptors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

public class DescriptorsTest {
	private void testPrimitive(final PrimitiveDescriptor descriptor, final String stringToParse,
			final Object expected, final String expectedString) {
		assert descriptor.parseObject(stringToParse).equals(expected);
		assert descriptor.parseObject(stringToParse).getClass() == expected.getClass();
		assert descriptor.parseObject(expected).equals(expected);
		assert descriptor.parseObject(expected).getClass() == expected.getClass();
		assert descriptor.parseObject(null) == null;
		assert descriptor.toString(null) == null;
		assert descriptor.toString(expected).equals(expectedString);
	}

	@Test
	public void testBool() throws Exception {
		testPrimitive(Descriptors.bool, "TRUE", true, "true");
		testPrimitive(Descriptors.bool, "False", false, "false");
	}

	@Test
	public void testInt16() throws Exception {
		testPrimitive(Descriptors.int16, "16", (short) 16, "16");
	}

	@Test
	public void testInt32() throws Exception {
		testPrimitive(Descriptors.int32, "32", 32, "32");
	}

	@Test
	public void testInt64() throws Exception {
		testPrimitive(Descriptors.int64, "64", 64L, "64");
	}

	@Test
	public void testFloat() throws Exception {
		testPrimitive(Descriptors.float0, "1.5", 1.5f, "1.5");
	}

	@Test
	public void testDouble() throws Exception {
		testPrimitive(Descriptors.double0, "2.5", 2.5d, "2.5");
	}

	private void testData(final DataDescriptor descriptor, final Object objectToParse,
			final Object expected) {
		assert descriptor.parseObject(objectToParse).equals(expected);
		assert descriptor.parseObject(null) == null;
		assert descriptor.toObject(null) == null;
		assert descriptor.toObject(expected).equals(expected);
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
