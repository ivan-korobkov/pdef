package io.pdef.fixtures;

import io.pdef.Message;
import io.pdef.Subtype;
import io.pdef.Subtypes;
import io.pdef.TypeField;

@TypeField("type")
@Subtypes({
	@Subtype(value = GenericObject.class, type = "object"),
	@Subtype(value = Image.class, type = "image"),
	@Subtype(value = User.class, type = "user")
})
public class GenericObject implements Message {
	private ObjectType type;

	public GenericObject() {}

	public GenericObject(final Builder builder) {
		this.type = builder.type;
	}

	public ObjectType getType() {
		return type;
	}

	public static class Builder implements Message.Builder {
		private ObjectType type;

		public Builder() {
			this.type = ObjectType.OBJECT;
		}

		public ObjectType getType() {
			return type;
		}

		public Builder setType(final ObjectType type) {
			this.type = type;
			return this;
		}

		@Override
		public Message build() {
			return new GenericObject(this);
		}
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GenericObject that = (GenericObject) o;
		if (type != that.type) return false;
		return true;
	}

	@Override
	public int hashCode() {
		return type != null ? type.hashCode() : 0;
	}
}
