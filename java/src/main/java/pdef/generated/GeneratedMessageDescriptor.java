package pdef.generated;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import pdef.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class GeneratedMessageDescriptor extends GeneratedTypeDescriptor
		implements MessageDescriptor, GeneratedDescriptor {
	private final Map<List<TypeDescriptor>, ParameterizedMessageDescriptor> pmap;

	protected GeneratedMessageDescriptor(final Class<?> type) {
		super(type);
		pmap = Maps.newHashMap();
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

	@Override
	public MessageDescriptor parameterize(final TypeDescriptor... args) {
		checkNotNull(args);
		checkArgument(getVariables().size() == args.length,
				"Wrong number of args for %s: %s", this, args);
		List<TypeDescriptor> argList = ImmutableList.copyOf(args);

		final ParameterizedMessageDescriptor pmessage;
		synchronized (this) {
			if (pmap.containsKey(argList)) {
				pmessage = pmap.get(argList);
			} else {
				pmessage = new ParameterizedMessageDescriptor(this, argList);
				pmap.put(argList, pmessage);
			}
		}

		pmessage.initialize();
		return pmessage;
	}

	@Override
	public MessageDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return this;
	}

	@Override
	public Map<String, Object> serialize(final Object object) {
		if (object == null) {
			return null;
		}
		Message message = (Message) object;
		MessageDescriptor realDescriptor = message.getDescriptor();
		return doSerialize(message, realDescriptor);
	}

	private Map<String, Object> doSerialize(final Message message, final MessageDescriptor real) {
		Map<String, Object> map = Maps.newLinkedHashMap();
		for (FieldDescriptor field : real.getFields()) {
			if (!field.isSet(message)) {
				continue;
			}

			String name = field.getName();
			TypeDescriptor type = field.getType();

			Object value = field.get(message);
			Object rawValue = type.serialize(value);
			map.put(name, rawValue);
		}
		return map;
	}

	@Override
	public Message parse(final Object object) {
		if (object == null) {
			return null;
		}

		Map<?, ?> map = (Map<?, ?>) object;
		MessageDescriptor real = parseDescriptorType(map);
		return doParse(map, real);
	}

	private Message doParse(final Map<?, ?> map, final MessageDescriptor real) {
		Message.Builder builder = real.newBuilder();
		for (FieldDescriptor field : real.getFields()) {
			String name = field.getName();
			if (!map.containsKey(name)) {
				continue;
			}

			TypeDescriptor type = field.getType();
			Object rawValue = map.get(name);
			Object value = type.parse(rawValue);
			field.set(builder, value);
		}
		return builder.build();
	}

	@Override
	public MessageDescriptor parseDescriptorType(final Map<?, ?> map) {
		checkNotNull(map);
		MessageTree tree = getTree();
		if (tree == null) {
			return this;
		}

		FieldDescriptor field = tree.getField();
		String name = field.getName();
		if (!map.containsKey(name)) {
			return this;
		}

		TypeDescriptor type = field.getType();
		Object rawValue = map.get(name);
		Object value = type.parse(rawValue);
		MessageDescriptor subdescriptor = tree.getMap().get(value);

		if (subdescriptor == null || subdescriptor == this) {
			// TODO: Log if a subtype is not found.
			return this;
		}
		return subdescriptor.parseDescriptorType(map);
	}
}
