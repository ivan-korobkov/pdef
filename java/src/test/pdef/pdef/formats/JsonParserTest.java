package pdef.formats;

import com.google.common.collect.Maps;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import pdef.fixtures.*;

import java.util.Map;

public class JsonParserTest {
	private JsonParser parser;

	@Before
	public void setUp() throws Exception {
		parser = new JsonParser();
	}

	@Test
	public void testParse() throws Exception {
		String s = "{\"type\":\"user\",\"name\":\"Ivan Korobkov\",\"age\":25,\"sex\":\"male\","
				+ "\"profile\":{\"firstName\":\"Ivan\",\"lastName\":\"Korobkov\","
				+ "\"birthday\":{\"year\":1987,\"month\":8,\"day\":7},\"avatar\":123,"
				+ "\"preferedLanguage\":\"en\",\"complete\":true},\"floats\":{\"b\":0.0,"
				+ "\"a\":1.2}}";
		User user = (User) parser.parse(GenericObject.getClassDescriptor(), s);

		Birthday birthday = Birthday.builder()
				.setYear(1987)
				.setMonth(8)
				.setDay(7)
				.build();
		Profile profile = Profile.builder()
				.setFirstName("Ivan")
				.setLastName("Korobkov")
				.setBirthday(birthday)
				.setComplete(true)
				.setAvatar(123L)
				.setPreferedLanguage(Language.EN)
				.build();
		Map<String, Float> floats = Maps.newHashMap();
		floats.put("a", 1.2f);
		floats.put("b", 0f);
		User user0 = User.builder()
				.setName("Ivan Korobkov")
				.setAge(25)
				.setSex(Sex.MALE)
				.setProfile(profile)
				.setFloats(floats)
				.build();
		assertEquals(user0, user);
	}
}
