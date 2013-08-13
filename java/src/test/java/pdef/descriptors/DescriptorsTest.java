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
		Boolean value = (Boolean) Descriptors.bool.parseObject("true");
		assertTrue(value);
	}

	@Test
	public void testBoolSerialize() throws Exception {
		String value = Descriptors.bool.toString(true);
		assertEquals("true", value);
	}

	@Test
	public void testInt16Parse() throws Exception {
		Short value = (Short) Descriptors.int16.parseObject("123");
		assertEquals((short) 123, (short) value);
	}

	@Test
	public void testInt16Serialize() throws Exception {
		String value = Descriptors.int16.toString((short) 123);
		assertEquals("123", value);
	}

	@Test
	public void testInt32Parse() throws Exception {
		Integer value = (Integer) Descriptors.int32.parseObject("123");
		assertEquals(123, (int) value);
	}

	@Test
	public void testInt32Serialize() throws Exception {
		String value = Descriptors.int32.toString(123);
		assertEquals("123", value);
	}

	@Test
	public void testInt64Parse() throws Exception {
		Long value = (Long) Descriptors.int64.parseObject("123");
		assertEquals(123L, (long) value);
	}

	@Test
	public void testInt64Serialize() throws Exception {
		String value = Descriptors.int64.toString(123L);
		assertEquals("123", value);
	}

	@Test
	public void testFloatParse() throws Exception {
		Float value = (Float) Descriptors.float0.parseObject("1.5");
		assertEquals(1.5f, value, 0.0001);
	}

	@Test
	public void testFloatSerialize() throws Exception {
		String value = Descriptors.float0.toString(1.5f);
		assertEquals("1.5", value);
	}

	@Test
	public void testDoubleParse() throws Exception {
		Double value = (Double) Descriptors.double0.parseObject("0.5");
		assertEquals(0.5d, value, 0.0001);
	}

	@Test
	public void testDoubleSerialize() throws Exception {
		String value = Descriptors.double0.toString(0.5d);
		assertEquals("0.5", value);
	}

	@Test
	public void testList_serialize() throws Exception {
		List<String> list = ImmutableList.of("hello", "world");
		Object result = Descriptors.list(Descriptors.string).toObject(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testList_serializeNull() throws Exception {
		Object result = Descriptors.list(Descriptors.string).toObject(null);
		assertNull(result);
	}

	@Test
	public void testList_parse() throws Exception {
		Object list = ImmutableList.of("hello", "world");
		List<?> result = (List<?>) Descriptors.list(Descriptors.string).parseObject(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testList_parseNull() throws Exception {
		List<?> result = (List<?>) Descriptors.list(Descriptors.string).parseObject(null);
		assertNull(result);
	}

	@Test
	public void testSet_serialize() throws Exception {
		Set<String> list = ImmutableSet.of("hello", "world");
		Object result = Descriptors.set(Descriptors.string).toObject(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testSet_serializeNull() throws Exception {
		Object result = Descriptors.set(Descriptors.string).toObject(null);
		assertNull(result);
	}

	@Test
	public void testSet_parse() throws Exception {
		Object list = ImmutableSet.of("hello", "world");
		Set<?> result = (Set<?>) Descriptors.set(Descriptors.string).parseObject(list);
		assertEquals(list, result);
		assertFalse(list == result);
	}

	@Test
	public void testSet_parseNull() throws Exception {
		Set<?> result = (Set<?>) Descriptors.set(Descriptors.string).parseObject(null);
		assertNull(result);
	}
}
