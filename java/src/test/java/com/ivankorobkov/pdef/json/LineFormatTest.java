package com.ivankorobkov.pdef.json;

import com.google.common.collect.ImmutableList;
import com.ivankorobkov.pdef.fixtures.*;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

public class LineFormatTest {

	private LineFormatImpl lineFormat;

	@Before
	public void setUp() throws Exception {
		lineFormat = new LineFormatImpl();
	}

	@Test
	public void testToJson() throws Exception {
		Birthday birthday = new Birthday()
				.setYear(1987)
				.setMonth(8)
				.setDay(7);

		Profile profile = new Profile()
				.setFirstName("Ivan")
				.setLastName("Korobkov {}-")
				.setBirthday(birthday)
				.setComplete(true)
				.setAvatar(123L)
				.setPreferedLanguage(Language.EN);

		User user = new User()
				.setName("ivan")
				.setSex(Sex.MALE)
				.setProfile(profile);

		String s = lineFormat.toJson(user);
		assertEquals("-ivan--male-{Ivan-Korobkov %7B%7D%2D-{1987-8-7}-123--en---1}-", s);
	}

	@Test
	public void testFromJson() throws Exception {
		Birthday birthday = new Birthday()
				.setYear(1987)
				.setMonth(8)
				.setDay(7);

		Profile profile = new Profile()
				.setFirstName("Ivan")
				.setLastName("Korobkov {}-")
				.setBirthday(birthday)
				.setComplete(true)
				.setAvatar(123L)
				.setPreferedLanguage(Language.EN);

		String s = lineFormat.toJson(profile);
		Object object = lineFormat.fromJson(Profile.Descriptor.getInstance(), s);
		assertEquals(profile, object);
	}

	@Test
	public void testFromJsonSubtype() throws Exception {
		UserId id = new UserId().setValue(123L);
		String s = lineFormat.toJson(id);

		Object id2 = lineFormat.fromJson(Id.Descriptor.getInstance(), s);
		assertTrue(id2 instanceof UserId);
		assertEquals(id, id2);
	}

	@Test
	public void testToFromPolymorphic() throws Exception {
		Id id = new UserId().setValue(789L);
		String s = lineFormat.toJson(id);

		Object id2 = lineFormat.fromJson(Id.Descriptor.getInstance(), s);
		assertTrue(id2 instanceof UserId);
		assertEquals(id, id2);
	}

	@Test
	public void testFromJsonWrongPartial() throws Exception {
		// Was a stack overflow.
		String s = "object";
		Id id = (Id) lineFormat.fromJson(Id.Descriptor.getInstance(), s);
		assertNotNull(id);
	}

	@Test
	public void testParseTokens() throws Exception {
		String s = "ivan--male-{{}-Ivan-Korobkov %7B%7D%2D-{1987-8-7}-123--en---1}-";
		List<Object> tokens = lineFormat.parseTokens(s);

		List<Object> expected = ImmutableList.<Object>of(
				"ivan", "", "male", ImmutableList.of(ImmutableList.of(""),
					"Ivan", "Korobkov %7B%7D%2D", ImmutableList.of("1987", "8", "7"), "123", "",
				"en", "", "", "1"), "");

		assertEquals(expected, tokens);
	}

	@Test(expected = IOException.class)
	public void testParseTokensMalformed() throws Exception {
		String s = "ivan-}{-male}-{{-";
		lineFormat.parseTokens(s);
	}
}
