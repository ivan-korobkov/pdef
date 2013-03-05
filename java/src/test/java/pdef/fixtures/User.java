package pdef.fixtures;

import javax.annotation.Nullable;

public class User extends pdef.fixtures.Base {
	private static final User defaultInstance = new User();

	public static User getDefaultInstance() {
		return defaultInstance;
	}

	private pdef.fixtures.Photo avatar;
	private pdef.fixtures.Base object;

	protected User() {
		this(new Builder());
	}

	protected User(final Builder builder) {
		super(builder);
		if (builder.hasAvatar()) {
			this.avatar = builder.getAvatar();
		}
		if (builder.hasObject()) {
			this.object = builder.getObject();
		}
	}

	public pdef.fixtures.Photo getAvatar() {
		if (!hasAvatar()) {
			return null;
		}
		return avatar;
	}

	public boolean hasAvatar() {
		return _fields.get(1);
	}

	public pdef.fixtures.Base getObject() {
		if (!hasObject()) {
			return null;
		}
		return object;
	}

	public boolean hasObject() {
		return _fields.get(1);
	}

	@Override
	public pdef.MessageDescriptor getDescriptor() {
		return Descriptor.instance;
	}

	public static pdef.MessageDescriptor getClassDescriptor() {
		return Descriptor.instance;
	}

	public static class Builder extends pdef.fixtures.Base.Builder {
		private pdef.fixtures.Photo avatar;
		private pdef.fixtures.Base object;

		public pdef.fixtures.Photo getAvatar() {
			if (!hasAvatar()) {
				return null;
			}
			return avatar;
		}

		public boolean hasAvatar() {
			return _fields.get(1);
		}

		public Builder setAvatar(final pdef.fixtures.Photo value) {
			this.avatar = value;
			_fields.set(1);
			return this;
		}

		public Builder clearAvatar() {
			this.avatar = null;
			_fields.clear(1);
			return this;
		}

		public pdef.fixtures.Base getObject() {
			if (!hasObject()) {
				return null;
			}
			return object;
		}

		public boolean hasObject() {
			return _fields.get(1);
		}

		public Builder setObject(final pdef.fixtures.Base value) {
			this.object = value;
			_fields.set(1);
			return this;
		}

		public Builder clearObject() {
			this.object = null;
			_fields.clear(1);
			return this;
		}

		@Override
		public User build() {
			return new User(this);
		}

		@Override
		public pdef.MessageDescriptor getDescriptor() {
			return Descriptor.instance;
		}
	}

	public static class Descriptor extends pdef.generated.GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();

		private final pdef.SymbolTable<pdef.VariableDescriptor> variables;

		private pdef.MessageDescriptor base;
		private Enum<?> baseType;

		private pdef.FieldDescriptor avatarField;
		private pdef.FieldDescriptor objectField;
		private pdef.SymbolTable<pdef.FieldDescriptor> declaredFields;

		protected Descriptor() {
			super(User.class);
			variables = pdef.ImmutableSymbolTable.of(
			);
		}
		@Override
		public pdef.MessageDescriptor getBase() {
			return base;
		}

		@Override
		public Enum<?> getBaseType() {
			return baseType;
		}

		@Override
		public pdef.SymbolTable<pdef.VariableDescriptor> getVariables() {
			return variables;
		}

		@Override
		public pdef.SymbolTable<pdef.FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		protected void init() {
			base = pdef.fixtures.Base.getClassDescriptor();
			baseType = pdef.fixtures.Type.USER;

			avatarField = new pdef.generated.GeneratedFieldDescriptor("avatar",
					pdef.fixtures.Photo.getClassDescriptor()) {
				@Override
				public Object get(final pdef.Message message) {
					return ((User) message).getAvatar();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getAvatar();
				}

				@Override
				public boolean isSet(final pdef.Message message) {
					return ((User) message).hasAvatar();
				}

				@Override
				public boolean isSet(final pdef.Message.Builder builder) {
					return ((Builder) builder).hasAvatar();
				}

				@Override
				public void set(final pdef.Message.Builder builder, final Object value) {
					if (value == null) {
						clear(builder);
					} else {
						((Builder) builder).setAvatar((pdef.fixtures.Photo) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearAvatar();
				}
			};

			objectField = new pdef.generated.GeneratedFieldDescriptor("object",
					pdef.fixtures.Base.getClassDescriptor()) {
				@Override
				public Object get(final pdef.Message message) {
					return ((User) message).getObject();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getObject();
				}

				@Override
				public boolean isSet(final pdef.Message message) {
					return ((User) message).hasObject();
				}

				@Override
				public boolean isSet(final pdef.Message.Builder builder) {
					return ((Builder) builder).hasObject();
				}

				@Override
				public void set(final pdef.Message.Builder builder, final Object value) {
					if (value == null) {
						clear(builder);
					} else {
						((Builder) builder).setObject((pdef.fixtures.Base) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearObject();
				}
			};

			declaredFields = pdef.ImmutableSymbolTable.of(
					avatarField,
					objectField
			);
		}

		static {
			instance.link();
		}
	}
}
