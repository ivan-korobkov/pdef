package pdef.rpc;

import com.google.common.collect.ImmutableMap;
import pdef.*;
import pdef.generated.GeneratedInterfaceDescriptor;
import pdef.generated.GeneratedMethodDescriptor;
import pdef.provided.NativeVariableDescriptor;

import java.util.Map;

public interface GenericCollection<Id, T> extends Interface {

	T get(Id id);

	public static class Descriptor extends GeneratedInterfaceDescriptor {
		private static final Descriptor instance = new Descriptor();

		public static Descriptor getInstance() {
			instance.initialize();
			return instance;
		}

		private final VariableDescriptor variableId;
		private final VariableDescriptor variableT;
		private final SymbolTable<VariableDescriptor> variables;

		private MethodDescriptor getMethod;
		private SymbolTable<MethodDescriptor> declaredMethods;
		private SymbolTable<MethodDescriptor> methods;

		protected Descriptor() {
			super(GenericCollection.class);

			variableId = new NativeVariableDescriptor("Id");
			variableT = new NativeVariableDescriptor("T");
			variables = ImmutableSymbolTable.of(variableId, variableT);
		}

		@Override
		public SymbolTable<VariableDescriptor> getVariables() {
			return variables;
		}

		@Override
		public SymbolTable<MethodDescriptor> getDeclaredMethods() {
			return declaredMethods;
		}

		@Override
		public SymbolTable<MethodDescriptor> getMethods() {
			return methods;
		}

		@Override
		protected void link() {}

		@Override
		protected void init() {
			getMethod = new GeneratedMethodDescriptor("get",
					ImmutableMap.<String, TypeDescriptor>builder()
							.put("id", variableId)
							.build()) {
				@SuppressWarnings("unchecked")
				@Override
				public Object call(final Interface iface, final Map<String, Object> args) {
					Object id = args.get("id");
					return ((GenericCollection) iface).get(id);
				}
			};

			declaredMethods = ImmutableSymbolTable.of(
					getMethod
			);
			methods = ImmutableSymbolTable.of();
			methods = methods.merge(declaredMethods);
		}
	}
}
