package io.pdef;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.pdef.test.messages.TestMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class DataTypesTest {
	private final Random random = new Random();

	@Test
	public void testCopy_message() throws Exception {
		TestMessage message = createMessage();

		TestMessage copy = DataTypes.copy(message);
		assertEquals(message, copy);
		assertNotSame(message, copy);
	}

	@Test
	public void testCopy_messageNull() throws Exception {
		Message message = DataTypes.copy((Message) null);
		assertNull(message);
	}

	@Test
	public void testCopy_list() throws Exception {
		TestMessage message0 = new TestMessage();
		TestMessage message1 = new TestMessage();
		List<TestMessage> list = ImmutableList.of(message0, message1);

		List<TestMessage> copy = DataTypes.copy(list);
		assertEquals(list, copy);
		assertNotSame(list, copy);
		assertNotSame(message0, copy.get(0));
		assertNotSame(message1, copy.get(1));
	}

	@Test
	public void testCopy_set() throws Exception {
		TestMessage message0 = new TestMessage();
		TestMessage message1 = new TestMessage();
		Set<TestMessage> list = ImmutableSet.of(message0, message1);

		Set<TestMessage> copy = DataTypes.copy(list);
		assertEquals(list, copy);
		assertNotSame(list, copy);
	}

	@Test
	public void testCopy_map() throws Exception {
		TestMessage message0 = new TestMessage();
		TestMessage message1 = new TestMessage();
		Map<Integer, TestMessage> map = ImmutableMap.of(0, message0, 1, message1);

		Map<Integer, TestMessage> copy = DataTypes.copy(map);
		assertEquals(map, copy);
		assertNotSame(map, copy);
		assertNotSame(message0, copy.get(0));
		assertNotSame(message1, copy.get(1));
	}

	private TestMessage createMessage() {
		return new TestMessage()
				.setBool0(random.nextBoolean())
				.setShort0((short) random.nextInt())
				.setString0(String.valueOf(random.nextInt()));
	}
}
