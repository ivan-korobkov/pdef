package io.pdef;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class Pdef {
	public static enum TypeEnum {
		BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, DECIMAL, DATE, DATETIME, STRING, UUID, OBJECT,
		VOID, LIST, SET, MAP, MESSAGE, ENUM
	}

	private final Map<Type, TypeInfo> map = Maps.newHashMap();

	public synchronized TypeInfo get(final Type javaType) {
		TypeInfo info = map.get(javaType);
		if (info != null) return info;
		return info(javaType);
	}

	private void add(final Type javaType, final TypeInfo typeInfo) {
		checkState(!map.containsKey(javaType), "Duplicate type info for " + javaType);
		map.put(javaType, typeInfo);
	}

	private TypeInfo info(final Type javaType) {
		checkNotNull(javaType);

		Class<?> cls = (javaType instanceof ParameterizedType)
					   ? (Class<?>) ((ParameterizedType) javaType).getRawType()
					   : (Class<?>) javaType;

		if (cls == boolean.class || cls == Boolean.class) return value(javaType, TypeEnum.BOOL);
		if (cls == short.class || cls == Short.class) return value(javaType, TypeEnum.INT16);
		if (cls == int.class || cls == Integer.class) return value(javaType, TypeEnum.INT32);
		if (cls == long.class || cls == Long.class) return value(javaType, TypeEnum.INT64);
		if (cls == float.class || cls == Float.class) return value(javaType, TypeEnum.FLOAT);
		if (cls == double.class || cls == Double.class) return value(javaType, TypeEnum.DOUBLE);

		if (cls == BigDecimal.class) return value(javaType, TypeEnum.DECIMAL);
		if (cls == String.class) return value(javaType, TypeEnum.STRING);
		if (cls == Object.class) return value(javaType, TypeEnum.OBJECT);
		if (cls == void.class || cls == Void.class) return value(javaType, TypeEnum.VOID);

		if (cls == List.class) return new ListInfo(javaType, this);
		if (cls == Set.class) return new SetInfo(javaType, this);
		if (cls == Map.class) return new MapInfo(javaType, this);
		if (cls.isEnum()) return new EnumInfo(javaType, this);
		if (Message.class.isAssignableFrom(cls)) return new MessageInfo(cls, this);

		throw new IllegalArgumentException("Unsupported java type " + javaType);
	}

	private TypeInfo value(final Type javaType, final TypeEnum type) {
		return new TypeInfo(javaType, type, this);
	}

	public static class TypeInfo {
		protected final Type javaType;
		protected final TypeEnum type;
		protected final Pdef pdef;

		protected TypeInfo(final Type javaType, final TypeEnum type, final Pdef pdef) {
			this.javaType = javaType;
			this.type = type;
			this.pdef = pdef;

			this.pdef.add(javaType, this);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(type).toString();
		}

		public Type getJavaType() { return javaType; }
		public Class<?> getJavaClass() { return (Class<?>) javaType; }
		public TypeEnum getType() { return type; }
		public Pdef getPdef() { return pdef; }

		public ListInfo asList() { return (ListInfo) this; }
		public SetInfo asSet() { return (SetInfo) this; }
		public MapInfo asMap() { return (MapInfo) this; }
		public MessageInfo asMesage() { return (MessageInfo) this; }
		public EnumInfo asEnum() { return (EnumInfo) this; }

		protected TypeInfo infoOf(final Type javaType1) { return pdef.get(javaType1); }
	}

	public static class ListInfo extends TypeInfo {
		private final TypeInfo element;
		protected ListInfo(final Type javaType, final Pdef pdef) {
			super(javaType, TypeEnum.LIST, pdef);
			element = infoOf(param(javaType).getActualTypeArguments()[0]);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(element).toString();
		}

		@Override
		public Class<?> getJavaClass() { return List.class; }
		public TypeInfo getElement() { return element; }
	}

	public static class SetInfo extends TypeInfo {
		private final TypeInfo element;
		protected SetInfo(final Type javaType, final Pdef pdef) {
			super(javaType, TypeEnum.SET, pdef);
			element = infoOf(param(javaType).getActualTypeArguments()[0]);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(element).toString();
		}

		@Override
		public Class<?> getJavaClass() { return Set.class; }
		public TypeInfo getElement() { return element; }
	}

	public static class MapInfo extends TypeInfo {
		private final TypeInfo key;
		private final TypeInfo value;
		protected MapInfo(final Type javaType, final Pdef pdef) {
			super(javaType, TypeEnum.MAP, pdef);
			key = infoOf(param(javaType).getActualTypeArguments()[0]);
			value = infoOf(param(javaType).getActualTypeArguments()[1]);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(key).addValue(value).toString();
		}

		@Override
		public Type getJavaType() { return Map.class; }
		public TypeInfo getKey() { return key; }
		public TypeInfo getValue() { return value; }
	}

	public static class EnumInfo extends TypeInfo {
		private final BiMap<String, Enum<?>> values;
		protected EnumInfo(final Type javaType, final Pdef pdef) {
			super(javaType, TypeEnum.ENUM, pdef);

			ImmutableBiMap.Builder<String, Enum<?>> builder = ImmutableBiMap.builder();
			for (Enum<?> anEnum : enumValues(javaType)) {
				builder.put(anEnum.name(), anEnum);
			}
			values = builder.build();
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(getJavaClass().getSimpleName()).toString();
		}

		public BiMap<String, Enum<?>> getValues() { return values; }
	}

	public static class MessageInfo extends TypeInfo {
		private final Class<?> builderClass;
		private final Constructor<?> builderConstructor;
		private final MessageInfo base;
		private final Map<String, FieldInfo> fields;
		private final Map<String, FieldInfo> declaredFields;
		private final Map<String, MessageInfo> subtypes;
		private final FieldInfo discriminator;

		protected MessageInfo(final Class<?> cls, final Pdef pdef) {
			super(cls, TypeEnum.MESSAGE, pdef);
			Type baseType = cls.getGenericSuperclass();
			try {
				builderClass = Class.forName(cls.getName() + "$Builder");
				builderConstructor = builderClass.getConstructor();
				base = baseType == Object.class || baseType == RuntimeException.class
					   ? null : (MessageInfo) infoOf(baseType);
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}

			{
				Field[] declared = cls.getDeclaredFields();
				ImmutableMap.Builder<String, FieldInfo> builder = ImmutableMap.builder();
				for (Field field : declared) {
					if (Modifier.isStatic(field.getModifiers())) continue;
					FieldInfo fieldInfo = new FieldInfo(this, field);
					builder.put(fieldInfo.getName(), fieldInfo);
				}
				declaredFields = builder.build();
			}

			{
				fields = base == null ? declaredFields : ImmutableMap.<String, FieldInfo>builder()
						.putAll(base.getFields())
						.putAll(declaredFields)
						.build();
			}

			if (!cls.isAnnotationPresent(Subtypes.class)) {
				discriminator = null;
				subtypes = ImmutableMap.of();
			} else {
				Discriminator dann = cls.getAnnotation(Discriminator.class);
				Subtypes sann = cls.getAnnotation(Subtypes.class);
				checkNotNull(dann, "Discriminator annotation must be present in %s", cls);
				checkNotNull(sann, "Subtypes annotation must be present in %s", cls);

				ImmutableMap.Builder<String, MessageInfo> builder = ImmutableMap.builder();
				for (Subtype value : sann.value()) {
					String name = value.type();
					MessageInfo info = (MessageInfo) infoOf(value.value());
					builder.put(name, info);
				}
				subtypes = builder.build();
				discriminator = fields.get(dann.value().toLowerCase());
				checkState(discriminator != null, "Discriminator field \"%s\" is not found in %s",
						dann.value(), cls);
			}
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(getJavaClass().getSimpleName()).toString();
		}

		public Class<?> getBuilderClass() { return builderClass; }
		public Constructor<?> getBuilderConstructor() { return builderConstructor; }
		public MessageInfo getBase() { return base; }
		public Map<String, FieldInfo> getFields() { return fields; }
		public Map<String, FieldInfo> getDeclaredFields() { return declaredFields; }
		public FieldInfo getDiscriminator() { return discriminator; }
		public Map<String, MessageInfo> getSubtypes() { return subtypes; }
		public boolean isPolymorphic() { return !subtypes.isEmpty(); }
		public Message.Builder createBuilder() {
			try {
				return (Message.Builder) builderConstructor.newInstance();
			} catch (Exception e) {
				throw Throwables.propagate(e);
			}
		}
	}

	public static class FieldInfo {
		private final MessageInfo message;
		private final Field field;
		private final String name;
		private final TypeInfo type;
		private final Method messageGet;
		private final Method messageHas;
		private final Method builderSet;

		public FieldInfo(final MessageInfo message, final Field field) {
			this.message = checkNotNull(message);
			this.field = checkNotNull(field);
			type = message.infoOf(field.getGenericType());
			name = field.getName().toLowerCase();

			String upperFirst = Character.toUpperCase(field.getName().charAt(0))
					+ field.getName().substring(1);
			String get = "get" + upperFirst;
			String has = "has" + upperFirst;
			String set = "set" + upperFirst;
			try {
				messageGet = message.getJavaClass().getMethod(get);
				messageHas = message.getJavaClass().getMethod(has);
				builderSet = message.builderClass.getMethod(set, field.getType());
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(getName()).addValue(getType()).toString();
		}

		public String getName() { return name; }
		public MessageInfo getMessage() { return message; }
		public Field getField() { return field; }
		public TypeInfo getType() { return type; }

		public Object get(final Object message) {
			try {
				return messageGet.invoke(message);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		public boolean isSet(final Object message) {
			try {
				return (Boolean) messageHas.invoke(message);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		public void set(final Object builder, final Object value) {
			try {
				builderSet.invoke(builder, value);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}
	}

	static ParameterizedType param(final Type javaType) {
		return (ParameterizedType) javaType;
	}

	@SuppressWarnings("unchecked")

	static Enum<?>[] enumValues(final Type javaType) {
		Class<?> cls = (Class<?>) javaType;
		try {
			Method method = cls.getMethod("values");
			return (Enum<?>[]) method.invoke(null);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		} catch (IllegalAccessException e) {
			throw new RuntimeException(e);
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
