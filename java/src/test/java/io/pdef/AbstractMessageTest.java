package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.test.messages.TestValues;
import io.pdef.test.messages.TestMessage;
import io.pdef.test.messages.TestEnum;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AbstractMessageTest {
	@Test
	public void testEquals() throws Exception {
		assertEquals(createComplexMessage(), createComplexMessage());
	}

	@Test
	public void testHashCode() throws Exception {
		TestValues msg = createComplexMessage();
		int h = msg.hashCode();
		assertTrue(h != 0);
		assertEquals(h, createComplexMessage().hashCode());
	}

	@Test
	public void testToMap() throws Exception {
		Message msg = createComplexMessage();
		Map<String, Object> map = msg.toMap();
		Map<String, Object> expected = createComplexMessageMap();
		assertEquals(expected, map);
	}

	@Test
	public void testFromMap() throws Exception {
		Map<String, Object> map = createComplexMessageMap();
		Message msg = TestValues.fromMap(map);
		Message expected = createComplexMessage();
		assertEquals(expected, msg);
	}

	@Test
	public void testInitNullFields() throws Exception {
		TestValues message = new TestValues();
		List<Integer> list = message.getList0();
		Set<Integer> set = message.getSet0();
		Map<Integer, Float> map = message.getMap0();
		TestMessage testMessage = message.getMessage0();

		assertNotNull(list);
		assertNotNull(set);
		assertNotNull(map);
		assertNotNull(testMessage);

		assertSame(list, message.getList0());
		assertSame(set, message.getSet0());
		assertSame(map, message.getMap0());
		assertSame(testMessage, message.getMessage0());
	}

	@Test
	public void testCopy() throws Exception {
		TestValues message = createComplexMessage();
		TestValues copy = message.copy();

		assertEquals(message, copy);
		assertNotSame(message, copy);
	}

	@Test
	public void testMerge() throws Exception {
		TestValues message = createComplexMessage();
		TestValues another = new TestValues().merge(message);

		assertEquals(message, another);
	}

	private TestValues createComplexMessage() {
		return new TestValues()
				.setEnum0(TestEnum.THREE)
				.setBool0(true)
				.setShort0((short) -16)
				.setInt0(-32)
				.setLong0(-64L)
				.setFloat0(-1.5f)
				.setDouble0(-2.5d)
				.setString0("hello")
				.setList0(ImmutableList.of(1, 2))
				.setSet0(ImmutableSet.of(1, 2))
				.setMap0(ImmutableMap.<Integer, Float>of(1, 1.5f))
				.setMessage0(null);
	}

	private Map<String, Object> createComplexMessageMap() {
		return ImmutableMap.<String, Object>builder()
				.put("enum0", TestEnum.THREE)
				.put("bool0", true)
				.put("short0", (short) -16)
				.put("int0", -32)
				.put("long0", -64L)
				.put("float0", -1.5f)
				.put("double0", -2.5d)
				.put("string0", "hello")
				.put("list0", ImmutableList.of(1, 2))
				.put("set0", ImmutableSet.of(1, 2))
				.put("map0", ImmutableMap.of(1, 1.5f))
				.build();
	}
}
