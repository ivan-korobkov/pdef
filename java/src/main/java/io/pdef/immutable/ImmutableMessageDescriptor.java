package io.pdef.immutable;

import io.pdef.*;

import javax.annotation.Nullable;
import java.util.*;

/** MessageDescriptor is a descriptor for Pdef messages. */
public class ImmutableMessageDescriptor<M extends Message> extends AbstractDataDescriptor<M>
		implements MessageDescriptor<M> {
	private final Provider<M> provider;
	private final MessageDescriptor<? super M> base;

	private final List<FieldDescriptor<M, ?>> declaredFields;
	private final List<FieldDescriptor<? super M, ?>> fields;
	private final Map<String, FieldDescriptor<? super M, ?>> fieldMap;

	private final Enum<?> discriminatorValue;
	private final FieldDescriptor<? super M, ?> discriminator;

	private final List<Provider<MessageDescriptor<? extends M>>> subtypeProviders;
	private Set<MessageDescriptor<? extends M>> subtypes;
	private Map<Enum<?>, MessageDescriptor<? extends M>> subtypeMap;

	private final boolean form;

	private ImmutableMessageDescriptor(final Builder<M> builder) {
		super(TypeEnum.MESSAGE, builder.javaClass);
		if (builder.provider == null) throw new NullPointerException("provider");

		provider = builder.provider;
		base = builder.base;

		declaredFields = ImmutableCollections.list(builder.declaredFields);
		fields = joinFields(declaredFields, base);
		fieldMap = fieldMap(fields);

		discriminator = findDiscriminator(fields);
		discriminatorValue = builder.discriminatorValue;
		subtypeProviders = ImmutableCollections.list(builder.subtypes);

		form = builder.form;
	}

	public static <M extends Message> Builder<M> builder() {
		return new Builder<M>();
	}

	@Override
	public MessageDescriptor getBase() {
		return base;
	}

	@Nullable
	@Override
	public FieldDescriptor<? super M, ?> getField(final String name) {
		return fieldMap.get(name);
	}

	@Override
	public boolean isPolymorphic() {
		return discriminator != null;
	}

	@Override
	public Enum<?> getDiscriminatorValue() {
		return discriminatorValue;
	}

	@Override
	public FieldDescriptor<? super M, ?> getDiscriminator() {
		return discriminator;
	}

	@Override
	public MessageDescriptor<? extends M> getSubtype(@Nullable final Enum<?> discriminatorValue) {
		if (subtypeMap == null) {
			getSubtypes();
		}
		return subtypeMap.get(discriminatorValue);
	}

	@Override
	public Set<MessageDescriptor<? extends M>> getSubtypes() {
		if (subtypes != null) {
			return subtypes;
		}

		Set<MessageDescriptor<? extends M>> list = new HashSet<MessageDescriptor<? extends M>>();
		for (Provider<MessageDescriptor<? extends M>> provider : subtypeProviders) {
			MessageDescriptor<? extends M> subtype = provider.get();
			list.add(subtype);
		}

		Map<Enum<?>, MessageDescriptor<? extends M>> map = new HashMap<Enum<?>,
				MessageDescriptor<? extends M>>();
		for (MessageDescriptor<? extends M> subtype : list) {
			map.put(subtype.getDiscriminatorValue(), subtype);
		}

		subtypes = ImmutableCollections.set(list);
		subtypeMap = ImmutableCollections.map(map);
		return subtypes;
	}

	@Override
	public List<FieldDescriptor<M, ?>> getDeclaredFields() {
		return declaredFields;
	}

	@Override
	public List<FieldDescriptor<? super M, ?>> getFields() {
		return fields;
	}

	@Override
	public boolean isForm() {
		return form;
	}

	// Message methods.

	@Override
	public M newInstance() {
		return provider.get();
	}

	@Override
	public M copy(final M src) {
		if (src == null) {
			return null;
		}

		M dst = newInstance();
		copy(src, dst);
		return dst;
	}

	@Override
	public void copy(final M src, final M dst) {
		for (FieldDescriptor<? super M, ?> field : getFields()) {
			field.copy(src, dst);
		}
	}

	@Override
	public String toString() {
		return "MessageDescriptor{" + getJavaClass().getSimpleName() + '}';
	}

	public static class Builder<M extends Message> {
		private Class<M> javaClass;
		private Provider<M> provider;
		private MessageDescriptor<? super M> base;
		private Enum<?> discriminatorValue;
		private final List<FieldDescriptor<M, ?>> declaredFields;
		private final List<Provider<MessageDescriptor<? extends M>>> subtypes;
		private boolean form;

		private Builder() {
			declaredFields = new ArrayList<FieldDescriptor<M, ?>>();
			subtypes = new ArrayList<Provider<MessageDescriptor<? extends M>>>();
		}

		public Builder<M> setJavaClass(final Class<M> javaClass) {
			this.javaClass = javaClass;
			return this;
		}

		public Builder<M> setProvider(final Provider<M> provider) {
			this.provider = provider;
			return this;
		}

		public Builder<M> setBase(final MessageDescriptor<? super M> base) {
			this.base = base;
			return this;
		}

		public Builder<M> setForm(final boolean form) {
			this.form = form;
			return this;
		}

		public Builder<M> setDiscriminatorValue(final Enum<?> value) {
			this.discriminatorValue = value;
			return this;
		}

		public Builder<M> addField(final FieldDescriptor<M, ?> field) {
			declaredFields.add(field);
			return this;
		}

		public Builder<M> addSubtype(final Provider<MessageDescriptor<? extends M>> subtype) {
			subtypes.add(subtype);
			return this;
		}

		public MessageDescriptor<M> build() {
			return new ImmutableMessageDescriptor<M>(this);
		}
	}

	private static <M extends Message> List<FieldDescriptor<? super M, ?>> joinFields(
			final List<FieldDescriptor<M, ?>> declaredFields,
			@Nullable final MessageDescriptor<? super M> base) {
		List<FieldDescriptor<? super M, ?>> result = new ArrayList<FieldDescriptor<? super M, ?>>();
		if (base != null) {
			result.addAll(base.getFields());
		}
		result.addAll(declaredFields);
		return Collections.unmodifiableList(result);
	}

	private static <M extends Message> Map<String, FieldDescriptor<? super M, ?>> fieldMap(
			final List<FieldDescriptor<? super M, ?>> iterable) {
		Map<String, FieldDescriptor<? super M, ?>> map = new HashMap<String,
				FieldDescriptor<? super M, ?>>();
		for (FieldDescriptor<? super M, ?> field : iterable) {
			map.put(field.getName(), field);
		}
		return Collections.unmodifiableMap(map);
	}

	private static <M> FieldDescriptor<? super M, ?> findDiscriminator(
			final Iterable<FieldDescriptor<? super M, ?>> fields) {
		for (FieldDescriptor<? super M, ?> field : fields) {
			if (field.isDiscriminator()) {
				return field;
			}
		}
		return null;
	}
}
