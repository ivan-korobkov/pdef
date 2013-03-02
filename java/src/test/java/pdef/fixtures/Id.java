package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.generated.GeneratedFieldDescriptor;
import pdef.generated.GeneratedMessage;
import pdef.generated.GeneratedMessageDescriptor;
import pdef.FieldDescriptor;
import pdef.MessageDescriptor;
import pdef.provided.NativeValueDescriptors;

public class Id extends GeneratedMessage {
	private static final Id defaultInstance = new Id();
	public static Id getDefaultInstance() {
		return defaultInstance;
	}

	private int value;

	protected Id() {}

	protected Id(final Builder builder) {
		init(builder);
	}

	protected void init(final Builder builder) {
		super.init(builder);
		value = builder.getValue();
	}

	public int getValue() { return value; }

	private void setValue(final int value) { this.value = value; }

	@Override
	public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }

	public static class Builder extends GeneratedMessage.Builder {
		private int value = 0;

		public int getValue() { return value; }

		public Builder setValue(final int value) { this.value = value; return this; }

		@Override
		public Id build() { return new Id(this); }

		@Override
		public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }
	}

	public static class Descriptor extends GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();
		public static Descriptor getInstance() { return instance; }

		private SymbolTable<FieldDescriptor> declaredFields;
		private FieldDescriptor valueField;

		protected Descriptor() { super(Id.class); }

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() { return declaredFields; }

		@Override
		protected void init() {
			valueField = new GeneratedFieldDescriptor("value",
					NativeValueDescriptors.getInt32()) {
				@Override
				public Object get(final Message message) {
					return ((Id) message).getValue();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder) builder).getValue();
				}

				@Override
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setValue((Integer) value);
				}
			};


			declaredFields = ImmutableSymbolTable.of(valueField);
		}

		@Override
		public Id getDefaultInstance() { return defaultInstance; }

		static {
			instance.link();
		}
	}

	static {
		defaultInstance.init(new Builder());
	}
}
