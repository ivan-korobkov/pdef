package pdef.fixtures;

public class Base extends pdef.generated.GeneratedMessage {
	private static final Base defaultInstance = new Base();

	public static Base getDefaultInstance() {
		return defaultInstance;
	}

	private pdef.fixtures.Type discriminator;

	protected Base() {
		this(new Builder());
	}

	protected Base(final Builder builder) {
		super(builder);
		if (builder.hasDiscriminator()) {
			this.discriminator = builder.getDiscriminator();
		}
	}

	public pdef.fixtures.Type getDiscriminator() {
		if (!hasDiscriminator()) {
			return null;
		}
		return discriminator;
	}

	public boolean hasDiscriminator() {
		return _fields.get(0);
	}

	@Override
	public pdef.MessageDescriptor getDescriptor() {
		return Descriptor.instance;
	}

	public static pdef.MessageDescriptor getClassDescriptor() {
		return Descriptor.instance;
	}

	public static class Builder extends pdef.generated.GeneratedMessage.Builder {
		private pdef.fixtures.Type discriminator;

		public pdef.fixtures.Type getDiscriminator() {
			if (!hasDiscriminator()) {
				return null;
			}
			return discriminator;
		}

		public boolean hasDiscriminator() {
			return _fields.get(0);
		}

		public Builder setDiscriminator(final pdef.fixtures.Type value) {
			this.discriminator = value;
			_fields.set(0);
			return this;
		}

		public Builder clearDiscriminator() {
			this.discriminator = null;
			_fields.clear(0);
			return this;
		}

		@Override
		public Base build() {
			return new Base(this);
		}

		@Override
		public pdef.MessageDescriptor getDescriptor() {
			return Descriptor.instance;
		}
	}

	public static class Descriptor extends pdef.generated.GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();

		private final pdef.SymbolTable<pdef.VariableDescriptor> variables;

		private Enum<?> defaultType;
		private pdef.FieldDescriptor typeField;
		private java.util.Map<Enum<?>, pdef.MessageDescriptor> subtypes;

		private pdef.FieldDescriptor discriminatorField;
		private pdef.SymbolTable<pdef.FieldDescriptor> declaredFields;

		protected Descriptor() {
			super(Base.class);
			variables = pdef.ImmutableSymbolTable.of(
			);
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
		public pdef.FieldDescriptor getTypeField() {
			return typeField;
		}

		@Override
		public Enum<?> getDefaultType() {
			return defaultType;
		}

		@Override
		public java.util.Map<Enum<?>, pdef.MessageDescriptor> getSubtypes() {
			return subtypes;
		}

		@Override
		protected void init() {

			discriminatorField = new pdef.generated.GeneratedFieldDescriptor("discriminator",
					pdef.fixtures.Type.getClassDescriptor()) {
				@Override
				public Object get(final pdef.Message message) {
					return ((Base) message).getDiscriminator();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getDiscriminator();
				}

				@Override
				public boolean isSet(final pdef.Message message) {
					return ((Base) message).hasDiscriminator();
				}

				@Override
				public boolean isSet(final pdef.Message.Builder builder) {
					return ((Builder) builder).hasDiscriminator();
				}

				@Override
				public void set(final pdef.Message.Builder builder, final Object value) {
					if (value == null) {
						clear(builder);
					} else {
						((Builder) builder).setDiscriminator((pdef.fixtures.Type) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearDiscriminator();
				}
			};

			declaredFields = pdef.ImmutableSymbolTable.of(
					discriminatorField
			);

			typeField = discriminatorField;
			defaultType = pdef.fixtures.Type.BASE;
			subtypes = com.google.common.collect.ImmutableMap.<Enum<?>, pdef.MessageDescriptor>builder()
					.put(pdef.fixtures.Type.BASE, pdef.fixtures.Base.getClassDescriptor())
					.put(pdef.fixtures.Type.PHOTO, pdef.fixtures.Photo.getClassDescriptor())
					.put(pdef.fixtures.Type.USER, pdef.fixtures.User.getClassDescriptor())
					.build();
		}

		static {
			instance.link();
		}
	}
}
