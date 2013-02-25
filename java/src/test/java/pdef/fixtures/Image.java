package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.descriptors.AbstractFieldDescriptor;
import pdef.descriptors.AbstractMessageDescriptor;
import pdef.descriptors.FieldDescriptor;
import pdef.descriptors.MessageDescriptor;

public class Image extends Entity {
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

		private MessageDescriptor base;
		private SymbolTable<FieldDescriptor> declaredFields;

		Descriptor() {
			super(Image.class);
		}

		@Override
		public MessageDescriptor getBase() {
			return base;
		}

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		protected void init() {
			base = Entity.Descriptor.getInstance();
			declaredFields = ImmutableSymbolTable.<FieldDescriptor>of(
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
