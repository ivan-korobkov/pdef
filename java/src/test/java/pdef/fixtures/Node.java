package pdef.fixtures;

import pdef.*;
import pdef.generated.GeneratedFieldDescriptor;
import pdef.generated.GeneratedMessageDescriptor;
import pdef.provided.NativeVariableDescriptor;

public class Node<T> extends Entity {
	private static final Node<?> defaultInstance = new Node<Object>();
	public static Node<?> getDefaultInstance() {
		return defaultInstance;
	}

	private RootNode<T> root;
	private T element;

	protected Node() {
		super();
	}

	protected Node(final Builder<T> builder) {
		init(builder);
	}

	protected void init(final Builder<T> builder) {
		super.init(builder);
		this.root = builder.getRoot();
		this.element = builder.getElement();
	}

	public RootNode<T> getRoot() { return root; }

	public T getElement() { return element; }

	@Override
	public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }

	public static class Builder<T> extends Entity.Builder {
		private RootNode<T> root = (RootNode<T>) RootNode.getDefaultInstance();
		private T element = null;

		public RootNode<T> getRoot() { return root; }

		public Builder<T> setRoot(final RootNode<T> root) { this.root = root; return this; }

		public T getElement() { return element; }

		public Builder<T> setElement(final T element) { this.element = element; return this; }

		@Override
		public Node<T> build() { return new Node<T>(this); }

		@Override
		public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }
	}

	public static class Descriptor extends GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();
		public static Descriptor getInstance() { return instance; }

		private final VariableDescriptor var0;
		private final SymbolTable<VariableDescriptor> variables;
		private SymbolTable<FieldDescriptor> declaredFields;
		private FieldDescriptor rootField;
		private FieldDescriptor elementField;

		Descriptor() {
			super(Node.class);

			var0 = new NativeVariableDescriptor("T");
			variables = ImmutableSymbolTable.of(var0);
		}

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return variables; }

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() { return declaredFields; }

		@Override
		protected void init() {
			rootField = new GeneratedFieldDescriptor("root",
					RootNode.Descriptor.getInstance().parameterize(var0)) {
				@Override
				public Object get(final Message message) {
					return ((Node) message).getRoot();
				}

				@Override
				public Object get(final Message.Builder builder) {
					return ((Builder) builder).getRoot();
				}

				@Override
				@SuppressWarnings("unchecked")
				public void set(final Message.Builder builder, final Object value) {
					((Builder) builder).setRoot((RootNode<?>) value);
				}
			};

			elementField = new GeneratedFieldDescriptor("element", var0) {
				@Override
				public Object get(final Message message) {
					return ((Node) message).getElement();
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

			declaredFields = ImmutableSymbolTable.of(rootField, elementField);
		}

		@Override
		public Node<?> getDefaultInstance() {
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
