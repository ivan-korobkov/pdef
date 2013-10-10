package io.pdef.meta;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.inheritance.*;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Map;

public class MessageTypeTest {
	private MessageType<SimpleMessage> metaType = SimpleMessage.META_TYPE;

	@Test
	public void testGetJavaClass() throws Exception {
		assertTrue(metaType.getJavaClass() == SimpleMessage.class);
	}

	@Test
	public void testGetBase() throws Exception {
		assertNull(metaType.getBase());
	}

	@Test
	public void testGetSubtypes() throws Exception {
		assertTrue(metaType.getSubtypes().isEmpty());
	}

	@Test
	public void testToNative() throws Exception {
		SimpleMessage message = fixture();
		Object map = metaType.serializeToNative(message);
		Map<String, Object> expected = fixtureMap();

		assertEquals(expected, map);
	}

	@Test
	public void testParseObject() throws Exception {
		Map<String, Object> map = fixtureMap();
		SimpleMessage message = metaType.parseFromNative(map);
		SimpleMessage expected = fixture();

		assertEquals(expected, message);
	}

	@Test
	public void testToJson() throws Exception {
		SimpleMessage message = fixture();
		String s = metaType.serializeToJson(message);
		SimpleMessage parsed = metaType.parseFromJson(s);
		assertEquals(message, parsed);
	}

	@Test
	public void testParseJson() throws Exception {
		SimpleMessage message = metaType.parseFromJson(fixtureJson());
		SimpleMessage expected = fixture();
		assertEquals(expected, message);
	}

	private SimpleMessage fixture() {
		return new SimpleMessage()
				.setABool(Boolean.TRUE)
				.setAnInt16((short) 123)
				.setAString("hello");
	}

	private Map<String, Object> fixtureMap() {
		return ImmutableMap.<String, Object>of(
				"aBool", true,
				"anInt16", (short) 123,
				"aString", "hello");
	}

	private String fixtureJson() {
		return "{\"aString\": \"hello\", \"aBool\": true, \"anInt16\": 123}";
	}

	// Polymorphic message tests.

	@Test
	public void testParseObject_polymorphic() throws Exception {
		Map<String, Object> subtypeMap = ImmutableMap.<String, Object>of("type",
				"subtype", "subfield", "hello");
		Map<String, Object> subtype2Map = ImmutableMap.<String, Object>of("type",
				"subtype2", "subfield2", "hello");
		Map<String, Object> mlevelSubtypeMap = ImmutableMap.<String, Object>of("type",
				"multilevel_subtype", "subfield", "hello", "mfield", "bye");

		Subtype subtype = new Subtype().setSubfield("hello");
		Subtype2 subtype2 = new Subtype2().setSubfield2("hello");
		MultiLevelSubtype mlevelSubtype = new MultiLevelSubtype()
				.setSubfield("hello")
				.setMfield("bye");

		MessageType metaType = Base.META_TYPE;
		assertEquals(subtype, metaType.parseFromNative(subtypeMap));
		assertEquals(subtype2, metaType.parseFromNative(subtype2Map));
		assertEquals(mlevelSubtype, metaType.parseFromNative(mlevelSubtypeMap));
	}
}
