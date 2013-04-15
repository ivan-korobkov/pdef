package io.pdef.descriptors;

import io.pdef.fixtures.Image;
import io.pdef.fixtures.User;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MessageDescriptorTest {
	@Test
	public void testConstructor() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		MessageDescriptor descriptor = new MessageDescriptor(User.class, pool);
		assertEquals(User.class, descriptor.getJavaType());
	}

	@Test
	public void testLink() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		MessageDescriptor descriptor = new MessageDescriptor(User.class, pool);
		descriptor.link();

		Map<String, FieldDescriptor> fields = descriptor.getDeclaredFields();
		assertEquals(3, fields.size());
		assertTrue(fields.containsKey("name"));
		assertTrue(fields.containsKey("avatar"));
		assertTrue(fields.containsKey("photos"));

		MessageDescriptor image = (MessageDescriptor) pool.getDescriptor(Image.class);
		assertEquals(image, fields.get("avatar").getType());
	}
}
