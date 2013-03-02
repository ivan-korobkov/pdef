package pdef.provided;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;
import pdef.ImmutableSymbolTable;
import pdef.SymbolTable;
import pdef.MapDescriptor;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Arrays;
import java.util.Map;

public final class NativeMapDescriptor implements MapDescriptor, NativeDescriptor {
	private static final NativeMapDescriptor INSTANCE = new NativeMapDescriptor();

	public static NativeMapDescriptor getInstance() {
		return INSTANCE;
	}

	private final VariableDescriptor key;
	private final VariableDescriptor value;
	private final SymbolTable<VariableDescriptor> variables;

	private NativeMapDescriptor() {
		key = new NativeVariableDescriptor("K");
		value = new NativeVariableDescriptor("V");
		variables = ImmutableSymbolTable.of(key, value);
	}

	@Override
	public Object getDefaultInstance() { return ImmutableMap.of(); }

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return variables; }

	@Override
	public TypeDescriptor getKey() { return key; }

	@Override
	public TypeDescriptor getValue() { return value; }

	@Override
	public MapDescriptor parameterize(final TypeDescriptor... args) {
		checkArgument(args.length == 2, "Wrong number of arguments for %s: %s", this,
				Arrays.toString(args));
		return new Parameterized(args[0], args[1]);
	}

	@Override
	public TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }

	class Parameterized implements MapDescriptor {
		private final TypeDescriptor key;
		private final TypeDescriptor value;

		Parameterized(final TypeDescriptor key, final TypeDescriptor value) {
			this.key = checkNotNull(key);
			this.value = checkNotNull(value);
		}

		@Override
		public Object getDefaultInstance() { return NativeMapDescriptor.this.getDefaultInstance(); }

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

		@Override
		public TypeDescriptor getKey() { return key; }

		@Override
		public TypeDescriptor getValue() { return value; }

		@Override
		public MapDescriptor parameterize(final TypeDescriptor... args) {
			throw new UnsupportedOperationException();
		}

		@Override
		public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
			TypeDescriptor bkey = key.bind(argMap);
			TypeDescriptor bvalue = value.bind(argMap);
			return NativeMapDescriptor.this.parameterize(bkey, bvalue);
		}
	}
}
