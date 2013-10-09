package io.pdef.types;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.inheritance.*;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Map;

public class MessageTypeTest {
	private MessageType type = SimpleMessage.classType();

	@Test
	public void testGetJavaClass() throws Exception {
		assertTrue(type.getJavaClass() == SimpleMessage.class);
	}

	@Test
	public void testGetBase() throws Exception {
		assertNull(type.getBase());
	}

	@Test
	public void testGetSubtypes() throws Exception {
		assertTrue(type.getSubtypes().isEmpty());
	}

	@Test
	public void testToNative() throws Exception {
		SimpleMessage message = fixture();
		Object map = type.toNative(message);
		Map<String, Object> expected = fixtureMap();

		assertEquals(expected, map);
	}

	@Test
	public void testParseObject() throws Exception {
		Map<String, Object> map = fixtureMap();
		SimpleMessage message = (SimpleMessage) type.parseNative(map);
		SimpleMessage expected = fixture();

		assertEquals(expected, message);
	}

	@Test
	public void testToJson() throws Exception {
		SimpleMessage message = fixture();
		String s = type.toJson(message);
		SimpleMessage parsed = (SimpleMessage) type.parseJson(s);
		assertEquals(message, parsed);
	}

	@Test
	public void testParseJson() throws Exception {
		SimpleMessage message = (SimpleMessage) type.parseJson(fixtureJson());
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
	public void testSubtype_polymorphic() throws Exception {
		MessageType type = Base.classType();

		assertTrue(type.getSubtype(PolymorphicType.SUBTYPE) == Subtype.classType());
		assertTrue(type.getSubtype(PolymorphicType.SUBTYPE2) == Subtype2.classType());
		assertTrue(type.getSubtype(PolymorphicType.MULTILEVEL_SUBTYPE) == MultiLevelSubtype
				.classType());
	}

	@Test
	public void testParseObject_polymorphic() throws Exception {
		Map<String, Object> subtypeMap = ImmutableMap.<String, Object>of("type",
				"subtype", "subfield", "hello");
		Map<String, Object> subtype2Map = ImmutableMap.<String, Object>of("type",
				"subtype2", "subfield2", "hello");
		Map<String, Object> mlevelSubtypeMap = ImmutableMap.<String, Object>of("type",
				"multilevel_subtype", "subfield", "hello", "mfield", "bye");

		MessageType type = Base.classType();

		Subtype subtype = new Subtype().setSubfield("hello");
		Subtype2 subtype2 = new Subtype2().setSubfield2("hello");
		MultiLevelSubtype mlevelSubtype = new MultiLevelSubtype()
				.setSubfield("hello")
				.setMfield("bye");

		assertEquals(subtype, type.parseNative(subtypeMap));
		assertEquals(subtype2, type.parseNative(subtype2Map));
		assertEquals(mlevelSubtype, type.parseNative(mlevelSubtypeMap));
	}
}
