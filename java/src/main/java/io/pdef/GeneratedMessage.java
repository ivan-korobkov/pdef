package io.pdef;

import com.google.common.collect.Maps;
import io.pdef.json.Json;

import java.util.Map;

/** Abstract class for a generated message. */
public abstract class GeneratedMessage implements Message {
	private transient int hash;

	protected GeneratedMessage() {}

	protected GeneratedMessage(final Map<?, ?> map) {}

	protected GeneratedMessage(final Builder builder) {}

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
		protected Builder() {}
		protected Builder(final GeneratedMessage message) {}

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
