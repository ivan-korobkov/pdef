package io.pdef.test;

public enum TreeType1 {
    BASE, A, B;

    public static TreeType1 parse(final Object object) {
        return parse((String) object);
    }

    public static TreeType1 parse(final String object) {
        if (object == null) return instance();
        String s = object.toUpperCase();
        if (BASE.name().equals(s)) return BASE;
        if (A.name().equals(s)) return A;
        if (B.name().equals(s)) return B;
        return instance();
    }

    public static String serialize(final TreeType1 object) {
        return object == null ? null : object.name().toLowerCase();
    }

    public static TreeType1 instance() {
        return BASE;
    }

    public static final io.pdef.descriptors.Descriptor<TreeType1> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TreeType1>() {
                @Override
                public Class<TreeType1> getJavaClass() {
                    return TreeType1.class;
                }

                @Override
                public TreeType1 getDefault() {
                    return TreeType1.instance();
                }

                @Override
                public TreeType1 parse(final Object object) {
                    return TreeType1.parse(object);
                }

                @Override
                public Object serialize(final TreeType1 object) {
                    return TreeType1.serialize(object);
                }
            };
}
