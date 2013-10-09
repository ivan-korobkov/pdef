package io.pdef.types;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class InterfaceMethod {
	private final String name;
	private final Supplier<Type> result;
	private final boolean index;
	private final boolean post;
	private final List<InterfaceMethodArg> args;
	private final InterfaceType anInterface;
	private final Method reflexMethod;

	private InterfaceMethod(final Builder builder, final InterfaceType anInterface) {
		this.anInterface = checkNotNull(anInterface);
		name = checkNotNull(builder.name);
		result = checkNotNull(builder.result);
		index = builder.index;
		post = builder.post;

		ImmutableList.Builder<InterfaceMethodArg> temp = ImmutableList.builder();
		for (InterfaceMethodArg.Builder ab : builder.args) {
			temp.add(ab.build(this));
		}
		args = temp.build();

		reflexMethod = getReflexMethod(anInterface.getJavaClass(), name);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(name)
				.toString();
	}

	public String getName() {
		return name;
	}

	public Type getResult() {
		return result.get();
	}

	public MessageType getExc() {
		return anInterface.getExc();
	}

	public List<InterfaceMethodArg> getArgs() {
		return args;
	}

	public InterfaceType getInterface() {
		return anInterface;
	}

	public boolean isIndex() {
		return index;
	}

	public boolean isPost() {
		return post;
	}

	public boolean isRemote() {
		return getResult().type() != TypeEnum.INTERFACE;
	}

	public static Builder builder() {
		return new Builder();
	}

	/** Invokes this method an object with arguments. */
	public Object invoke(final Object object, final Object[] args) throws Exception {
		checkNotNull(object);

		try {
			return reflexMethod.invoke(object, args);
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof Error) {
				throw (Error) t;
			}
			throw (Exception) t;
		}
	}

	public static class Builder {
		private String name;
		private Supplier<Type> result;
		private boolean index;
		private boolean post;
		private final List<InterfaceMethodArg.Builder> args;

		public Builder() {
			args = Lists.newArrayList();
		}

		public Builder setName(final String name) {
			this.name = name;
			return this;
		}

		public Builder setResult(final Supplier<Type> result) {
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

		public Builder addArg(final String name, final Supplier<DataType> type) {
			return addArg(InterfaceMethodArg.builder()
					.setName(name)
					.setType(type));
		}

		public Builder addArg(final InterfaceMethodArg.Builder arg) {
			args.add(arg);
			return this;
		}

		public InterfaceMethod build(final InterfaceType anInterface) {
			return new InterfaceMethod(this, anInterface);
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
