package com.ivankorobkov.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.*;
import org.junit.Before;

public class PdefJacksonModuleTest {

	@Inject private ObjectMapper mapper;
	@Inject private PdefJacksonModule pdefJacksonModule;

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(new PdefJsonModule(),
				new AbstractModule() {
			@Override
			protected void configure() {
				bind(ObjectMapper.class).in(Singleton.class);
			}
		});
		injector.injectMembers(this);
		mapper.registerModule(pdefJacksonModule);
	}
//
//	@Test
//	public void test() throws Exception {
//		User user = getUser();
//		String s = mapper.writeValueAsString(user);
//		User user1 = mapper.readValue(s, User.class);
//		assertEquals(user, user1);
//	}
//
//	private User getUser() {
//		UserId userId = new UserId().setValue(123L);
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
//		return new User()
//				.setId(userId)
//				.setName("Ivan Korobkov")
//				.setAge(25)
//				.setSex(Sex.MALE)
//				.setProfile(profile)
//				.setFloats(floats);
//	}
}
