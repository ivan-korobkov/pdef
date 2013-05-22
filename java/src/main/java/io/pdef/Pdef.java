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
	private Pdef() {}

	public static TypeInfo info(final Type javaType) { return info(javaType, new Pool()); }
	public static TypeInfo info(final Type javaType, final Pool pool) {
		checkNotNull(javaType);
		checkNotNull(pool);

		Class<?> cls = (javaType instanceof ParameterizedType)
					   ? (Class<?>) ((ParameterizedType) javaType).getRawType()
					   : (Class<?>) javaType;

		if (cls == boolean.class || cls == Boolean.class) return info(javaType, TypeEnum.BOOL, pool);
		if (cls == short.class || cls == Short.class) return info(javaType, TypeEnum.INT16, pool);
		if (cls == int.class || cls == Integer.class) return info(javaType, TypeEnum.INT32, pool);
		if (cls == long.class || cls == Long.class) return info(javaType, TypeEnum.INT64, pool);
		if (cls == float.class || cls == Float.class) return info(javaType, TypeEnum.FLOAT, pool);
		if (cls == double.class || cls == Double.class) return info(javaType, TypeEnum.DOUBLE,
				pool);

		if (cls == BigDecimal.class) return info(javaType, TypeEnum.DECIMAL, pool);
		if (cls == String.class) return info(javaType, TypeEnum.STRING, pool);
		if (cls == Object.class) return info(javaType, TypeEnum.OBJECT, pool);
		if (cls == void.class || cls == Void.class) return info(javaType, TypeEnum.VOID, pool);

		if (cls == List.class) return new ListInfo(javaType, pool);
		if (cls == Set.class) return new SetInfo(javaType, pool);
		if (cls == Map.class) return new MapInfo(javaType, pool);
		if (cls.isEnum()) return new EnumInfo(javaType, pool);
		if (Message.class.isAssignableFrom(cls)) return new MessageInfo(cls, pool);

		throw new IllegalArgumentException("Unsupported java type " + javaType);
	}

	private static TypeInfo info(final Type javaType, final TypeEnum type, final Pool pool) {
		return new TypeInfo(javaType, type, pool);
	}

	public static enum TypeEnum {
		BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, DECIMAL, DATE, DATETIME, STRING, UUID, OBJECT,
		VOID, LIST, SET, MAP, MESSAGE, ENUM
	}

	public static class Pool {
		private final Map<Type, TypeInfo> map = Maps.newHashMap();

		public TypeInfo get(final Type javaType) {
			TypeInfo info = map.get(javaType);
			if (info != null) return info;
			return info(javaType, this);
		}

		private void add(final Type javaType, final TypeInfo typeInfo) {
			checkState(!map.containsKey(javaType), "Duplicate type info for " + javaType);
			map.put(javaType, typeInfo);
		}
	}

	public static class TypeInfo {
		protected final Type javaType;
		protected final TypeEnum type;
		protected final Pool pool;

		protected TypeInfo(final Type javaType, final TypeEnum type, final Pool pool) {
			this.javaType = javaType;
			this.type = type;
			this.pool = pool;

			pool.add(javaType, this);
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(type).toString();
		}

		public Type getJavaType() { return javaType; }
		public Class<?> getJavaClass() { return (Class<?>) javaType; }
		public TypeEnum getType() { return type; }
		public Pool getPool() { return pool; }

		public ListInfo asList() { return (ListInfo) this; }
		public SetInfo asSet() { return (SetInfo) this; }
		public MapInfo asMap() { return (MapInfo) this; }
		public MessageInfo asMesage() { return (MessageInfo) this; }
		public EnumInfo asEnum() { return (EnumInfo) this; }

		protected TypeInfo infoOf(final Type javaType1) { return pool.get(javaType1); }
	}

	public static class ListInfo extends TypeInfo {
		private final TypeInfo element;
		protected ListInfo(final Type javaType, final Pool pool) {
			super(javaType, TypeEnum.LIST, pool);
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
		protected SetInfo(final Type javaType, final Pool pool) {
			super(javaType, TypeEnum.SET, pool);
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
		protected MapInfo(final Type javaType, final Pool pool) {
			super(javaType, TypeEnum.MAP, pool);
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
		protected EnumInfo(final Type javaType, final Pool pool) {
			super(javaType, TypeEnum.ENUM, pool);

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
		private final String discriminator;

		protected MessageInfo(final Class<?> cls, final Pool pool) {
			super(cls, TypeEnum.MESSAGE, pool);
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
				Discriminator fieldAnnotation = cls.getAnnotation(Discriminator.class);
				Subtypes subtypesAnnotation = cls.getAnnotation(Subtypes.class);
				checkNotNull(fieldAnnotation, "Discriminator annotation must be present in %s", cls);
				checkNotNull(subtypesAnnotation, "Subtypes annotation must be present in %s", cls);

				ImmutableMap.Builder<String, MessageInfo> builder = ImmutableMap.builder();
				for (Subtype value : subtypesAnnotation.value()) {
					String name = value.type();
					MessageInfo info = (MessageInfo) infoOf(value.value());
					builder.put(name, info);
				}
				subtypes = builder.build();
				discriminator = fieldAnnotation.value();
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
		public String getDiscriminator() { return discriminator; }
		public Map<String, MessageInfo> getSubtypes() { return subtypes; }
	}

	public static class FieldInfo {
		private final MessageInfo message;
		private final Field field;
		private final TypeInfo type;
		private final Method messageGetter;
		private final Method builderSetter;

		public FieldInfo(final MessageInfo message, final Field field) {
			this.message = checkNotNull(message);
			this.field = checkNotNull(field);
			type = message.infoOf(field.getGenericType());

			String name = field.getName();
			String upperFirst = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			String getterName = "get" + upperFirst;
			String setterName = "set" + upperFirst;
			try {
				messageGetter = message.getJavaClass().getMethod(getterName);
				builderSetter = message.builderClass.getMethod(setterName, field.getType());
			} catch (NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			return Objects.toStringHelper(this).addValue(getName()).addValue(getType()).toString();
		}

		public String getName() { return field.getName(); }
		public MessageInfo getMessage() { return message; }
		public Field getField() { return field; }
		public TypeInfo getType() { return type; }

		public Object get(final Object message) {
			try {
				return messageGetter.invoke(message);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			} catch (InvocationTargetException e) {
				throw new RuntimeException(e);
			}
		}

		public void set(final Object builder, final Object value) {
			try {
				builderSetter.invoke(builder, value);
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
