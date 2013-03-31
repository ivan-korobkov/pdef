package pdef.generated;

import com.google.common.base.Function;
import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.*;
import pdef.*;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public abstract class GeneratedMessageDescriptor extends GeneratedTypeDescriptor
		implements MessageDescriptor, Generated {
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

	static final class ParameterizedMessageDescriptor extends GeneratedMessageDescriptor
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

		@Nullable
		@Override
		public MessageTree getTree() {
			return rawtype.getTree();
		}

		@Nullable
		@Override
		public MessageTree getBaseTree() {
			return rawtype.getBaseTree();
		}

		@Nullable
		@Override
		public MessageTree getRootTree() {
			return rawtype.getRootTree();
		}

		@Nullable
		@Override
		public FieldDescriptor getTypeField() {
			return rawtype.getTypeField();
		}

		@Nullable
		@Override
		public MessageDescriptor getSubtype(final Object object) {
			return rawtype.getSubtype(object);
		}

		@Override
		public boolean hasSubtypes() {
			return rawtype.hasSubtypes();
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
}
