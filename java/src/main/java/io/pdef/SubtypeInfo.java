package io.pdef;

import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class SubtypeInfo {
	private final Class<?> cls;
	private final String discriminator;
	private final Map<String, Class<?>> subtypes;

	@Nullable
	public static SubtypeInfo ofClassOrNull(final Class<?> cls) {
		if (!hasSubtypes(cls)) return null;
		return new SubtypeInfo(cls);
	}

	private SubtypeInfo(final Class<?> cls) {
		this.cls = checkNotNull(cls);
		checkArgument(hasSubtypes(cls), "No subtypes in %s", cls);

		Discriminator fieldAnnotation = cls.getAnnotation(Discriminator.class);
		Subtypes subtypesAnnotation = cls.getAnnotation(Subtypes.class);
		checkNotNull(fieldAnnotation, "Discriminator annotation must be present in %s", cls);
		checkNotNull(subtypesAnnotation, "Subtypes annotation must be present in %s", cls);

		discriminator = fieldAnnotation.value();
		checkNotNull(discriminator, "Discriminator %s is not found in %s",
				fieldAnnotation.value(), cls);

		ImmutableMap.Builder<String, Class<?>> builder = ImmutableMap.builder();
		for (Subtype value : subtypesAnnotation.value()) {
			String name = value.type();
			builder.put(name, value.value());
		}
		subtypes = builder.build();
	}

	public Class<?> getCls() {
		return cls;
	}

	public String getDiscriminator() {
		return discriminator;
	}

	public Map<String, Class<?>> getSubtypes() {
		return subtypes;
	}

	public static boolean hasSubtypes(final Class<?> cls) {
		return cls.isAnnotationPresent(Subtypes.class);
	}

	public Class<?> getSubtype(final String value) {
		return subtypes.get(value);
	}
}
