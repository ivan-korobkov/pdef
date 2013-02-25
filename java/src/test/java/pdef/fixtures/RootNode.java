package pdef.fixtures;

import pdef.ImmutableSymbolTable;
import pdef.SymbolTable;
import pdef.descriptors.*;

public class RootNode<R> extends Node<R> {

	@Override
	public MessageDescriptor getDescriptor() {
		return super.getDescriptor();
	}

	public static class Descriptor extends AbstractMessageDescriptor {
		private static final Descriptor INSTANCE = new Descriptor();

		public static Descriptor getInstance() {
			INSTANCE.link();
			return INSTANCE;
		}

		private MessageDescriptor base;
		private VariableDescriptor var0;
		private SymbolTable<VariableDescriptor> variables;
		private SymbolTable<FieldDescriptor> declaredFields;

		Descriptor() {
			super(RootNode.class);

			var0 = new BaseVariableDescriptor("R");
			variables = ImmutableSymbolTable.of(var0);
		}

		@Override
		public MessageDescriptor getBase() {
			return base;
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
			base = Node.Descriptor.getInstance().parameterize(var0);
			declaredFields = ImmutableSymbolTable.of();
		}
	}
}
