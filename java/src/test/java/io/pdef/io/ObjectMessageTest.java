package io.pdef.io;

import com.google.common.collect.ImmutableMap;
import io.pdef.test.TestSimpleMessage;
import org.junit.Test;

import java.util.Map;

public class ObjectMessageTest {

	@Test
	public void test() throws Exception {
		Map<String, Object> map = ImmutableMap.<String, Object>of(
				"firstField", true,
				"secondField", "hello, world");
		ObjectMessage input = new ObjectMessage(map);
		TestSimpleMessage msg = new TestSimpleMessage(input);
		System.out.println(msg);
	}
}
