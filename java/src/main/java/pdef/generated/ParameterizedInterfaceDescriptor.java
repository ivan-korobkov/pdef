package pdef.generated;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import pdef.*;

import java.util.List;
import java.util.Map;

final class ParameterizedInterfaceDescriptor
		extends ParameterizedTypeDescriptor<InterfaceDescriptor>
		implements InterfaceDescriptor {
	private List<InterfaceDescriptor> bases;
	private SymbolTable<MethodDescriptor> declaredMethods;
	private SymbolTable<MethodDescriptor> methods;

	ParameterizedInterfaceDescriptor(final GeneratedInterfaceDescriptor raw,
			final List<TypeDescriptor> args) {
		super(raw.getJavaClass(), raw, args);
	}

	@Override
	protected void link() {
		Map<VariableDescriptor, TypeDescriptor> argMap = argMap();
		List<InterfaceDescriptor> temp = Lists.newArrayList();
		for (InterfaceDescriptor base : raw.getBases()) {
			temp.add(base.bind(argMap));
		}
		bases = ImmutableList.copyOf(temp);
	}

	@Override
	protected void init() {
		Map<VariableDescriptor, TypeDescriptor> argMap = argMap();
		Function<MethodDescriptor, MethodDescriptor> bind = bindFunc(argMap);
		declaredMethods = ImmutableSymbolTable.copyOf(
				Iterables.transform(raw.getDeclaredMethods(), bind));
		if (bases.isEmpty()) {
			methods = declaredMethods;
			return;
		}

		SymbolTable<MethodDescriptor> temp = ImmutableSymbolTable.of();
		for (InterfaceDescriptor base : bases) {
			temp = temp.merge(base.getMethods());
		}
		methods = temp.merge(declaredMethods);
	}

	@Override
	public InterfaceDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return (InterfaceDescriptor) super.bind(argMap);
	}

	@Override
	public InterfaceDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected TypeDescriptor newParameterizedType(final List<TypeDescriptor> args) {
		TypeDescriptor[] array = new TypeDescriptor[0];
		return raw.parameterize(args.toArray(array));
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
}
