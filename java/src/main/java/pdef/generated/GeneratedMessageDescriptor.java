package pdef.generated;

import pdef.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class GeneratedMessageDescriptor extends GeneratedTypeDescriptor
		implements MessageDescriptor {
	protected GeneratedMessageDescriptor(final Class<?> type) {
		super(type);
	}

	@Override
	public MessageDescriptor getBase() {
		return null;
	}

	@Nullable
	@Override
	public Subtypes getSubtypes() {
		return null;
	}

	@Override
	public SymbolTable<VariableDescriptor> getVariables() {
		return ImmutableSymbolTable.of();
	}

	@Override
	public MessageDescriptor parameterize(final TypeDescriptor... args) {
		return (MessageDescriptor) super.parameterize(args);
	}

	@Override
	protected TypeDescriptor newParameterizedType(final List<TypeDescriptor> args) {
		return new ParameterizedMessageDescriptor(this, args);
	}

	@Override
	public MessageDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return this;
	}
}
