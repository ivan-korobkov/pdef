package pdef.provided;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import pdef.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class NativeListDescriptor implements ListDescriptor, NativeDescriptor {
	private static final NativeListDescriptor INSTANCE = new NativeListDescriptor();

	public static NativeListDescriptor getInstance() {
		return INSTANCE;
	}

	private final VariableDescriptor element;
	private final SymbolTable<VariableDescriptor> variables;

	private NativeListDescriptor() {
		element = new NativeVariableDescriptor("T");
		variables = ImmutableSymbolTable.of(element);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(variables)
				.toString();
	}

	@Override
	public TypeDescriptor getElement() { return element; }

	@Override
	public SymbolTable<VariableDescriptor> getVariables() { return variables; }

	@Override
	public ListDescriptor parameterize(final TypeDescriptor... args) {
		checkArgument(args.length == 1, "Wrong number of arguments for %s: %s", this,
				Arrays.toString(args));
		return new ParameterizedListDescriptor(args[0]);
	}

	@Override
	public List<Object> serialize(final Object object) {
		return doSerialize(object, element);
	}

	protected List<Object> doSerialize(final Object object, final TypeDescriptor element) {
		List<?> list = (List<?>) object;
		ImmutableList.Builder<Object> builder = ImmutableList.builder();
		for (Object e : list) {
			Object serialized = element.serialize(e);
			builder.add(serialized);
		}
		return builder.build();
	}

	@Override
	public List<Object> parse(final Object object) {
		return doParse(object, element);
	}

	private List<Object> doParse(final Object object, final TypeDescriptor element) {
		if (object == null) {
			return null;
		}

		List<?> list = (List<?>) object;
		List<Object> result = Lists.newArrayList();
		for (Object rawValue : list) {
			Object value = element.parse(rawValue);
			result.add(value);
		}

		return result;
	}

	@Override
	public TypeDescriptor bind(Map<VariableDescriptor, TypeDescriptor> argMap) { return this; }

	class ParameterizedListDescriptor implements ListDescriptor {
		private final TypeDescriptor element;

		ParameterizedListDescriptor(final TypeDescriptor element) { this.element = element; }

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(element)
					.toString();
		}

		@Override
		public TypeDescriptor getElement() { return element; }

		@Override
		public SymbolTable<VariableDescriptor> getVariables() { return ImmutableSymbolTable.of(); }

		@Override
		public ListDescriptor parameterize(final TypeDescriptor... args) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<Object> serialize(final Object object) {
			return doSerialize(object, element);
		}

		@Override
		public List<Object> parse(final Object object) {
			return doParse(object, element);
		}

		@Override
		public TypeDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
			TypeDescriptor barg = element.bind(argMap);
			return NativeListDescriptor.this.parameterize(barg);
		}
	}
}
