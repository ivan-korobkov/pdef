package pdef.fixtures;

public class Photo extends pdef.fixtures.Base {
	private static final Photo defaultInstance = new Photo();

	public static Photo getDefaultInstance() {
		return defaultInstance;
	}


	protected Photo() {
		this(new Builder());
	}

	protected Photo(final Builder builder) {
		super(builder);
	}

	@Override
	public pdef.MessageDescriptor getDescriptor() {
		return Descriptor.instance;
	}

	public static pdef.MessageDescriptor getClassDescriptor() {
		return Descriptor.instance;
	}

	public static class Builder extends pdef.fixtures.Base.Builder {

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

			declaredFields = pdef.ImmutableSymbolTable.of(
			);
		}

		static {
			instance.link();
		}
	}
}
