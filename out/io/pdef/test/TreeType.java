package io.pdef.test;

public enum TreeType {
    BASE, ONE, TWO;

    public static TreeType parse(final Object object) {
        return parse((String) object);
    }

    public static TreeType parse(final String object) {
        if (object == null) return instance();
        String s = object.toUpperCase();
        if (BASE.name().equals(s)) return BASE;
        if (ONE.name().equals(s)) return ONE;
        if (TWO.name().equals(s)) return TWO;
        return instance();
    }

    public static String serialize(final TreeType object) {
        return object == null ? null : object.name().toLowerCase();
    }

    public static TreeType instance() {
        return BASE;
    }

    public static final io.pdef.descriptors.Descriptor<TreeType> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TreeType>() {
                @Override
                public Class<TreeType> getJavaClass() {
                    return TreeType.class;
                }

                @Override
                public TreeType getDefault() {
                    return TreeType.instance();
                }

                @Override
                public TreeType parse(final Object object) {
                    return TreeType.parse(object);
                }

                @Override
                public Object serialize(final TreeType object) {
                    return TreeType.serialize(object);
                }
            };
}
