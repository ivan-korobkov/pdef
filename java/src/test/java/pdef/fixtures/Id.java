package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.PdefMessage;
import pdef.SymbolTable;
import pdef.generated.GeneratedFieldDescriptor;
import pdef.generated.GeneratedMessageDescriptor;
import pdef.descriptors.FieldDescriptor;
import pdef.descriptors.MessageDescriptor;

public class Id implements PdefMessage {
	private int value;

	public int getValue() {
		return value;
	}

	public Id setValue(final int value) {
		this.value = value;
		return this;
	}

	@Override
	public MessageDescriptor getPdefDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Descriptor extends GeneratedMessageDescriptor {
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
					new GeneratedFieldDescriptor("value", IntDescriptor.getInstance()) {
						@Override
						public Object get(final PdefMessage message) {
							return ((Id) message).getValue();
						}

						@Override
						public void set(final PdefMessage message, final Object value) {
							((Id) message).setValue((Integer) value);
						}
					}
			);
		}
	}
}
