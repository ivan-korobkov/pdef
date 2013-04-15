package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.fixtures.Image;
import io.pdef.fixtures.User;
import io.pdef.raw.RawSerializer;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonSerializerTest {
	private JsonSerializer serializer;

	@Before
	public void setUp() throws Exception {
		RawSerializer rawSerializer = new RawSerializer();
		ObjectMapper mapper = new ObjectMapper();
		serializer = new JsonSerializer(rawSerializer, mapper);
	}

	@Test
	public void test() throws Exception {
		Image image = new Image.Builder()
				.setUrl("http://example.com/image.jpg")
				.setCreatedAt(1234)
				.build();

		User user = new User.Builder()
				.setName("John Doe")
				.setAvatar(image)
				.build();

		String s = serializer.serialize(user);
		assertEquals("{\"type\":\"user\",\"name\":\"John Doe\",\"avatar\":{\"type\":\"image\","
				+ "\"url\":\"http://example.com/image.jpg\",\"owner\":null,\"createdAt\":1234},"
				+ "\"photos\":null}", s);
	}
}
