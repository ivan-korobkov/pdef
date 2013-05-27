package io.pdef;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableMap;
import io.pdef.test.*;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonFormatTest {
	private JsonFormat format;

	@Before
	public void setUp() throws Exception {
		format = new JsonFormat();
	}

	@Test
	public void testRead() throws Exception {
		String s = "{\"id\":\"id\",\"type\":\"event\",\"timestamp\":1369301577283,"
				+ "\"eventtype\":\"user\",\"ip\":\"192.168.0.1\",\"user\":{\"id\":\"user-10\","
				+ "\"type\":\"user\",\"timestamp\":0,\"isactive\":true,\"name\":\"John Doe\","
				+ "\"age\":18,\"sex\":\"male\",\"shorts\":{\"10\":11,\"12\":13},\"weight\":0.0}}";

		UserEvent event = (UserEvent) format.read(GenericObject.class, s);
		UserEvent expected = getUserEvent();

		assertEquals(expected, event);
	}

	@Test
	public void testRead_object() throws Exception {
		String s = "{\"success\":true,\"result\":{\"id\":\"id\",\"type\":\"event\","
				+ "\"timestamp\":1369301577283,\"eventtype\":\"user\",\"ip\":\"192.168.0.1\","
				+ "\"user\":{\"id\":\"user-10\",\"type\":\"user\",\"timestamp\":0,"
				+ "\"isactive\":true,\"name\":\"John Doe\",\"age\":18,\"sex\":\"male\","
				+ "\"shorts\":{\"10\":11,\"12\":13},\"weight\":0.0}}}";

		Response response = (Response) format.read(Response.class, s);
		assertTrue(response.getResult() instanceof TreeNode);

		UserEvent expected = getUserEvent();
		UserEvent event = (UserEvent) format
				.read(GenericObject.class, (JsonNode) response.getResult());
		assertEquals(expected, event);
	}

	@Test
	public void testWrite() throws Exception {
		UserEvent event = getUserEvent();

		String s = format.write(event);

		UserEvent read = (UserEvent) format.read(UserEvent.class, s);
		assertEquals(event, read);
	}

	@Test
	public void testWrite_object() throws Exception {
		UserEvent event = getUserEvent();
		Response response = Response.builder()
				.setSuccess(true)
				.setResult(event)
				.build();

		String s = format.write(response);
		assertEquals("{\"success\":true,\"result\":{\"id\":\"id\",\"type\":\"event\","
				+ "\"timestamp\":1369301577283,\"eventtype\":\"user\",\"ip\":\"192.168.0.1\","
				+ "\"user\":{\"id\":\"user-10\",\"type\":\"user\",\"timestamp\":0,"
				+ "\"isactive\":true,\"name\":\"John Doe\",\"age\":18,\"sex\":\"male\","
				+ "\"shorts\":{\"10\":11,\"12\":13},\"weight\":0.0}}}", s);
	}

	private UserEvent getUserEvent() {
		return UserEvent.builder()
					.setTimestamp(1369301577283L)
					.setId("id")
					.setIp("192.168.0.1")
					.setUser(User.builder()
							.setId("user-10")
							.setSex(Sex.MALE)
							.setName("John Doe")
							.setIsActive(true)
							.setAge(18)
							.setShorts(ImmutableMap.of((short) 10, 11L, (short) 12, 13L))
							.build())
					.build();
	}
}
