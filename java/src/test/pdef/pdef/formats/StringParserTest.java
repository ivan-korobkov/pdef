package pdef.formats;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import pdef.fixtures.GenericObject;
import pdef.fixtures.Profile;
import pdef.fixtures.Sex;
import pdef.fixtures.User;

public class StringParserTest {

	@Test
	public void testParse() throws Exception {
		Profile profile = Profile.builder()
				.setAvatar(123L)
				.setComplete(true)
				.setWallpaper(7L)
				.setFirstName("John")
				.setLastName("Doe")
				.build();

		User user = User.builder()
				.setName("John Doe")
				.setSex(Sex.MALE)
				.setProfile(profile)
				.build();

		StringSerializer serializer = new StringSerializer();
		String s = (String) serializer.serialize(user);

		StringParser parser = new StringParser();
		User user1 = (User) parser.parse(User.getClassDescriptor(), s);
		assertEquals(user, user1);
	}

	@Test
	public void testParse_polymorphic() throws Exception {
		Profile profile = Profile.builder()
				.setAvatar(123L)
				.setComplete(true)
				.setWallpaper(7L)
				.setFirstName("-John-")
				.setLastName(".Doe.")
				.build();

		User user = User.builder()
				.setName("John+Doe")
				.setSex(Sex.MALE)
				.setProfile(profile)
				.build();

		StringSerializer serializer = new StringSerializer();
		String s = (String) serializer.serialize(user);

		StringParser parser = new StringParser();
		User user1 = (User) parser.parse(GenericObject.getClassDescriptor(), s);
		assertEquals(user, user1);
	}
}
