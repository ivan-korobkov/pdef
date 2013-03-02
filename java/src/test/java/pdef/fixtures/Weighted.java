package pdef.fixtures;

import pdef.*;
import pdef.generated.GeneratedFieldDescriptor;
import pdef.generated.GeneratedMessage;
import pdef.generated.GeneratedMessageDescriptor;
import pdef.provided.NativeValueDescriptors;
import pdef.provided.NativeVariableDescriptor;

public class Weighted<T> extends GeneratedMessage {
	private static final Weighted<?> defaultInstance = new Weighted<Object>();
	public static Weighted<?> getDefaultInstance() {
		return defaultInstance;
	}

	private T element;
	private int weight;

	protected Weighted() {}

	protected Weighted(final Builder<T> builder) {
		init(builder);
	}

	protected void init(final Builder<T> builder) {
		super.init(builder);
		element = builder.getElement();
		weight = builder.getWeight();
	}

	public T getElement() { return element; }

	public int getWeight() { return weight; }

	@Override
	public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }

	public static class Builder<T> extends GeneratedMessage.Builder {
		private T element = null;
		private int weight = 0;

		public T getElement() { return element; }

		public Builder<T> setElement(final T element) { this.element = element; return this; }

		public int getWeight() { return weight; }

		public Builder<T> setWeight(final int weight) { this.weight = weight; return this; }

		@Override
		public Weighted<T> build() { return new Weighted<T>(this); }

		@Override
		public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }
	}

	public static class Descriptor extends GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();
		public static Descriptor getInstance() { return instance; }

		private final VariableDescriptor var0;
		private final SymbolTable<VariableDescriptor> variables;
		private SymbolTable<FieldDescriptor> declaredFields;
		private FieldDescriptor elementField;
		private FieldDescriptor weightField;

		Descriptor() {
			super(Weighted.class);

			var0 = new NativeVariableDescriptor("T");
			variables = ImmutableSymbolTable.of(var0);
		}

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return variables; }

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() { return declaredFields; }

		@Override
		protected void init() {
			elementField = new GeneratedFieldDescriptor("element", var0) {
				@Override
				public Object get(final Message message) {
					return ((Weighted<?>) message).getElement();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder) builder).getElement();
				}

				@Override
				@SuppressWarnings("unchecked")
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setElement(value);
				}
			};

			weightField = new GeneratedFieldDescriptor("weight",
					NativeValueDescriptors.getInt32()) {
				@Override
				public Object get(final Message message) {
					return ((Weighted<?>) message).getWeight();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder<?>) builder).getWeight();
				}

				@Override
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setWeight((Integer) value);
				}
			};

			declaredFields = ImmutableSymbolTable.of(elementField, weightField);
		}

		@Override
		public Weighted<?> getDefaultInstance() {
			return defaultInstance;
		}

		static {
			instance.link();
		}
	}

	static {
		defaultInstance.init(new Builder<Object>());
	}
}
