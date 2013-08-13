package pdef.descriptors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class DescriptorsTest {
	@Test
	public void testBoolParse() throws Exception {
		boolean value = Descriptors.bool.parse("true");
		assertTrue(value);
	}

	@Test
	public void testBoolSerialize() throws Exception {
		String value = Descriptors.bool.serializeToString(true);
		assertEquals("true", value);
	}

	@Test
	public void testInt16Parse() throws Exception {
		short value = Descriptors.int16.parse("123");
		assertEquals((short) 123, value);
	}

	@Test
	public void testInt16Serialize() throws Exception {
		String value = Descriptors.int16.serializeToString((short) 123);
		assertEquals("123", value);
	}

	@Test
	public void testInt32Parse() throws Exception {
		int value = Descriptors.int32.parse("123");
		assertEquals(123, value);
	}

	@Test
	public void testInt32Serialize() throws Exception {
		String value = Descriptors.int32.serializeToString(123);
		assertEquals("123", value);
	}

	@Test
	public void testInt64Parse() throws Exception {
		long value = Descriptors.int64.parse("123");
		assertEquals(123L, value);
	}

	@Test
	public void testInt64Serialize() throws Exception {
		String value = Descriptors.int64.serializeToString(123L);
		assertEquals("123", value);
	}

	@Test
	public void testFloatParse() throws Exception {
		float value = Descriptors.float0.parse("1.5");
		assertEquals(1.5f, value, 0.0001);
	}

	@Test
	public void testFloatSerialize() throws Exception {
		String value = Descriptors.float0.serializeToString(1.5f);
		assertEquals("1.5", value);
	}

	@Test
	public void testDoubleParse() throws Exception {
		double value = Descriptors.double0.parse("0.5");
		assertEquals(0.5d, value, 0.0001);
	}

	@Test
	public void testDoubleSerialize() throws Exception {
		String value = Descriptors.double0.serializeToString(0.5d);
		assertEquals("0.5", value);
	}

	@Test
	public void testList_serialize() throws Exception {
		List<String> list = ImmutableList.of("hello", "world");
		Object result = Descriptors.list(Descriptors.string).serialize(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testList_serializeNull() throws Exception {
		Object result = Descriptors.list(Descriptors.string).serialize(null);
		assertNull(result);
	}

	@Test
	public void testList_parse() throws Exception {
		Object list = ImmutableList.of("hello", "world");
		List<String> result = Descriptors.list(Descriptors.string).parse(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testList_parseNull() throws Exception {
		List<String> result = Descriptors.list(Descriptors.string).parse(null);
		assertNull(result);
	}

	@Test
	public void testSet_serialize() throws Exception {
		Set<String> list = ImmutableSet.of("hello", "world");
		Object result = Descriptors.set(Descriptors.string).serialize(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testSet_serializeNull() throws Exception {
		Object result = Descriptors.set(Descriptors.string).serialize(null);
		assertNull(result);
	}

	@Test
	public void testSet_parse() throws Exception {
		Object list = ImmutableSet.of("hello", "world");
		Set<String> result = Descriptors.set(Descriptors.string).parse(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testSet_parseNull() throws Exception {
		Set<String> result = Descriptors.set(Descriptors.string).parse(null);
		assertNull(result);
	}
}
