package io.pdef;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.GenericObject;
import io.pdef.test.Sex;
import io.pdef.test.User;
import io.pdef.test.UserEvent;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonFormatTest {

	@Test
	public void testRead() throws Exception {
		String s = "{\"id\":\"id\",\"type\":\"event\",\"timestamp\":1369301577283,"
				+ "\"eventtype\":\"user\",\"ip\":\"192.168.0.1\",\"user\":{\"id\":\"user-10\","
				+ "\"type\":\"user\",\"timestamp\":0,\"isactive\":true,\"name\":\"John Doe\","
				+ "\"age\":18,\"sex\":\"male\",\"shorts\":{\"10\":11,\"12\":13},\"weight\":0.0}}";

		JsonFormat format = new JsonFormat();
		UserEvent event = (UserEvent) format.read(GenericObject.class, s);
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
							.setShorts(ImmutableMap.of((short) 10, 11L, (short) 12, 13L))
							.build())
					.build();
	}
}
