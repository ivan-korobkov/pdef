package pdef.descriptors;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import pdef.test.messages.SimpleMessage;
import pdef.test.polymorphic.*;

import java.util.Map;

import static org.junit.Assert.*;

public class MessageDescriptorTest {
	private MessageDescriptor descriptor = SimpleMessage.descriptor();

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
	public void testGetSubtype() throws Exception {
		assertNull(descriptor.getSubtype(null));
	}

	@Test
	public void testToBuilder() throws Exception {
		SimpleMessage message = fixture();
		SimpleMessage.Builder builder = (SimpleMessage.Builder) descriptor.toBuilder(message);

		assertEquals(message.getABool(), builder.getABool());
		assertEquals(message.getAnInt16(), builder.getAnInt16());
		assertEquals(message.getAString(), builder.getAString());
	}

	@Test
	public void testToObject() throws Exception {
		SimpleMessage message = fixture();
		Map<String, Object> map = descriptor.toObject(message);
		Map<String, Object> expected = fixtureMap();

		assertEquals(expected, map);
	}

	@Test
	public void testParseObject() throws Exception {
		Map<String, Object> map = fixtureMap();
		SimpleMessage message = (SimpleMessage) descriptor.parseObject(map);
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
		return SimpleMessage.builder()
				.setABool(Boolean.TRUE)
				.setAnInt16((short) 123)
				.setAString("hello")
				.build();
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
		MessageDescriptor descriptor = Base.descriptor();

		assertTrue(descriptor.getSubtype(PolymorphicType.SUBTYPE) == Subtype.descriptor());
		assertTrue(descriptor.getSubtype(PolymorphicType.SUBTYPE2) == Subtype2.descriptor());
		assertTrue(descriptor.getSubtype(PolymorphicType.MULTILEVEL_SUBTYPE) == MultiLevelSubtype
				.descriptor());
	}

	@Test
	public void testParseObject_polymorphic() throws Exception {
		Map<String, Object> subtypeMap = ImmutableMap.<String, Object>of("type",
				"subtype", "subfield", "hello");
		Map<String, Object> subtype2Map = ImmutableMap.<String, Object>of("type",
				"subtype2", "subfield2", "hello");
		Map<String, Object> mlevelSubtypeMap = ImmutableMap.<String, Object>of("type",
				"multilevel_subtype", "subfield", "hello", "mfield", "bye");

		MessageDescriptor descriptor = Base.descriptor();

		Subtype subtype = Subtype.builder()
				.setSubfield("hello")
				.build();
		Subtype2 subtype2 = Subtype2.builder()
				.setSubfield2("hello")
				.build();
		MultiLevelSubtype mlevelSubtype = MultiLevelSubtype.builder()
				.setSubfield("hello")
				.setMfield("bye")
				.build();

		assertEquals(subtype, descriptor.parseObject(subtypeMap));
		assertEquals(subtype2, descriptor.parseObject(subtype2Map));
		assertEquals(mlevelSubtype, descriptor.parseObject(mlevelSubtypeMap));
	}
}
