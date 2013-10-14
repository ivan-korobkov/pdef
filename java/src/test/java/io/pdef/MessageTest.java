package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.test.messages.ComplexMessage;
import io.pdef.test.messages.TestEnum;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Map;

public class MessageTest {
	@Test
	public void testEquals() throws Exception {
		assertEquals(createComplexMessage(), createComplexMessage());
	}

	@Test
	public void testHashCode() throws Exception {
		ComplexMessage msg = createComplexMessage();
		int h = msg.hashCode();
		assertTrue(h != 0);
		assertEquals(h, createComplexMessage().hashCode());
	}

	@Test
	public void  testSerialize() throws Exception {
		Message msg = createComplexMessage();
		Map<String, Object> map = msg.serializeToMap();
		Map<String, Object> expected = createComplexMessageMap();
		assertEquals(expected, map);
	}

	@Test
	public void testParse() throws Exception {
		Map<String, Object> map = createComplexMessageMap();
		Message msg = ComplexMessage.parseFromMap(map);
		Message expected = createComplexMessage();
		assertEquals(expected, msg);
	}

	private ComplexMessage createComplexMessage() {
		return new ComplexMessage()
				.setAnEnum(TestEnum.THREE)
				.setABool(true)
				.setAnInt16((short) 16)
				.setAnInt32(32)
				.setAnInt64(64L)
				.setAFloat(1f)
				.setADouble(2d)
				.setAString("hello")
				.setAList(ImmutableList.of(1, 2))
				.setASet(ImmutableSet.of(1, 2))
				.setAMap(ImmutableMap.<Integer, Float>of(1, 1.5f))
				.setAMessage(null);
	}

	private Map<String, Object> createComplexMessageMap() {
		return ImmutableMap.<String, Object>builder()
				.put("anEnum", TestEnum.THREE)
				.put("aBool", true)
				.put("anInt16", (short) 16)
				.put("anInt32", 32)
				.put("anInt64", 64L)
				.put("aFloat", 1f)
				.put("aDouble", 2d)
				.put("aString", "hello")
				.put("aList", ImmutableList.of(1, 2))
				.put("aSet", ImmutableSet.of(1, 2))
				.put("aMap", ImmutableMap.of(1, 1.5f))
				.build();
	}
}
