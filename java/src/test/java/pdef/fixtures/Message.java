package module;

import pdef.MessageDescriptor;

public class Message extends pdef.generated.GeneratedMessage {
	private static final Message defaultInstance = new Message();

	public static Message getDefaultInstance() {
		return defaultInstance;
	}

	private final int field1;
	private final String field2;

	protected Message() {
		this(new Builder());
	}

	protected Message(final Builder builder) {
		super(builder);
		this.field1 = builder.getField1();
		this.field2 = builder.getField2();
	}

	public int getField1() {
		return field1;
	}

	public String getField2() {
		return field2;
	}

	@Override
	public pdef.MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Builder extends pdef.generated.GeneratedMessage.Builder {
		private int field1;
		private String field2;

		public int getField1() {
			return field1;
		}

		public Builder setField1(final int value) {
			this.field1 = value;
			return this;
		}

		public Builder clearField1() {
			this.field1 = 0;
			return this;
		}

		public String getField2() {
			return field2;
		}

		public Builder setField2(final String value) {
			this.field2 = value;
			return this;
		}

		public Builder clearField2() {
			this.field2 = null;
			return this;
		}

		@Override
		public Message build() {
			return new Message(this);
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

		private pdef.FieldDescriptor field1Field;
		private pdef.FieldDescriptor field2Field;
		private pdef.SymbolTable<pdef.FieldDescriptor> declaredFields;

		protected Descriptor() {
			super(Message.class);
		}

		@Override
		public pdef.SymbolTable<pdef.FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		protected void init() {
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

			field2Field = new pdef.generated.GeneratedFieldDescriptor("field2",
					pdef.provided.NativeValueDescriptors.getString()) {
				@Override
				public Object get(final pdef.Message message) {
					return ((Message) message).getField2();
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
						((Builder) builder).setField2((String) value);
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
