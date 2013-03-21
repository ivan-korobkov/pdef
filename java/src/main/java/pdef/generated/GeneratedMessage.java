package pdef.generated;

import com.google.common.base.Objects;
import pdef.FieldDescriptor;
import pdef.Message;
import pdef.MessageDescriptor;

import java.util.BitSet;

public abstract class GeneratedMessage implements Message {
	protected final BitSet _fields;
	/** Caches the hash code for the message */
	private int hash;

	protected GeneratedMessage(final Builder builder) {
		_fields = (BitSet) builder._fields.clone();
	}

	@Override
	public String toString() {
		MessageDescriptor descriptor = getDescriptor();
		Objects.ToStringHelper helper = Objects.toStringHelper(this);
		for (FieldDescriptor field : descriptor.getFields()) {
			if (!field.isSet(this)) {
				continue;
			}

			helper.add(field.getName(), field.get(this));
		}
		return helper.toString();
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GeneratedMessage that = (GeneratedMessage) o;
		MessageDescriptor descriptor = getDescriptor();
		for (FieldDescriptor field : descriptor.getFields()) {
			Object value0 = field.get(this);
			Object value1 = field.get(that);
			if (!Objects.equal(value0, value1)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int h = hash;
		if (h == 0) {
			MessageDescriptor descriptor = getDescriptor();
			for (FieldDescriptor field : descriptor.getFields()) {
				Object val = field.get(this);
				h = 31 * h + (val != null ? val.hashCode() : 0);
			}

			hash = h;
		}
		return h;
	}

	public static abstract class Builder implements Message.Builder {
		protected final BitSet _fields = new BitSet();
	}
}
