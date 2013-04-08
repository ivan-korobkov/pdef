package pdef.formats;

import com.google.common.collect.ImmutableList;
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

public class JsonSerializerTest {
	private JsonSerializer serializer;

	@Before
	public void setUp() throws Exception {
		serializer = new JsonSerializer();
	}

	@Test
	public void testSerialize() throws Exception {
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

		User user = User.builder()
				.setName("Ivan Korobkov")
				.setAge(25)
				.setSex(Sex.MALE)
				.setProfile(profile)
				.setFloats(floats)
				.build();

		String s = (String) serializer.serialize(user);
		assertEquals(
				"{\"type\":\"user\",\"name\":\"Ivan Korobkov\",\"age\":25,\"sex\":\"male\","
						+ "\"profile\":{\"firstName\":\"Ivan\",\"lastName\":\"Korobkov\","
						+ "\"birthday\":{\"year\":1987,\"month\":8,\"day\":7},\"avatar\":123,"
						+ "\"preferedLanguage\":\"en\",\"complete\":true},\"floats\":{\"b\":0.0,"
						+ "\"a\":1.2}}",
				s);
	}

	@Test
	public void testSerializeCalls() throws Exception {
		App.Descriptor app = App.Descriptor.getInstance();
		Calc.Descriptor calc = Calc.Descriptor.getInstance();
		MethodDescriptor calcMethod = app.getMethods().map().get("calc");
		MethodDescriptor sumMethod = calc.getMethods().map().get("sum");

		List<Call> calls = ImmutableList.of(
				new Call(calcMethod, ImmutableMap.of()),
				new Call(sumMethod, ImmutableMap.of("i0", 10, "i1", 11)));
		String result = (String) serializer.serializeCalls(calls);

		assertEquals("{\"calc\":{},\"sum\":{\"i0\":10,\"i1\":11}}", result);
	}
}
