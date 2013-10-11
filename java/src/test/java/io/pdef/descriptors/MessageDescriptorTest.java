package io.pdef.descriptors;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.inheritance.Subtype;
import io.pdef.test.messages.SimpleMessage;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.util.Map;

public class MessageDescriptorTest {

	@Test
	public void testGetJavaClass() throws Exception {
		assertTrue(SimpleMessage.DESCRIPTOR.getJavaClass() == SimpleMessage.class);
	}

	@Test
	public void testGetBase() throws Exception {
		assertNull(SimpleMessage.DESCRIPTOR.getBase());
	}

	@Test
	public void testGetSubtypes() throws Exception {
		assertTrue(SimpleMessage.DESCRIPTOR.getSubtypes().isEmpty());
	}

	// Fixtures.

	private SimpleMessage fixture() {
		return new SimpleMessage()
				.setABool(Boolean.TRUE)
				.setAnInt16((short) 123)
				.setAString("hello");
	}

	private Subtype polymorphicFixture() {
		return new Subtype().setSubfield("hello, world");
	}

	private Map<String, Object> fixtureMap() {
		return ImmutableMap.<String, Object>of(
				"aBool", true,
				"anInt16", (short) 123,
				"aString", "hello");
	}

	private Map<String, Object> polymorphicFixtureMap() {
		return ImmutableMap.<String, Object>of(
				"type", "subtype",
				"subfield", "hello, world");
	}

	private String fixtureJson() {
		return "{\"aString\":\"hello\",\"aBool\":true,\"anInt16\":123}";
	}
}
