package pdef.generated;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import pdef.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

final class ParameterizedMessageDescriptor extends GeneratedMessageDescriptor
		implements MessageDescriptor {

	private final MessageDescriptor rawtype;
	private final List<TypeDescriptor> args;

	private MessageDescriptor base;
	private SymbolTable<FieldDescriptor> declaredFields;
	private SymbolTable<FieldDescriptor> fields;

	ParameterizedMessageDescriptor(final GeneratedMessageDescriptor rawtype,
			final List<TypeDescriptor> args) {
		super(rawtype.getJavaClass());
		this.rawtype = checkNotNull(rawtype);
		this.args = ImmutableList.copyOf(args);
		checkArgument(args.size() == rawtype.getVariables().size());
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(getJavaClass())
				.addValue(args)
				.toString();
	}

	public MessageDescriptor getRawtype() {
		return rawtype;
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

	@Override
	public MessageDescriptor parameterize(final TypeDescriptor... args) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void link() {
		Map<VariableDescriptor, TypeDescriptor> argMap = argMap();
		Transform<MessageDescriptor> bindMessage = bindMessageFunc(argMap);
		base = bindMessage.apply(rawtype.getBase());
	}

	@Override
	protected void init() {
		Map<VariableDescriptor, TypeDescriptor> argMap = argMap();
		Transform<FieldDescriptor> bindField = bindFieldFunc(argMap);

		declaredFields = ImmutableSymbolTable.copyOf(
				Iterables.transform(rawtype.getDeclaredFields(), bindField));
		fields = base == null ? declaredFields : base.getFields().merge(declaredFields);
	}

	@Override
	public MessageDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		checkNotNull(argMap);
		Transform<TypeDescriptor> bindArg = bindArgFunc(argMap);
		List<TypeDescriptor> bargs = Lists.transform(args, bindArg);
		TypeDescriptor[] array = new TypeDescriptor[bargs.size()];
		return rawtype.parameterize(bargs.toArray(array));
	}

	@Override
	public Message.Builder newBuilder() {
		return rawtype.newBuilder();
	}

	private Map<VariableDescriptor, TypeDescriptor> argMap() {
		ImmutableMap.Builder<VariableDescriptor, TypeDescriptor> builder = ImmutableMap.builder();
		List<VariableDescriptor> vars = rawtype.getVariables().list();

		for (int i = 0; i < vars.size(); i++) {
			VariableDescriptor var = vars.get(i);
			TypeDescriptor arg = args.get(i);
			builder.put(var, arg);
		}

		return builder.build();
	}

	private Transform<TypeDescriptor> bindArgFunc(
			final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return new Transform<TypeDescriptor>() {
			@Nullable
			@Override
			public TypeDescriptor apply(final TypeDescriptor input) {
				assert input != null;
				return input.bind(argMap);
			}
		};
	}

	private Transform<MessageDescriptor> bindMessageFunc(
			final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return new Transform<MessageDescriptor>() {
			@Nullable
			@Override
			public MessageDescriptor apply(@Nullable final MessageDescriptor input) {
				if (input == null) {
					return null;
				}

				return input.bind(argMap);
			}
		};
	}

	private Transform<FieldDescriptor> bindFieldFunc(
			final Map<VariableDescriptor, TypeDescriptor> argMap) {
		return new Transform<FieldDescriptor>() {
			@Nullable
			@Override
			public FieldDescriptor apply(final FieldDescriptor input) {
				assert input != null;
				return input.bind(argMap);
			}
		};
	}

	private static interface Transform<T> extends Function<T, T> {}
}
