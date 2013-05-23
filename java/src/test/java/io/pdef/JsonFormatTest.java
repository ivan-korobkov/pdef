package io.pdef;

import io.pdef.test.Sex;
import io.pdef.test.User;
import io.pdef.test.UserEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonFormatTest {

	@Test
	public void testRead() throws Exception {
		String s = "{\"id\":\"id\",\"type\":\"EVENT\",\"TiMeStamP\":1369301577283,"
				+ "\"eventtype\":\"user\",\"ip\":\"192.168.0.1\",\"USER\":{\"id\":\"user-10\","
				+ "\"type\":\"user\",\"timestamp\":0,\"ISACTIVE\":true,\"name\":\"John Doe\","
				+ "\"age\":18,\"sex\":\"male\",\"weight\":0.0}}";

		JsonFormat format = new JsonFormat();
		UserEvent event = (UserEvent) format.read(UserEvent.class, s);
		UserEvent expected = getUserEvent();

		assertEquals(expected, event);
	}

	@Test
	public void testWrite() throws Exception {
		UserEvent event = getUserEvent();

		JsonFormat format = new JsonFormat();
		String s = format.write(event);

		UserEvent read = (UserEvent) format.read(UserEvent.class, s);
		assertEquals(event, read);
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
							.build())
					.build();
	}
}
