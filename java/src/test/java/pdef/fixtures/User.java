package pdef.fixtures;

import pdef.Message;
import pdef.descriptors.AbstractFieldDescriptor;
import pdef.descriptors.AbstractMessageDescriptor;
import pdef.descriptors.MessageDescriptor;

public class User implements Message {
	private Image image;

	public Image getImage() {
		return image;
	}

	public User setImage(final Image image) {
		this.image = image;
		return this;
	}

	@Override
	public MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
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
					new AbstractFieldDescriptor("avatar", Image.Descriptor.getInstance()) {
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
