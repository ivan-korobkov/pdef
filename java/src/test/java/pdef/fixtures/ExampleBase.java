package pdef.fixtures;

import pdef.provided.NativeVariableDescriptor;

public class ExampleBase<R> extends pdef.generated.GeneratedMessage {
	private static final ExampleBase<?> defaultInstance = new ExampleBase();

	public static ExampleBase<?> getDefaultInstance() {
		return defaultInstance;
	}

	private R field0;

	protected ExampleBase() {
		this(new Builder<R>());
	}

	protected ExampleBase(final Builder<R> builder) {
		super(builder);
		if (builder.hasField0()) {
			this.field0 = builder.getField0();
		}
	}

	@javax.annotation.Nullable
	public R getField0() {
		if (!hasField0()) {
			return null;
		}
		return field0;
	}

	public boolean hasField0() {
		return _fields.get(0);
	}

	@Override
	public pdef.MessageDescriptor getDescriptor() {
		return Descriptor.instance;
	}

	public static pdef.MessageDescriptor getClassDescriptor() {
		return Descriptor.instance;
	}

	public static class Builder<R> extends pdef.generated.GeneratedMessage.Builder {
		private R field0;

		@javax.annotation.Nullable
		public R getField0() {
			if (!hasField0()) {
				return null;
			}
			return field0;
		}

		public boolean hasField0() {
			return _fields.get(0);
		}

		public Builder<R> setField0(final R value) {
			this.field0 = value;
			_fields.set(0);
			return this;
		}

		public Builder<R> clearField0() {
			this.field0 = null;
			_fields.clear(0);
			return this;
		}

		@Override
		public ExampleBase<R> build() {
			return new ExampleBase<R>(this);
		}

		@Override
		public pdef.MessageDescriptor getDescriptor() {
			return Descriptor.instance;
		}
	}

	public static class Descriptor extends pdef.generated.GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();

		private final pdef.VariableDescriptor variableR;
		private final pdef.SymbolTable<pdef.VariableDescriptor> variables;

		private pdef.FieldDescriptor field0Field;
		private pdef.SymbolTable<pdef.FieldDescriptor> declaredFields;

		protected Descriptor() {
			super(ExampleBase.class);
			variableR = new NativeVariableDescriptor("R");
			variables = pdef.ImmutableSymbolTable.of(
					variableR
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
		protected void init() {
			field0Field = new pdef.generated.GeneratedFieldDescriptor("field0",
					variableR) {
				@Override
				public Object get(final pdef.Message message) {
					return ((ExampleBase) message).getField0();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getField0();
				}

				@Override
				public boolean isSet(final pdef.Message message) {
					return ((ExampleBase) message).hasField0();
				}

				@Override
				public boolean isSet(final pdef.Message.Builder builder) {
					return ((Builder) builder).hasField0();
				}

				@SuppressWarnings("unchecked")
				@Override
				public void set(final pdef.Message.Builder builder, final Object value) {
					if (value == null) {
						clear(builder);
					} else {
						((Builder) builder).setField0((Object) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearField0();
				}
			};

			declaredFields = pdef.ImmutableSymbolTable.of(
					field0Field
			);
		}

		static {
			instance.link();
		}
	}
}
