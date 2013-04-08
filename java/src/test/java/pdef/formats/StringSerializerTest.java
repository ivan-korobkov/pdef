package pdef.formats;

import org.junit.Test;
import pdef.fixtures.Profile;
import pdef.fixtures.Sex;
import pdef.fixtures.User;

import static org.junit.Assert.assertEquals;

public class StringSerializerTest {
	@Test
	public void testSerialize() throws Exception {
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
		assertEquals("-user--John+Doe--male-{%2DJohn%2D-%2EDoe%2E--123-7----1}--", s);
	}
}
