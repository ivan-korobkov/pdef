package pdef.fixtures;

import pdef.SymbolTable;
import pdef.VariableDescriptor;
import pdef.provided.NativeVariableDescriptor;

public class Base<R> extends pdef.generated.GeneratedMessage {
	private static final Base<?> defaultInstance = new Base();

	public static Base<?> getDefaultInstance() {
		return defaultInstance;
	}

	private final R field0;

	protected Base() {
		this(new Builder<R>());
	}

	protected Base(final Builder<R> builder) {
		super(builder);
		this.field0 = builder.getField0();
	}

	public R getField0() {
		return field0;
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

		public R getField0() {
			return field0;
		}

		public Builder<R> setField0(final R value) {
			this.field0 = value;
			return this;
		}

		public Builder<R> clearField0() {
			this.field0 = null;
			return this;
		}

		@Override
		public Base<R> build() {
			return new Base<R>(this);
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
			super(Base.class);
			variableR = new NativeVariableDescriptor("R");
			variables = pdef.ImmutableSymbolTable.of(
					variableR
			);
		}

		@Override
		public SymbolTable<VariableDescriptor> getVariables() {
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
					return ((Base) message).getField0();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getField0();
				}

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
