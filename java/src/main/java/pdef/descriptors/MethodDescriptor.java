package pdef.descriptors;

import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

public class MethodDescriptor {
	private final String name;
	private final DescriptorSupplier result;
	private final boolean index;
	private final boolean post;
	private final List<ArgDescriptor> args;
	private final InterfaceDescriptor anInterface;

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
	}

	public String getName() {
		return name;
	}

	public Descriptor getResult() {
		return result.get();
	}

	public boolean isIndex() {
		return index;
	}

	public boolean isPost() {
		return post;
	}

	public List<ArgDescriptor> getArgs() {
		return args;
	}

	public InterfaceDescriptor getInterface() {
		return anInterface;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private DescriptorSupplier result;
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

		public Builder setResult(final DescriptorSupplier result) {
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
}
