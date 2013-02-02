package com.ivankorobkov.pdef.data;

import com.google.common.base.Objects;
import com.ivankorobkov.pdef.json.LineFormat;
import com.ivankorobkov.pdef.json.LineFormatImpl;

import java.util.BitSet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Provides default equals and hashCode implementations which iterate over the fields.
 */
public abstract class AbstractMessage implements Message {

	protected final BitSet _fields_bitset = new BitSet();

	@Override
	public String toString() {
		LineFormat lineFormat = LineFormatImpl.getInstance();
		return lineFormat.toJson(this);
	}

	@Override
	public Message deepCopy() {
		MessageDescriptor descriptor = getDescriptor();
		return descriptor.deepCopy(this);
	}

	@Override
	public Message mergeFrom(final Message another) {
		checkNotNull(another);

		MessageDescriptor descriptor;
		if (getClass().isInstance(another)) {
			descriptor = getDescriptor();
		} else if (another.getClass().isInstance(this)) {
			descriptor = another.getDescriptor();
		} else {
			throw new IllegalArgumentException(
					"Can merge only matching, sub and super classes, got " + this + " and "
							+ another);
		}

		return descriptor.merge(this, another);
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}

		Message message = (Message) obj;
		MessageDescriptor descriptor = getDescriptor();

		for (MessageField field : descriptor.getFields()) {
			Object val1 = field.get(this);
			Object val2 = field.get(message);
			if (!Objects.equal(val1, val2)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = 1;

		MessageDescriptor descriptor = getDescriptor();
		for (MessageField field : descriptor.getFields()) {
			Object val = field.get(this);
			result = 31 * result + (val != null ? val.hashCode() : 0);
		}

		return result;
	}
}
