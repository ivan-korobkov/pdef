package pdef.json;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JsonTest {
	@Test
	public void testParse_map() throws Exception {
		String s = "{\"hello\": \"world\", \"float\": 1.23, \"int\": 123}";
		Object result = Json.parse(s);
		assertEquals(ImmutableMap.of("hello", "world", "float", 1.23d, "int", 123L), result);
	}

	@Test
	public void testParse_array() throws Exception {
		String s = "[123, 456, 5.5, \"hello\"]";
		Object result = Json.parse(s);
		assertEquals(ImmutableList.of(123L, 456L, 5.5d, "hello"), result);
	}

	@Test
	public void testParse_complex() throws Exception {
		String s = "[{\n"
				+ "    \"null\": null,\n"
				+ "    \"true\": true,\n"
				+ "    \"false\": false,\n"
				+ "    \"string\": \"hello\",\n"
				+ "    \"int\": 123,\n"
				+ "    \"float\": 0.5,\n"
				+ "    \"object\": {\"emptyArray\": []},\n"
				+ "    \"emptyObject\": {}\n"
				+ "}]";
		Object result = Json.parse(s);
		Map<String, Object> map = Maps.newLinkedHashMap();
		map.put("null", null);
		map.put("true", true);
		map.put("false", false);
		map.put("string", "hello");
		map.put("int", 123L);
		map.put("float", 0.5d);
		map.put("object", ImmutableMap.of("emptyArray", ImmutableList.of()));
		map.put("emptyObject", ImmutableMap.of());
		assertEquals(ImmutableList.of(map), result);
	}

	@Test(expected = JsonException.class)
	public void testParse_wrongJson() throws Exception {
		Json.parse("hello, world");
	}

	@Test
	public void testSerialize_map() throws Exception {
		Map<?, ?> map = ImmutableMap.of("hello", "world", "float", 1.23d, "int", 123L);
		String s = Json.serialize(map, false);
		assertEquals("{\"hello\":\"world\",\"float\":1.23,\"int\":123}", s);
	}

	@Test
	public void testSerialize_array() throws Exception {
		Object result = Json.serialize(ImmutableList.of(123L, 456L, 5.5d, "hello"), false);
		assertEquals("[123,456,5.5,\"hello\"]", result);
	}

	@Test
	public void testSerialize_complex() throws Exception {
		Map<String, Object> map = Maps.newLinkedHashMap();
		map.put("null", null);
		map.put("true", true);
		map.put("false", false);
		map.put("string", "hello");
		map.put("int", 123L);
		map.put("float", 0.5d);
		map.put("object", ImmutableMap.of("emptyArray", ImmutableList.of()));
		map.put("emptyObject", ImmutableMap.of());

		String s = Json.serialize(ImmutableList.of(map), false);
		String expected = "[{\"null\":null,\"true\":true,\"false\":false,\"string\":\"hello\","
				+ "\"int\":123,\"float\":0.5,\"object\":{\"emptyArray\":[]},\"emptyObject\":{}}]";
		assertEquals(expected, s);
	}

	@Test(expected = JsonException.class)
	public void testSerialize_unsupported() throws Exception {
		Object object = new Object();
		Json.serialize(object);
	}

	@Test
	public void testSerialize_indent() throws Exception {
		Map<String, String> map = ImmutableMap.of("key", "value");
		String s = Json.serialize(map, true);
		assertEquals("{\n  \"key\" : \"value\"\n}", s);
	}
}
