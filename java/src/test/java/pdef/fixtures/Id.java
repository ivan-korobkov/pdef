package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.descriptors.AbstractFieldDescriptor;
import pdef.descriptors.AbstractMessageDescriptor;
import pdef.descriptors.FieldDescriptor;
import pdef.descriptors.MessageDescriptor;

public class Id implements Message {
	private int value;

	public int getValue() {
		return value;
	}

	public Id setValue(final int value) {
		this.value = value;
		return this;
	}

	@Override
	public MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Descriptor extends AbstractMessageDescriptor {
		private static final Descriptor INSTANCE = new Descriptor();

		public static Descriptor getInstance() {
			INSTANCE.link();
			return INSTANCE;
		}

		private ImmutableSymbolTable<FieldDescriptor> declaredFields;
		private ImmutableSymbolTable<FieldDescriptor> fields;

		private Descriptor() {
			super(Id.class);
		}

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		public SymbolTable<FieldDescriptor> getFields() {
			return fields;
		}

		@Override
		protected void doLink() {
			declaredFields = ImmutableSymbolTable.<FieldDescriptor>of(
					new AbstractFieldDescriptor("value", IntDescriptor.getInstance()) {
						@Override
						public Object get(final Message message) {
							return ((Id) message).getValue();
						}

						@Override
						public void set(final Message message, final Object value) {
							((Id) message).setValue((Integer) value);
						}
					}
			);
			fields = declaredFields;
		}
	}
}
