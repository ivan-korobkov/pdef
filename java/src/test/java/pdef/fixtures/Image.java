package pdef.fixtures;

import pdef.Message;
import pdef.descriptors.AbstractFieldDescriptor;
import pdef.descriptors.AbstractMessageDescriptor;
import pdef.descriptors.MessageDescriptor;

public class Image implements Message {
	private User user;

	public User getUser() {
		return user;
	}

	public Image setUser(final User user) {
		this.user = user;
		return this;
	}

	@Override
	public MessageDescriptor getDescriptor() {
		return Image.Descriptor.getInstance();
	}

	public static class Descriptor extends AbstractMessageDescriptor {
		private static final Descriptor INSTANCE = new Descriptor();
		public static Descriptor getInstance() {
			INSTANCE.link();
			return INSTANCE;
		}

		private Descriptor() {}

		@Override
		protected void doLink() {
			setDeclaredFields(
					new AbstractFieldDescriptor("user", User.Descriptor.getInstance()) {
						@Override
						public Object get(final Message message) {
							return ((User) message).getImage();
						}

						@Override
						public void set(final Message message, final Object value) {
							((User) message).setImage((Image) value);
						}
					}
			);
		}
	}
}
