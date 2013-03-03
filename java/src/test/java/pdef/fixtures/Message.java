package module;

import pdef.SymbolTable;
import pdef.VariableDescriptor;
import pdef.provided.NativeVariableDescriptor;

public class Message<T> extends pdef.generated.GeneratedMessage {
	private static final Message<?> defaultInstance = new Message();

	public static Message<?> getDefaultInstance() {
		return defaultInstance;
	}

	public static pdef.MessageDescriptor getClassDescriptor() {
		return Descriptor.getInstance();
	}

	private final T field0;
	private final int field1;

	protected Message() {
		this(new Builder<T>());
	}

	protected Message(final Builder<T> builder) {
		super(builder);
		this.field0 = builder.getField0();
		this.field1 = builder.getField1();
	}

	public T getField0() {
		return field0;
	}

	public int getField1() {
		return field1;
	}

	@Override
	public pdef.MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Builder<T> extends pdef.generated.GeneratedMessage.Builder {
		private T field0;
		private int field1;

		public T getField0() {
			return field0;
		}

		public Builder<T> setField0(final T value) {
			this.field0 = value;
			return this;
		}

		public Builder<T> clearField0() {
			this.field0 = null;
			return this;
		}

		public int getField1() {
			return field1;
		}

		public Builder<T> setField1(final int value) {
			this.field1 = value;
			return this;
		}

		public Builder<T> clearField1() {
			this.field1 = 0;
			return this;
		}

		@Override
		public Message<T> build() {
			return new Message<T>(this);
		}

		@Override
		public pdef.MessageDescriptor getDescriptor() {
			return Descriptor.getInstance();
		}
	}

	public static class Descriptor extends pdef.generated.GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();

		public static Descriptor getInstance() {
			return instance;
		}

		private final pdef.VariableDescriptor variableT;
		private final pdef.SymbolTable<pdef.VariableDescriptor> variables;

		private pdef.FieldDescriptor field0Field;
		private pdef.FieldDescriptor field1Field;
		private pdef.SymbolTable<pdef.FieldDescriptor> declaredFields;

		protected Descriptor() {
			super(Message.class);
			variableT = new NativeVariableDescriptor("T");
			variables = pdef.ImmutableSymbolTable.of(
					variableT
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
					variableT) {
				@Override
				public Object get(final pdef.Message message) {
					return ((Message) message).getField0();
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

			field1Field = new pdef.generated.GeneratedFieldDescriptor("field1",
					pdef.provided.NativeValueDescriptors.getInt32()) {
				@Override
				public Object get(final pdef.Message message) {
					return ((Message) message).getField1();
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
						((Builder) builder).setField1((Integer) value);
					}
				}

				@Override
				public void clear(final pdef.Message.Builder builder) {
					((Builder) builder).clearField1();
				}
			};

			declaredFields = pdef.ImmutableSymbolTable.of(
					field0Field,
					field1Field
			);
		}

		static {
			instance.link();
		}
	}
}
