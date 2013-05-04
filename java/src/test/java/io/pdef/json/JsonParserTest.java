package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.Invocation;
import io.pdef.descriptors.DefaultDescriptorPool;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.fixtures.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class JsonParserTest {
	private DefaultDescriptorPool pool;
	private JsonParser parser;

	@Before
	public void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		pool = new DefaultDescriptorPool();
		parser = new JsonParser(pool, mapper);
	}

	@Test
	public void testParse() throws Exception {
		String s = "{\"type\":\"user\", \"Name\":\"John Doe\",\"Avatar\":{\"url\":\"http://example"
				+ ".com/image.jpg\",\"owner\":null,\"CREATEDAT\":1234},\"photos\":null}";

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

	@Test
	public void testParseInvocations() throws Exception {
		String s = "{\"Calc\": {}, \"Sum\": {\"i0\": 10, \"i1\": 11}}";

		List<Invocation> invocations = parser.parseInvocations(App.class, s);
		InterfaceDescriptor app = (InterfaceDescriptor) pool.getDescriptor(App.class);
		InterfaceDescriptor calc = (InterfaceDescriptor) pool.getDescriptor(Calc.class);

		assertEquals(app.getMethods().get("calc"), invocations.get(0).getMethod());
		assertEquals(calc.getMethods().get("sum"), invocations.get(1).getMethod());

		assertTrue(invocations.get(0).getArgs().length == 0);
		assertArrayEquals(new Object[]{10, 11}, invocations.get(1).getArgs());
	}
}
