package com.ivankorobkov.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;

public class JsonFormatTest {

	private JsonFormat jsonFormat;

	@Before
	public void setUp() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		LineFormat lineFormat = new LineFormatImpl();
		jsonFormat = new JsonFormatImpl(mapper, lineFormat);
	}
//
//	@Test
//	public void testToJson() throws Exception {
//		Birthday birthday = new Birthday()
//				.setYear(1987)
//				.setMonth(8)
//				.setDay(7);
//
//		Profile profile = new Profile()
//				.setFirstName("Ivan")
//				.setLastName("Korobkov")
//				.setBirthday(birthday)
//				.setComplete(true)
//				.setAvatar(123L)
//				.setPreferedLanguage(Language.EN);
//
//		Map<String, Float> floats = Maps.newHashMap();
//		floats.put("a", 1.2f);
//		floats.put("b", 0f);
//
//		User user = new User()
//				.setName("Ivan Korobkov")
//				.setAge(25)
//				.setSex(Sex.MALE)
//				.setProfile(profile)
//				.setFloats(floats);
//		String s = jsonFormat.toJson(user);
//
//		assertEquals("{\"name\":\"Ivan Korobkov\",\"age\":25,\"sex\":\"male\","
//				+ "\"profile\":{\"firstName\":\"Ivan\",\"lastName\":\"Korobkov\","
//				+ "\"birthday\":\"1987-8-7\",\"avatar\":123,"
//				+ "\"preferedLanguage\":\"en\",\"complete\":true},\"floats\":{\"b\":0.0,"
//				+ "\"a\":1.2}}",
//				s);
//	}
//
//	@Test
//	public void testFromJson() throws Exception {
//		String s = "{\"name\":\"Ivan Korobkov\",\"age\":25,\"sex\":\"male\","
//				+ "\"profile\":{\"firstName\":\"Ivan\",\"lastName\":\"Korobkov\","
//				+ "\"birthday\":\"1987-8-7\",\"avatar\":123,"
//				+ "\"preferedLanguage\":\"en\",\"complete\":true},\"floats\":{\"b\":0.0,"
//				+ "\"a\":1.2}}";
//		User.Descriptor descriptor = User.Descriptor.getInstance();
//		User user = (User) jsonFormat.fromJson(descriptor, s);
//
//		String s2 = jsonFormat.toJson(user);
//		assertEquals(s, s2);
//	}
//
//	@Test
//	public void testTypedFromToJson() throws Exception {
//		User user = new User()
//				.setId(new UserId().setValue(123L));
//		String s = jsonFormat.toJson(user);
//
//		User user1 = (User) jsonFormat.fromJson(User.Descriptor.getInstance(), s);
//		assertEquals(user, user1);
//
//		String s1 = jsonFormat.toJson(user1);
//		assertEquals(s, s1);
//	}
//
//	@Test
//	public void testFromJsonTyped() throws Exception {
//		UserId id = new UserId().setValue(123L);
//		String s = jsonFormat.toJson(id);
//
//		MessageDescriptor descriptor = Id.Descriptor.getInstance();
//		Object object = jsonFormat.fromJson(descriptor, s);
//		assertEquals(id, object);
//	}
}
