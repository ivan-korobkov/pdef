package pdef.descriptors;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import pdef.TypeEnum;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class MethodDescriptor {
	private final String name;
	private final Supplier<Descriptor> result;
	private final boolean index;
	private final boolean post;
	private final List<ArgDescriptor> args;
	private final InterfaceDescriptor anInterface;
	private final Method reflexMethod;

	private MethodDescriptor(final Builder builder, final InterfaceDescriptor anInterface) {
		this.anInterface = checkNotNull(anInterface);
		name = checkNotNull(builder.name);
		result = checkNotNull(builder.result);
		index = builder.index;
		post = builder.post;

		ImmutableList.Builder<ArgDescriptor> temp = ImmutableList.builder();
		for (ArgDescriptor.Builder ab : builder.args) {
			temp.add(ab.build(this));
		}
		args = temp.build();

		reflexMethod = getReflexMethod(anInterface.getCls(), name);
	}

	public String getName() {
		return name;
	}

	public Descriptor getResult() {
		return result.get();
	}

	public MessageDescriptor getExc() {
		return anInterface.getExc();
	}

	public List<ArgDescriptor> getArgs() {
		return args;
	}

	public InterfaceDescriptor getInterface() {
		return anInterface;
	}

	public boolean isIndex() {
		return index;
	}

	public boolean isPost() {
		return post;
	}

	public boolean isRemote() {
		return getResult().getType() != TypeEnum.INTERFACE;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Invokes this method an object with arguments. */
	public Object invoke(final Object object, final Object[] args) {
		checkNotNull(object);

		try {
			return reflexMethod.invoke(object, args);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	public static class Builder {
		private String name;
		private Supplier<Descriptor> result;
		private boolean index;
		private boolean post;
		private final List<ArgDescriptor.Builder> args;

		public Builder() {
			args = Lists.newArrayList();
		}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setResult(final Supplier<Descriptor> result) {
			this.result = result;
			return this;
		}

		public Builder setIndex(final boolean index) {
			this.index = index;
			return this;
		}

		public Builder setPost(final boolean post) {
			this.post = post;
			return this;
		}

		public Builder addArg(final ArgDescriptor.Builder arg) {
			args.add(arg);
			return this;
		}

		public MethodDescriptor build(final InterfaceDescriptor anInterface) {
			return new MethodDescriptor(this, anInterface);
		}
	}

	private static Method getReflexMethod(final Class<?> cls, final String name) {
		for (Method method : cls.getMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}

		throw new AssertionError("Method is not found " + name);
	}
}
