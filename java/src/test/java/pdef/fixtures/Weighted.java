package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.descriptors.*;

public class Weighted<T> implements Message {

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
	public MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Descriptor extends AbstractMessageDescriptor {
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

			var0 = new BaseVariableDescriptor("T");
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
					new AbstractFieldDescriptor("element", var0) {
						@Override
						public Object get(final Message message) {
							return ((Weighted<?>) message).getElement();
						}

						@Override
						@SuppressWarnings("unchecked")
						public void set(final Message message, final Object value) {
							((Weighted<Object>) message).setElement(value);
						}
					},

					new AbstractFieldDescriptor("weight", IntDescriptor.getInstance()) {
						@Override
						public Object get(final Message message) {
							return ((Weighted<?>) message).getWeight();
						}

						@Override
						public void set(final Message message, final Object value) {
							((Weighted) message).setWeight((Integer) value);
						}
					}
			);
		}
	}
}
