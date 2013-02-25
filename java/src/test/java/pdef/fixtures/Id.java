package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.descriptors.BaseFieldDescriptor;
import pdef.descriptors.BaseMessageDescriptor;
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

	public static class Descriptor extends BaseMessageDescriptor {
		private static final Descriptor INSTANCE = new Descriptor();

		public static Descriptor getInstance() {
			INSTANCE.link();
			return INSTANCE;
		}

		private SymbolTable<FieldDescriptor> declaredFields;

		Descriptor() {
			super(Id.class);
		}

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		protected void init() {
			declaredFields = ImmutableSymbolTable.<FieldDescriptor>of(
					new BaseFieldDescriptor("value", IntDescriptor.getInstance()) {
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
		}
	}
}
