package io.pdef.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import io.pdef.descriptors.DefaultDescriptorPool;
import io.pdef.descriptors.DescriptorPool;
import io.pdef.descriptors.InterfaceDescriptor;
import io.pdef.fixtures.App;
import io.pdef.fixtures.Calc;
import io.pdef.fixtures.Image;
import io.pdef.fixtures.User;
import io.pdef.invocation.Invocation;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JsonSerializerTest {
	private DescriptorPool pool;
	private JsonSerializer serializer;

	@Before
	public void setUp() throws Exception {
		pool = new DefaultDescriptorPool();
		ObjectMapper mapper = new ObjectMapper();
		serializer = new JsonSerializer(pool, mapper);
	}

	@Test
	public void testSerialize() throws Exception {
		Image image = new Image.Builder()
				.setUrl("http://example.com/image.jpg")
				.setCreatedAt(1234)
				.build();

		User user = new User.Builder()
				.setName("John Doe")
				.setAvatar(image)
				.build();

		String s = serializer.serialize(user);
		assertEquals("{\"type\":\"user\",\"name\":\"John Doe\",\"avatar\":{\"type\":\"image\","
				+ "\"url\":\"http://example.com/image.jpg\",\"owner\":null,\"createdAt\":1234},"
				+ "\"photos\":null}", s);
	}

	@Test
	public void testSerializeInvocations() throws Exception {
		InterfaceDescriptor app = (InterfaceDescriptor) pool.getDescriptor(App.class);
		InterfaceDescriptor calc = (InterfaceDescriptor) pool.getDescriptor(Calc.class);
		List<Invocation> invocations = ImmutableList.of(
				new Invocation(app.getMethods().get("calc"), Arrays.asList()),
				new Invocation(calc.getMethods().get("sum"), Arrays.asList(3, 4)));

		String s = serializer.serializeInvocations(invocations);
		assertEquals("{\"calc\":[],\"sum\":[3,4]}", s);
	}
}
