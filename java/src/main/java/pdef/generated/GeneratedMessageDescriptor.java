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
	public MessageTree getTree() {
		MessageTree tree = getRootTree();
		return tree != null ? tree : getBaseTree();
	}

	@Nullable
	@Override
	public MessageTree getBaseTree() {
		return null;
	}

	@Nullable
	@Override
	public MessageTree getRootTree() {
		return null;
	}

	@Override
	public SymbolTable<VariableDescriptor> getVariables() {
		return ImmutableSymbolTable.of();
	}

	@Nullable
	@Override
	public FieldDescriptor getTypeField() {
		MessageTree tree = getTree();
		return tree == null ? null : tree.getField();
	}

	@Nullable
	@Override
	public MessageDescriptor getSubtype(final Object object) {
		MessageTree tree = getTree();
		return tree == null ? null : tree.getMap().get(object);
	}

	@Override
	public boolean hasSubtypes() {
		return getTree() != null;
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
