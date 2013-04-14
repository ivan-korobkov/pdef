package pdef;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;

public class ImmutableSubtypes implements Subtypes {
	private final Enum<?> type;
	private final FieldDescriptor field;
	private final ImmutableMap<Enum<?>, MessageDescriptor> map;

	public static Builder builder() {
		return new Builder();
	}

	private ImmutableSubtypes(final Enum<?> type, final FieldDescriptor field,
			final ImmutableMap<Enum<?>, MessageDescriptor> map) {
		this.type = checkNotNull(type);
		this.field = checkNotNull(field);
		this.map = checkNotNull(map);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.addValue(type)
				.addValue(field)
				.addValue(map)
				.toString();
	}

	@Override
	public Enum<?> getType() {
		return type;
	}

	@Override
	public FieldDescriptor getField() {
		return field;
	}

	@Nullable
	@Override
	public MessageDescriptor getSubtype(final Object object) {
		return map.get(object);
	}

	@Override
	public ImmutableMap<Enum<?>, MessageDescriptor> getMap() {
		return map;
	}

	@Override
	public Subtypes.Builder subclass(final Enum<?> subtype) {
		checkArgument(map.containsKey(subtype));
		return builder().setField(field).setType(subtype);
	}

	public static class Builder implements Subtypes.Builder {
		private Enum<?> type;
		private FieldDescriptor field;
		private ImmutableMap.Builder<Enum<?>, MessageDescriptor> map;

		private Builder() {
			map = ImmutableMap.builder();
		}

		@Override
		public Builder setType(final Enum<?> type) {
			checkState(this.type == null, "type is already set to %s", this.type);
			this.type = type;
			return this;
		}

		@Override
		public Builder setField(final FieldDescriptor field) {
			checkState(this.field == null, "field is already set to %s", this.field);
			this.field = field;
			return this;
		}

		@Override
		public Builder put(final Enum<?> enumType, final MessageDescriptor message) {
			map.put(enumType, message);
			return this;
		}

		@Override
		public Builder putAll(final Map<? extends Enum<?>, ? extends MessageDescriptor> map) {
			this.map.putAll(map);
			return this;
		}

		public ImmutableSubtypes build() {
			return new ImmutableSubtypes(type, field, map.build());
		}
	}
}
