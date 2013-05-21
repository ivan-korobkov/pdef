package io.pdef.jackson;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.google.common.collect.Lists;
import io.pdef.Discriminator;
import io.pdef.Subtype;
import io.pdef.Subtypes;

import java.util.List;

public class PdefJacksonAnnotationIntrospector extends AnnotationIntrospector {
	@Override
	public Version version() {
		return Version.unknownVersion();
	}

	@Override
	public TypeResolverBuilder<?> findTypeResolver(final MapperConfig<?> config,
			final AnnotatedClass ac, final JavaType baseType) {
		// See JacksonAnnotationIntrospector code.

		Discriminator discriminator = ac.getAnnotation(Discriminator.class);
		Subtypes subtypes = ac.getAnnotation(Subtypes.class);
		if (discriminator == null || subtypes == null) return null;

		StdTypeResolverBuilder resolver = constructTypeResolverBuilder();
		resolver.init(JsonTypeInfo.Id.NAME, null); // Null custom id resolver is OK.
		resolver.inclusion(JsonTypeInfo.As.PROPERTY);
		resolver.typeProperty(discriminator.value());
		resolver.defaultImpl(baseType.getRawClass());
		resolver.typeIdVisibility(true);
		return resolver;
	}

	@Override
	public List<NamedType> findSubtypes(final Annotated a) {
		Subtypes subtypes = a.getAnnotation(Subtypes.class);
		if (subtypes == null) return null;

		List<NamedType> types = Lists.newArrayList();
		for (Subtype subtype : subtypes.value()) {
			types.add(new NamedType(subtype.value(), subtype.type()));
		}
		return types;
	}

	protected StdTypeResolverBuilder constructTypeResolverBuilder() {
		return new StdTypeResolverBuilder();
	}
}
