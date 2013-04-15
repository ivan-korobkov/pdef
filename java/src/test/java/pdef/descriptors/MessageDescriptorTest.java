package pdef.descriptors;

import org.junit.Test;
import pdef.Message;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MessageDescriptorTest {
	@Test
	public void testConstructor() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		MessageDescriptor descriptor = new MessageDescriptor(User.class, pool);
		assertEquals(User.class, descriptor.getJavaType());
		assertNull(descriptor.getBaseType());
	}

	@Test
	public void testLink() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		MessageDescriptor descriptor = new MessageDescriptor(User.class, pool);
		descriptor.link();

		Map<String, FieldDescriptor> fields = descriptor.getDeclaredFields().map();
		assertEquals(3, fields.size());
		assertTrue(fields.containsKey("name"));
		assertTrue(fields.containsKey("avatar"));
		assertTrue(fields.containsKey("photos"));

		MessageDescriptor image = (MessageDescriptor) pool.getDescriptor(Image.class);
		assertEquals(image, fields.get("avatar").getDescriptor());
	}

	private static class User implements Message {
		private String name;
		private Image avatar;
		private List<Image> photos;

		@Override
		public pdef.MessageDescriptor getDescriptor() {
			return null;
		}

		@Override
		public Builder newBuilderForType() {
			return null;
		}

		@Override
		public Builder toBuilder() {
			return null;
		}
	}

	private static class Image implements Message {
		private String url;
		private User owner;
		private long createdAt;

		@Override
		public pdef.MessageDescriptor getDescriptor() {
			return null;
		}

		@Override
		public Builder newBuilderForType() {
			return null;
		}

		@Override
		public Builder toBuilder() {
			return null;
		}
	}
}
