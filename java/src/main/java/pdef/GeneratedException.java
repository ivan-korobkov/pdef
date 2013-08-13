package pdef;

import com.google.common.collect.Maps;
import pdef.json.Json;

import java.io.Serializable;
import java.util.Map;

public abstract class GeneratedException extends RuntimeException implements Message, Serializable {
	private transient int hash;

	protected GeneratedException() {}

	protected GeneratedException(final Map<?, ?> map) {}

	protected GeneratedException(final Builder builder) {}

	@Override
	public Map<String, Object> serialize() {
		return Maps.newLinkedHashMap();
	}

	@Override
	public String serializeToJson() {
		Object object = serialize();
		return Json.serialize(object);
	}

	@Override
	public boolean equals(final Object o) {
		return this == o || !(o == null || getClass() != o.getClass());
	}

	@Override
	public int hashCode() {
		if (hash == 0) hash = generateHashCode();
		return hash;
	}

	protected int generateHashCode() {
		return 31;
	}

	public static abstract class Builder implements Message.Builder {
		public Builder() {}
		public Builder(final GeneratedException message) {}

		@Override
		public boolean equals(final Object o) {
			return this == o || !(o == null || getClass() != o.getClass());
		}

		@Override
		public int hashCode() {
			return 31;
		}
	}
}
