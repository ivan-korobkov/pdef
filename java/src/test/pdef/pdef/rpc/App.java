package pdef.rpc;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import pdef.*;
import pdef.fixtures.User;
import pdef.generated.GeneratedInterfaceDescriptor;
import pdef.generated.GeneratedMethodDescriptor;
import pdef.provided.NativeValueDescriptors;
import pdef.provided.NativeVariableDescriptor;

import java.util.List;
import java.util.Map;

public interface App extends GenericCollection<Integer, String> {

	String echo(String text);

	User register(String nick, String email, String password);

	User login(String email, String password);

	void ping();

	public static class Descriptor extends GeneratedInterfaceDescriptor {
		private static final Descriptor instance = new Descriptor();

		public static Descriptor getInstance() {
			instance.initialize();
			return instance;
		}

		private final VariableDescriptor variableT;
		private final SymbolTable<VariableDescriptor> variables;
		private List<InterfaceDescriptor> bases;

		private MethodDescriptor echoMethod;
		private MethodDescriptor registerMethod;
		private MethodDescriptor loginMethod;
		private MethodDescriptor pingMethod;
		private SymbolTable<MethodDescriptor> declaredMethods;
		private SymbolTable<MethodDescriptor> methods;

		protected Descriptor() {
			super(App.class);
			variableT = new NativeVariableDescriptor("T");
			variables = ImmutableSymbolTable.of(variableT);
		}

		@Override
		public SymbolTable<VariableDescriptor> getVariables() {
			return variables;
		}

		@Override
		public List<InterfaceDescriptor> getBases() {
			return bases;
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
		protected void link() {
			bases = ImmutableList.<InterfaceDescriptor>of(
					GenericCollection.Descriptor.getInstance().parameterize(
							NativeValueDescriptors.getInt32(),
							NativeValueDescriptors.getString())
			);
		}

		@Override
		protected void init() {
			echoMethod = new GeneratedMethodDescriptor("echo",
					ImmutableMap.<String, TypeDescriptor>builder()
							.put("text", NativeValueDescriptors.getString())
							.build()) {
				@Override
				public Object call(final Interface iface, final Map<String, Object> args) {
					String text = (String) args.get("text");
					return ((App) iface).echo(text);
				}
			};

			registerMethod = new GeneratedMethodDescriptor("register",
					ImmutableMap.<String, TypeDescriptor>builder()
							.put("nick", NativeValueDescriptors.getString())
							.put("email", NativeValueDescriptors.getString())
							.put("password", NativeValueDescriptors.getString())
							.build()) {
				@Override
				public Object call(final Interface iface, final Map<String, Object> args) {
					String nick = (String) args.get("nick");
					String email = (String) args.get("email");
					String password = (String) args.get("password");
					return ((App) iface).register(nick, email, password);
				}
			};

			loginMethod = new GeneratedMethodDescriptor("login",
					ImmutableMap.<String, TypeDescriptor>builder()
							.put("email", NativeValueDescriptors.getString())
							.put("password", NativeValueDescriptors.getString())
							.build()) {
				@Override
				public Object call(final Interface iface, final Map<String, Object> args) {
					String email = (String) args.get("email");
					String password = (String) args.get("password");
					return ((App) iface).login(email, password);
				}
			};

			pingMethod = new GeneratedMethodDescriptor("ping",
					ImmutableMap.<String, TypeDescriptor>builder()
							.build()) {
				@Override
				public Object call(final Interface iface, final Map<String, Object> args) {
					((App) iface).ping();
					return null;
				}
			};

			declaredMethods = ImmutableSymbolTable.of(
					echoMethod,
					registerMethod,
					loginMethod,
					pingMethod);
			methods = ImmutableSymbolTable.of();
			for (InterfaceDescriptor base : bases) {
				methods = methods.merge(base.getMethods());
			}
			methods = methods.merge(declaredMethods);
		}
	}
}
