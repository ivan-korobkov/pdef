package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.test.*;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageTest {
	@Test
	public void testEquals() throws Exception {
		assertEquals(createTestMessage(), createTestMessage());
	}

	@Test
	public void testHashCode() throws Exception {
		TestMessage msg = createTestMessage();
		int h = msg.hashCode();
		assertTrue(h != 0);
		assertEquals(h, createTestMessage().hashCode());
	}

	@Test
	public void testSerialize() throws Exception {
		Message msg = createTestMessage();
		Map<String, Object> map = msg.serialize();
		Map<String, Object> expected = createTestMessageMap();
		assertEquals(expected, map);
	}

	@Test
	public void testParse() throws Exception {
		Map<String, Object> map = createTestMessageMap();
		Message msg = TestMessage.parse(map);
		Message expected = createTestMessage();
		assertEquals(expected, msg);
	}

	private TestMessage createTestMessage() {
		return TestMessage.builder()
				.setAnEnum(TestEnum.THREE)
				.setABool(true)
				.setAnInt16((short)16)
				.setAnInt32(32)
				.setAnInt64(64L)
				.setAFloat(1f)
				.setADouble(2d)
				.setAString("hello")
				.setAList(ImmutableList.of("a", "b"))
				.setASet(ImmutableSet.of("1", "2"))
				.setAMap(ImmutableMap.of("a", "1"))
				.setAMessage(null)
				.setAnObject("object")
				.build();
	}

	private ImmutableMap<String, Object> createTestMessageMap() {
		return ImmutableMap.<String, Object>builder()
				.put("anEnum", "three")
				.put("aBool", true)
				.put("anInt16", (short) 16)
				.put("anInt32", 32)
				.put("anInt64", 64L)
				.put("aFloat", 1f)
				.put("aDouble", 2d)
				.put("aString", "hello")
				.put("aList", ImmutableList.of("a", "b"))
				.put("aSet", ImmutableSet.of("1", "2"))
				.put("aMap", ImmutableMap.of("a", "1"))
				.put("anObject", "object")
				.build();
	}

	@Test
	public void testParse_polymorphicRootType() throws Exception {
		Map<String, Object> map = ImmutableMap.<String, Object>of("type", "base");
		Tree0 tree = Tree0.parse(map);
		Tree0 expected = Tree0.builder()
				.setType(TreeType.BASE)
				.build();
		assertEquals(expected, tree);
	}

	@Test
	public void testParse_polymorphicNoType() throws Exception {
		Map<String, Object> map = ImmutableMap.of();
		Tree0 tree = Tree0.parse(map);
		Tree0 expected = Tree0.builder()
				.setType(TreeType.BASE)
				.build();
		assertEquals(expected, tree);
	}

	@Test
	public void testParse_polymorphicSubtype() throws Exception {
		Map<String, Object> map = ImmutableMap.<String, Object>of("type", "one");
		Tree0 tree = Tree0.parse(map);
		Tree1 expected = Tree1.builder()
				.setType(TreeType.ONE)
				.build();
		assertEquals(expected, tree);
	}

	@Test
	public void testParse_polymorphicSubtypeSubtype() throws Exception {
		Map<String, Object> map = ImmutableMap.<String, Object>of("type", "two", "type1", "b");
		Tree0 tree = Tree0.parse(map);
		TreeB expected = TreeB.builder()
				.setType(TreeType.TWO)
				.setType1(TreeType1.B)
				.build();
		assertEquals(expected, tree);
	}
}
