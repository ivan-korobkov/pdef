package io.pdef.jackson;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.pdef.test.GenericObject;
import io.pdef.test.User;
import org.junit.Test;

public class PdefJacksonModuleTest {
	@Test
	public void testSerialize() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new PdefJacksonModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

		String s = "{\"type\":\"user\", \"name\":\"John Doe\",\"avatar\":{\"url\":\"http://example"
				+ ".com/image.jpg\",\"owner\":null,\"createdAt\":1234},\"photos\":null}";

		User expected = new User.Builder()
				.setName("John Doe")
				.build();

		Object user = mapper.readValue(s, GenericObject.class);
		System.out.println(user);
	}
}
