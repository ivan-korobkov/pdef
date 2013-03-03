package pdef.fixtures;

import pdef.SymbolTable;
import pdef.VariableDescriptor;
import pdef.provided.NativeVariableDescriptor;

public class Example<T> extends Base<T> {
	private static final Example<?> defaultInstance = new Example();

	public static Example<?> getDefaultInstance() {
		return defaultInstance;
	}

	private final T field1;
	private final int field2;

	protected Example() {
		this(new Builder<T>());
	}

	protected Example(final Builder<T> builder) {
		super(builder);
		this.field1 = builder.getField1();
		this.field2 = builder.getField2();
	}

	public T getField1() {
		return field1;
	}

	public int getField2() {
		return field2;
	}

	@Override
	public pdef.MessageDescriptor getDescriptor() {
		return Descriptor.instance;
	}

	public static pdef.MessageDescriptor getClassDescriptor() {
		return Descriptor.instance;
	}

	public static class Builder<T> extends Base.Builder<T> {
		private T field1;
		private int field2;

		public T getField1() {
			return field1;
		}

		public Builder<T> setField1(final T value) {
			this.field1 = value;
			return this;
		}

		public Builder<T> clearField1() {
			this.field1 = null;
			return this;
		}

		public int getField2() {
			return field2;
		}

		public Builder<T> setField2(final int value) {
			this.field2 = value;
			return this;
		}

		public Builder<T> clearField2() {
			this.field2 = 0;
			return this;
		}

		@Override
		public Example<T> build() {
			return new Example<T>(this);
		}

		@Override
		public pdef.MessageDescriptor getDescriptor() {
			return Descriptor.instance;
		}
	}

	public static class Descriptor extends pdef.generated.GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();

		private final pdef.VariableDescriptor variableT;
		private final pdef.SymbolTable<pdef.VariableDescriptor> variables;

		private pdef.MessageDescriptor base;
		private pdef.FieldDescriptor field1Field;
		private pdef.FieldDescriptor field2Field;
		private pdef.SymbolTable<pdef.FieldDescriptor> declaredFields;

		protected Descriptor() {
			super(Example.class);
			variableT = new NativeVariableDescriptor("T");
			variables = pdef.ImmutableSymbolTable.of(
					variableT
			);
		}

		@Override
		public pdef.MessageDescriptor getBase() {
			return base;
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
			base = Base.getClassDescriptor();

			field1Field = new pdef.generated.GeneratedFieldDescriptor("field1",
					variableT) {
				@Override
				public Object get(final pdef.Message message) {
					return ((Example) message).getField1();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getField1();
				}

				@Override
				public void set(final pdef.Message.Builder builder, final Object value) {
					if (value == null) {
						clear(builder);
					} else {
						((Builder) builder).setField1((Object) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearField1();
				}
			};

			field2Field = new pdef.generated.GeneratedFieldDescriptor("field2",
					pdef.provided.NativeValueDescriptors.getInt32()) {
				@Override
				public Object get(final pdef.Message message) {
					return ((Example) message).getField2();
				}

				@Override
				public Object get(final pdef.Message.Builder builder) {
					return ((Builder) builder).getField2();
				}

				@Override
				public void set(final pdef.Message.Builder builder, final Object value) {
					if (value == null) {
						clear(builder);
					} else {
						((Builder) builder).setField2((Integer) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearField2();
				}
			};

			declaredFields = pdef.ImmutableSymbolTable.of(
					field1Field,
					field2Field
			);
		}

		static {
			instance.link();
		}
	}
}
