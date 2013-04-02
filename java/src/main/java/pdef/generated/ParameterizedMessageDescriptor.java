package pdef.generated;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import pdef.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

final class ParameterizedMessageDescriptor extends ParameterizedTypeDescriptor<MessageDescriptor>
		implements MessageDescriptor {
	private MessageDescriptor base;
	private SymbolTable<FieldDescriptor> declaredFields;
	private SymbolTable<FieldDescriptor> fields;

	ParameterizedMessageDescriptor(final GeneratedMessageDescriptor raw,
			final List<TypeDescriptor> args) {
		super(raw.getJavaClass(), raw, args);
	}

	@Override
	protected void link() {
		Map<VariableDescriptor, TypeDescriptor> argMap = argMap();
		MessageDescriptor rawBase = raw.getBase();
		base = rawBase == null ? null : rawBase.bind(argMap);
	}

	@Override
	protected void init() {
		Map<VariableDescriptor, TypeDescriptor> argMap = argMap();
		Function<FieldDescriptor, FieldDescriptor> bind = bindFunc(argMap);
		declaredFields = ImmutableSymbolTable.copyOf(
				Iterables.transform(raw.getDeclaredFields(), bind));
		fields = base == null ? declaredFields : base.getFields().merge(declaredFields);
	}

	@Override
	public MessageDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return (MessageDescriptor) super.bind(argMap);
	}

	@Override
	public MessageDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected TypeDescriptor newParameterizedType(final List<TypeDescriptor> args) {
		TypeDescriptor[] array = new TypeDescriptor[0];
		return raw.parameterize(args.toArray(array));
	}

	@Nullable
	@Override
	public Subtypes getSubtypes() {
		return raw.getSubtypes();
	}

	@Override
	public Message.Builder newBuilder() {
		return raw.newBuilder();
	}

	@Override
	public MessageDescriptor getBase() {
		return base;
	}

	@Override
	public SymbolTable<FieldDescriptor> getDeclaredFields() {
		return declaredFields;
	}

	@Override
	public SymbolTable<FieldDescriptor> getFields() {
		return fields;
	}
}
