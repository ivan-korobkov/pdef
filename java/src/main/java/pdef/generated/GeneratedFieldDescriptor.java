package pdef.generated;

import com.google.common.base.Objects;
import static com.google.common.base.Preconditions.*;
import pdef.FieldDescriptor;
import pdef.Message;
import pdef.TypeDescriptor;
import pdef.VariableDescriptor;

import java.util.Map;

public abstract class GeneratedFieldDescriptor implements FieldDescriptor {
	private final String name;
	private final TypeDescriptor type;

	protected GeneratedFieldDescriptor(final String name, final TypeDescriptor type) {
		this.name = checkNotNull(name);
		this.type = checkNotNull(type);
	}

	@Override
	public String toString() {
		return Objects.toStringHelper("FieldDescriptor")
				.addValue(name)
				.addValue(type)
				.toString();
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public TypeDescriptor getType() {
		return type;
	}

	@Override
	public boolean isTypeField() {
		return false;
	}

	@Override
	public FieldDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
		TypeDescriptor btype = type.bind(argMap);
		if (type == btype) {
			return this;
		}

		return new ParameterizedFieldDescriptor(this, btype);
	}

	static final class ParameterizedFieldDescriptor implements FieldDescriptor {
		private final FieldDescriptor rawField;
		private final TypeDescriptor type;

		ParameterizedFieldDescriptor(final FieldDescriptor rawField, final TypeDescriptor type) {
			this.rawField = checkNotNull(rawField);
			this.type = checkNotNull(type);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this)
					.addValue(getName())
					.addValue(type)
					.toString();
		}

		@Override
		public TypeDescriptor getType() {
			return type;
		}

		@Override
		public boolean isTypeField() {
			return rawField.isTypeField();
		}

		@Override
		public String getName() {
			return rawField.getName();
		}

		@Override
		public Object get(final Message message) {
			return rawField.get(message);
		}

		@Override
		public Object get(final Message.Builder builder) {
			return rawField.get(builder);
		}

		@Override
		public boolean isSet(final Message message) {
			return rawField.isSet(message);
		}

		@Override
		public boolean isSet(final Message.Builder builder) {
			return rawField.isSet(builder);
		}

		@Override
		public void set(final Message.Builder builder, final Object value) {
			rawField.set(builder, value);
		}

		@Override
		public void clear(final Message.Builder builder) {
			rawField.clear(builder);
		}

		@Override
		public FieldDescriptor bind(final Map<VariableDescriptor, TypeDescriptor> argMap) {
			TypeDescriptor btype = type.bind(argMap);
			if (type == btype) {
				return this;
			}

			return new ParameterizedFieldDescriptor(this, btype);
		}
	}
}
