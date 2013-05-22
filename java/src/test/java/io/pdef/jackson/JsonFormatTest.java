package io.pdef.jackson;

import io.pdef.test.GenericObject;
import io.pdef.test.Sex;
import io.pdef.test.User;
import io.pdef.test.UserEvent;
import org.junit.Test;

public class JsonFormatTest {
	@Test
	public void testWrite() throws Exception {
		JsonFormat format = new JsonFormat();

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
		JsonFormat format = new JsonFormat();

		String s = "{\"type\":\"user\", \"name\":\"John Doe\",\"avatar\":{\"url\":\"http://example"
				+ ".com/image.jpg\",\"owner\":null,\"createdAt\":1234},\"photos\":null}";

		User expected = new User.Builder()
				.setName("John Doe")
				.build();

		Object user = format.read(s, GenericObject.class);
		System.out.println(user);
	}

	@Test
	public void testRead_inheritanceTree() throws Exception {
	}
}
