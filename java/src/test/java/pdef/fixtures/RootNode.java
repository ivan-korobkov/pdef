package pdef.fixtures;

import pdef.*;
import pdef.provided.NativeVariableDescriptor;
import pdef.generated.GeneratedMessageDescriptor;

public class RootNode<R> extends Node<R> {
	private static final RootNode<?> defaultInstance = new RootNode<Object>();
	public static RootNode<?> getDefaultInstance() {
		return defaultInstance;
	}

	protected RootNode() {
		super();
	}

	protected RootNode(final Builder<R> builder) {
		init(builder);
	}

	protected void init(final Builder<R> builder) {
		super.init(builder);
	}

	@Override
	public MessageDescriptor getDescriptor() { return super.getDescriptor(); }

	public static class Builder<R> extends Node.Builder<R> {
		@Override
		public Builder<R> setRoot(final RootNode<R> root) {
			super.setRoot(root);
			return this;
		}

		@Override
		public Builder<R> setElement(final R element) {
			super.setElement(element);
			return this;
		}

		@Override
		public RootNode<R> build() { return new RootNode<R>(this); }

		@Override
		public MessageDescriptor getDescriptor() { return Descriptor.getInstance(); }
	}

	public static class Descriptor extends GeneratedMessageDescriptor {
		private static final Descriptor instance = new Descriptor();
		public static Descriptor getInstance() { return instance; }

		private MessageDescriptor base;
		private VariableDescriptor var0;
		private SymbolTable<VariableDescriptor> variables;
		private SymbolTable<FieldDescriptor> declaredFields;

		Descriptor() {
			super(RootNode.class);

			var0 = new NativeVariableDescriptor("R");
			variables = ImmutableSymbolTable.of(var0);
		}

		@Override
		public MessageDescriptor getBase() { return base; }

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return variables; }

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() { return declaredFields; }

		@Override
		protected void init() {
			base = Node.Descriptor.getInstance().parameterize(var0);
			declaredFields = ImmutableSymbolTable.of();
		}

		@Override
		public RootNode<?> getDefaultInstance() {
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
