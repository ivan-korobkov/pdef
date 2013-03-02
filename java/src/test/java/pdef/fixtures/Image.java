package pdef.fixtures;

import pdef.*;
import pdef.generated.GeneratedFieldDescriptor;
import pdef.generated.GeneratedMessageDescriptor;

public class Image extends Entity {
	private static Image defaultInstance = new Image(new Builder());
	public static Image getDefaultInstance() { return defaultInstance; }

	private User user;

	public User getUser() { return user; }

	protected Image(final Builder builder) {
		super(builder);
		this.user = builder.getUser();
	}

	@Override
	public MessageDescriptor getDescriptor() { return Image.Descriptor.getInstance(); }

	public static class Builder extends Entity.Builder {
		private User user;

		public User getUser() { return user; }

		public Builder setUser(final User user) { this.user = user; return this; }

		@Override
		public Builder setId(final Id id) { super.setId(id); return this; }

		@Override
		public Image build() { return new Image(this); }
	}

	public static class Descriptor extends GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();
		public static Descriptor getInstance() { instance.link(); return instance; }

		private MessageDescriptor base;
		private SymbolTable<FieldDescriptor> declaredFields;
		private FieldDescriptor userField;

		Descriptor() { super(Image.class); }

		@Override
		public MessageDescriptor getBase() { return base; }

		@Override
		public Enum<?> getBaseType() { return Type.IMAGE; }

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() { return declaredFields; }

		@Override
		protected void init() {
			base = Entity.Descriptor.getInstance();
			userField = new GeneratedFieldDescriptor("user", User.Descriptor.getInstance()) {
				@Override
				public Object get(final Message message) {
					return ((Image) message).getUser();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder) builder).getUser();
				}

				@Override
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setUser((User) value);
				}
			};

			declaredFields = ImmutableSymbolTable.of(userField);
		}
	}
}
