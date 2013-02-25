package pdef.fixtures;

import com.google.common.collect.ImmutableMap;
import pdef.ImmutableSymbolTable;
import pdef.Message;
import pdef.SymbolTable;
import pdef.descriptors.BaseFieldDescriptor;
import pdef.descriptors.BaseMessageDescriptor;
import pdef.descriptors.FieldDescriptor;
import pdef.descriptors.MessageDescriptor;

import java.util.Map;

public class Entity implements Message {
	private Id id;

	public Id getId() {
		return id;
	}

	public Entity setId(final Id id) {
		this.id = id;
		return this;
	}

	@Override
	public MessageDescriptor getDescriptor() {
		return Descriptor.getInstance();
	}

	public static class Descriptor extends BaseMessageDescriptor {
		private static final Descriptor INSTANCE = new Descriptor();
		public static Descriptor getInstance() {
			INSTANCE.link();
			return INSTANCE;
		}

		private Map<Enum<?>, MessageDescriptor> typeMap;
		private SymbolTable<FieldDescriptor> declaredFields;

		private Descriptor() {
			super(Entity.class);
		}

		@Override
		public Enum<?> getType() {
			return Type.ENTITY;
		}

		@Override
		public Map<Enum<?>, MessageDescriptor> getTypeMap() {
			return typeMap;
		}

		@Override
		public SymbolTable<FieldDescriptor> getDeclaredFields() {
			return declaredFields;
		}

		@Override
		protected void init() {
			typeMap = ImmutableMap.<Enum<?>, MessageDescriptor>of(
					Type.ENTITY, this,
					Type.IMAGE, Image.Descriptor.getInstance(),
					Type.USER, User.Descriptor.getInstance()
			);

			declaredFields = ImmutableSymbolTable.<FieldDescriptor>of(
					new BaseFieldDescriptor("id", Id.Descriptor.getInstance()) {
						@Override
						public Object get(final Message message) {
							return ((Entity) message).getId();
						}

						@Override
						public void set(final Message message, final Object value) {
							((Entity) message).setId((Id) value);
						}
					}
			);
		}
	}
}
