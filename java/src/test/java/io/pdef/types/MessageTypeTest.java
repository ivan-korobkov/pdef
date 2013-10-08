package io.pdef.types;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.inheritance.Base;
import io.pdef.test.inheritance.MultiLevelSubtype;
import io.pdef.test.inheritance.Subtype;
import io.pdef.test.inheritance.Subtype2;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.*;
import org.junit.Test;

import java.util.Map;

public class MessageTypeTest {
	private MessageType descriptor = SimpleMessage.classType();

	@Test
	public void testGetJavaClass() throws Exception {
		assertTrue(descriptor.getJavaClass() == SimpleMessage.class);
	}

	@Test
	public void testGetBase() throws Exception {
		assertNull(descriptor.getBase());
	}

	@Test
	public void testGetSubtypes() throws Exception {
		assertTrue(descriptor.getSubtypes().isEmpty());
	}

	@Test
	public void testToNative() throws Exception {
		SimpleMessage message = fixture();
		Object map = descriptor.toNative(message);
		Map<String, Object> expected = fixtureMap();

		assertEquals(expected, map);
	}

	@Test
	public void testParseObject() throws Exception {
		Map<String, Object> map = fixtureMap();
		SimpleMessage message = (SimpleMessage) descriptor.parseNative(map);
		SimpleMessage expected = fixture();

		assertEquals(expected, message);
	}

	@Test
	public void testToJson() throws Exception {
		SimpleMessage message = fixture();
		String s = descriptor.toJson(message);
		SimpleMessage parsed = (SimpleMessage) descriptor.parseJson(s);
		assertEquals(message, parsed);
	}

	@Test
	public void testParseJson() throws Exception {
		SimpleMessage message = (SimpleMessage) descriptor.parseJson(fixtureJson());
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

//	@Test
//	public void testSubtype_polymorphic() throws Exception {
//		MessageType descriptor = Base.classType();
//
//		assertTrue(descriptor.getSubtype(PolymorphicType.SUBTYPE) == Subtype.descriptor());
//		assertTrue(descriptor.getSubtype(PolymorphicType.SUBTYPE2) == Subtype2.descriptor());
//		assertTrue(descriptor.getSubtype(PolymorphicType.MULTILEVEL_SUBTYPE) == MultiLevelSubtype
//				.descriptor());
//	}

	@Test
	public void testParseObject_polymorphic() throws Exception {
		Map<String, Object> subtypeMap = ImmutableMap.<String, Object>of("type",
				"subtype", "subfield", "hello");
		Map<String, Object> subtype2Map = ImmutableMap.<String, Object>of("type",
				"subtype2", "subfield2", "hello");
		Map<String, Object> mlevelSubtypeMap = ImmutableMap.<String, Object>of("type",
				"multilevel_subtype", "subfield", "hello", "mfield", "bye");

		MessageType descriptor = Base.classType();

		Subtype subtype = new Subtype().setSubfield("hello");
		Subtype2 subtype2 = new Subtype2().setSubfield2("hello");
		MultiLevelSubtype mlevelSubtype = new MultiLevelSubtype()
				.setSubfield("hello")
				.setMfield("bye");

		assertEquals(subtype, descriptor.parseNative(subtypeMap));
		assertEquals(subtype2, descriptor.parseNative(subtype2Map));
		assertEquals(mlevelSubtype, descriptor.parseNative(mlevelSubtypeMap));
	}
}
