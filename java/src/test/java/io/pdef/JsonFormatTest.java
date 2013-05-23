package io.pdef;

import io.pdef.test.Sex;
import io.pdef.test.User;
import io.pdef.test.UserEvent;
import org.junit.Test;

public class JsonFormatTest {

	@Test
	public void testRead() throws Exception {
		String s = "{\"id\":\"id\",\"type\":\"event\",\"timestamp\":1369299517714,"
				+ "\"eventtype\":\"user\",\"ip\":\"192.168.0.1\",\"user\":{\"id\":\"user-10\","
				+ "\"type\":\"user\",\"timestamp\":0,\"isactive\":true,\"name\":\"John Doe\","
				+ "\"age\":18,\"sex\":\"male\",\"weight\":0.0}}";

		JsonFormat format = new JsonFormat();
		UserEvent event = (UserEvent) format.read(UserEvent.class, s);
		System.out.println(event);
	}

	@Test
	public void testWrite() throws Exception {
		UserEvent event = UserEvent.builder()
				.setTimestamp(System.currentTimeMillis())
				.setId("id")
				.setIp("192.168.0.1")
				.setUser(User.builder()
						.setId("user-10")
						.setSex(Sex.MALE)
						.setName("John Doe")
						.setIsActive(true)
						.setAge(18)
						.build())
				.build();

		JsonFormat format = new JsonFormat();
		String s = format.write(event);
		System.out.println(s);
	}
}
