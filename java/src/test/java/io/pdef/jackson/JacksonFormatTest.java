package io.pdef.jackson;

import io.pdef.test.GenericObject;
import io.pdef.test.Sex;
import io.pdef.test.User;
import io.pdef.test.UserEvent;
import org.junit.Test;

public class JacksonFormatTest {
	@Test
	public void testWrite() throws Exception {
		JacksonFormat format = new JacksonFormat();

		User user = User.builder()
				.setId("id")
				.setAge(18)
				.setIsActive(true)
				.setName("John Doe")
				.setSex(Sex.MALE)
				.build();

		UserEvent event = UserEvent.builder()
				.setId("id")
				.setIp("192.168.0.1")
				.setTimestamp(System.currentTimeMillis())
				.setUser(user)
				.build();

		String s = format.write(event);
		System.out.println(s);
	}

	@Test
	public void testRead() throws Exception {
		JacksonFormat format = new JacksonFormat();

		String s = "{\n"
				+ "  \"eventType\" : \"user\",\n"
				+ "  \"id\" : \"id\",\n"
				+ "  \"type\" : \"event\",\n"
				+ "  \"timestamp\" : 1369230834239,\n"
				+ "  \"eventType\" : \"user\",\n"
				+ "  \"ip\" : \"192.168.0.1\",\n"
				+ "  \"user\" : {\n"
				+ "    \"type\" : \"user\",\n"
				+ "    \"id\" : \"id\",\n"
				+ "    \"type\" : \"user\",\n"
				+ "    \"timestamp\" : 0,\n"
				+ "    \"isActive\" : true,\n"
				+ "    \"name\" : \"John Doe\",\n"
				+ "    \"age\" : 18,\n"
				+ "    \"sex\" : \"male\",\n"
				+ "    \"profile\" : {\n"
				+ "      \"birthday\" : {\n"
				+ "        \"year\" : 0,\n"
				+ "        \"month\" : 0,\n"
				+ "        \"day\" : 0\n"
				+ "      },\n"
				+ "      \"avatar\" : 0,\n"
				+ "      \"wallpaper\" : 0,\n"
				+ "      \"complete\" : false\n"
				+ "    },\n"
				+ "    \"floats\" : { },\n"
				+ "    \"chats\" : [ ],\n"
				+ "    \"shorts\" : { },\n"
				+ "    \"ints\" : [ ],\n"
				+ "    \"weight\" : 0.0\n"
				+ "  }\n"
				+ "}";

		GenericObject object = (GenericObject) format.read(s, GenericObject.class);
		System.out.println(object);
	}

	@Test
	public void testRead_inheritanceTree() throws Exception {
	}
}
