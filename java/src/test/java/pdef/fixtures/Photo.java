package pdef.fixtures;

public class Photo extends pdef.fixtures.Base {
	private static final Photo defaultInstance = new Photo();

	public static Photo getDefaultInstance() {
		return defaultInstance;
	}

	private pdef.fixtures.Base owner;

	protected Photo() {
		this(new Builder());
	}

	protected Photo(final Builder builder) {
		super(builder);
		if (builder.hasOwner()) {
			this.owner = builder.getOwner();
		}
	}

	public pdef.fixtures.Base getOwner() {
		if (!hasOwner()) {
			return null;
		}
		return owner;
	}

	public boolean hasOwner() {
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
		private pdef.fixtures.Base owner;

		public pdef.fixtures.Base getOwner() {
			if (!hasOwner()) {
				return null;
			}
			return owner;
		}

		public boolean hasOwner() {
			return _fields.get(1);
		}

		public Builder setOwner(final pdef.fixtures.Base value) {
			this.owner = value;
			_fields.set(1);
			return this;
		}

		public Builder clearOwner() {
			this.owner = null;
			_fields.clear(1);
			return this;
		}

		@Override
		public Photo build() {
			return new Photo(this);
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

		private pdef.FieldDescriptor ownerField;
		private pdef.SymbolTable<pdef.FieldDescriptor> declaredFields;

		protected Descriptor() {
			super(Photo.class);
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
			baseType = pdef.fixtures.Type.PHOTO;

			ownerField = new pdef.generated.GeneratedFieldDescriptor("owner",
					pdef.fixtures.Base.getClassDescriptor()) {
				@Override
				public Object get(final pdef.Message message) {
					return ((Photo) message).getOwner();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getOwner();
				}

				@Override
				public boolean isSet(final pdef.Message message) {
					return ((Photo) message).hasOwner();
				}

				@Override
				public boolean isSet(final pdef.Message.Builder builder) {
					return ((Builder) builder).hasOwner();
				}

				@Override
				public void set(final pdef.Message.Builder builder, final Object value) {
					if (value == null) {
						clear(builder);
					} else {
						((Builder) builder).setOwner((pdef.fixtures.Base) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearOwner();
				}
			};

			declaredFields = pdef.ImmutableSymbolTable.of(
					ownerField
			);
		}

		static {
			instance.link();
		}
	}
}
