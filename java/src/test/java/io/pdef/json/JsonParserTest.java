package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.fixtures.GenericObject;
import io.pdef.fixtures.Image;
import io.pdef.fixtures.User;
import io.pdef.raw.RawParser;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JsonParserTest {
	private JsonParser parser;

	@Before
	public void setUp() throws Exception {
		RawParser rawParser = new RawParser();
		ObjectMapper mapper = new ObjectMapper();
		parser = new JsonParser(rawParser, mapper);
	}

	@Test
	public void testParse() throws Exception {
		String s = "{\"type\":\"user\", \"name\":\"John Doe\",\"avatar\":{\"url\":\"http://example"
				+ ".com/image.jpg\",\"owner\":null,\"createdAt\":1234},\"photos\":null}";

		Image image = new Image.Builder()
				.setUrl("http://example.com/image.jpg")
				.setCreatedAt(1234)
				.build();

		User expected = new User.Builder()
				.setName("John Doe")
				.setAvatar(image)
				.build();

		User user = (User) parser.parse(GenericObject.class, s);
		assertEquals(expected, user);
	}
}
