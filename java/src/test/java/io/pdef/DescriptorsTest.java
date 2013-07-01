package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class DescriptorsTest {

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
