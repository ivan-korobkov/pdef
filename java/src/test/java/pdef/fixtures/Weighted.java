package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.PdefMessage;
import pdef.SymbolTable;
import pdef.descriptors.*;
import pdef.provided.NativeVariableDescriptor;
import pdef.generated.GeneratedFieldDescriptor;
import pdef.generated.GeneratedMessageDescriptor;

public class Weighted<T> implements PdefMessage {

	private T element;
	private int weight;

	public T getElement() {
		return element;
	}

	public Weighted<T> setElement(final T element) {
		this.element = element;
		return this;
	}

	public int getWeight() {
		return weight;
	}

	public Weighted<T> setWeight(final int weight) {
		this.weight = weight;
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

		private final VariableDescriptor var0;
		private final SymbolTable<VariableDescriptor> variables;
		private SymbolTable<FieldDescriptor> declaredFields;

		Descriptor() {
			super(Weighted.class);

			var0 = new NativeVariableDescriptor("T");
			variables = ImmutableSymbolTable.of(var0);
		}

		@Override
		public SymbolTable<VariableDescriptor> getVariables() {
			return variables;
		}

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		protected void init() {
			declaredFields = ImmutableSymbolTable.<FieldDescriptor>of(
					new GeneratedFieldDescriptor("element", var0) {
						@Override
						public Object get(final PdefMessage message) {
							return ((Weighted<?>) message).getElement();
						}

						@Override
						@SuppressWarnings("unchecked")
						public void set(final PdefMessage message, final Object value) {
							((Weighted<Object>) message).setElement(value);
						}
					},

					new GeneratedFieldDescriptor("weight", IntDescriptor.getInstance()) {
						@Override
						public Object get(final PdefMessage message) {
							return ((Weighted<?>) message).getWeight();
						}

						@Override
						public void set(final PdefMessage message, final Object value) {
							((Weighted) message).setWeight((Integer) value);
						}
					}
			);
		}
	}
}
