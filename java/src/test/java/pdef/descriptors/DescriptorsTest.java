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
		assert value;
	}

	@Test
	public void testBoolParse_null() throws Exception {
		Boolean value = (Boolean) Descriptors.bool.parseObject(null);
		assert value == null;
	}

	@Test
	public void testBoolSerialize() throws Exception {
		String value = Descriptors.bool.toString(true);
		assert "true".equals(value);
	}

	@Test
	public void testInt16Parse() throws Exception {
		Short value = (Short) Descriptors.int16.parseObject("123");
		assert (short) 123 == value;
	}

	@Test
	public void testInt16Parse_number() throws Exception {
		Short value = (Short) Descriptors.int16.parseObject(123);
		assert (short) 123 == value;
	}

	@Test
	public void testInt16Parse_null() throws Exception {
		Short value = (Short) Descriptors.int16.parseObject(null);
		assert value == null;
	}

	@Test
	public void testInt16Serialize() throws Exception {
		String value = Descriptors.int16.toString((short) 123);
		assertEquals("123", value);
	}

	@Test
	public void testInt32Parse() throws Exception {
		Integer value = (Integer) Descriptors.int32.parseObject("123");
		assert 123 == value;
	}

	@Test
	public void testInt32Parse_number() throws Exception {
		Integer value = (Integer) Descriptors.int32.parseObject((long) 123);
		assert 123 == value;
	}

	@Test
	public void testInt32Parse_null() throws Exception {
		Integer value = (Integer) Descriptors.int32.parseObject(null);
		assert value == null;
	}

	@Test
	public void testInt32Serialize() throws Exception {
		String value = Descriptors.int32.toString(123);
		assert "123".equals(value);
	}

	@Test
	public void testInt64Parse() throws Exception {
		Long value = (Long) Descriptors.int64.parseObject("123");
		assert 123L == value;
	}

	@Test
	public void testInt64Parse_number() throws Exception {
		Long value = (Long) Descriptors.int64.parseObject((short) 123);
		assert 123L == value;
	}

	@Test
	public void testInt64Parse_null() throws Exception {
		Long value = (Long) Descriptors.int64.parseObject(null);
		assert value == null;
	}

	@Test
	public void testInt64Serialize() throws Exception {
		String value = Descriptors.int64.toString(123L);
		assert "123".equals(value);
	}

	@Test
	public void testFloatParse() throws Exception {
		Float value = (Float) Descriptors.float0.parseObject("1.5");
		assertEquals(1.5f, value, 1e-5);
	}

	@Test
	public void testFloatParse_number() throws Exception {
		Float value = (Float) Descriptors.float0.parseObject(123);
		assertEquals(123f, value, 1e-5);
	}

	@Test
	public void testFloatParse_null() throws Exception {
		Float value = (Float) Descriptors.float0.parseObject(null);
		assert value == null;
	}

	@Test
	public void testFloatSerialize() throws Exception {
		String value = Descriptors.float0.toString(1.5f);
		assert "1.5".equals(value);
	}

	@Test
	public void testDoubleParse() throws Exception {
		Double value = (Double) Descriptors.double0.parseObject("0.5");
		assertEquals(0.5d, value, 1e-5);
	}

	@Test
	public void testDoubleParse_number() throws Exception {
		Double value = (Double) Descriptors.double0.parseObject(123);
		assertEquals(123d, value, 1e-5);
	}

	@Test
	public void testDoubleParse_null() throws Exception {
		Double value = (Double) Descriptors.double0.parseObject(null);
		assert value == null;
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
