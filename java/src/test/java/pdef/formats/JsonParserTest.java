package pdef.formats;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import pdef.MethodDescriptor;
import pdef.fixtures.*;
import pdef.fixtures.interfaces.App;
import pdef.fixtures.interfaces.Calc;
import pdef.rpc.Call;

import java.util.List;
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

	@Test
	public void testParseCalls() throws Exception {
		String s = "{\"calc\":{},\"sum\":{\"i0\":10,\"i1\":11}}";
		App.Descriptor app = App.Descriptor.getInstance();
		Calc.Descriptor calc = Calc.Descriptor.getInstance();
		MethodDescriptor calcMethod = app.getMethods().map().get("calc");
		MethodDescriptor sumMethod = calc.getMethods().map().get("sum");

		List<Call> calls = parser.parseCalls(app, s);
		Call call0 = calls.get(0);
		Call call1 = calls.get(1);

		assertEquals(2, calls.size());
		assertEquals(calcMethod, call0.getMethod());
		assertEquals(sumMethod, call1.getMethod());
		assertEquals(ImmutableMap.of(), call0.getArgs());
		assertEquals(ImmutableMap.of("i0", 10, "i1", 11), call1.getArgs());
	}
}
