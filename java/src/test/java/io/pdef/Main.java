package io.pdef;

import com.google.common.base.Objects;
import io.pdef.test.messages.SimpleMessage;
import io.pdef.types.MessageField;

public class Main {
	public static void main(String[] args) {
		SimpleMessage message = new SimpleMessage();

		Validator<SimpleMessage> validator = new Validator<SimpleMessage>(message)
				.validateEquals(SimpleMessage.ABOOL_FIELD, true)
				.validateEquals(SimpleMessage.ANINT16_FIELD, (short) 123)
				.validateEquals(SimpleMessage.ASTRING_FIELD, "qwer");
	}

	public static class Validator<M> {
		private final M message;

		public Validator(final M message) {
			this.message = message;
		}

		public <V> Validator<M> validateEquals(final MessageField<? super M, V> field,
				final V expected) {
			V value = field.get(message);
			if (!Objects.equal(expected, value)) {
				throw new IllegalArgumentException("Wrong field " + field.getName());
			}

			return this;
		}

		public Validator<M> validateNotEmpty(final MessageField<? super M, ?> field) {
			Object value = field.get(message);
			if (value == null) {
				throw new IllegalArgumentException("Null field " + field.getName());
			}
			return this;
		}
	}
}
