package io.pdef.test;

public enum TestEnum {
    ONE, TWO, THREE;

    public static TestEnum parse(final Object object) {
        return parse((String) object);
    }

    public static TestEnum parse(final String object) {
        if (object == null) return instance();
        String s = object.toUpperCase();
        if (ONE.name().equals(s)) return ONE;
        if (TWO.name().equals(s)) return TWO;
        if (THREE.name().equals(s)) return THREE;
        return instance();
    }

    public static String serialize(final TestEnum object) {
        return object == null ? null : object.name().toLowerCase();
    }

    public static TestEnum instance() {
        return ONE;
    }

    public static final io.pdef.descriptors.Descriptor<TestEnum> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TestEnum>() {
                @Override
                public Class<TestEnum> getJavaClass() {
                    return TestEnum.class;
                }

                @Override
                public TestEnum getDefault() {
                    return TestEnum.instance();
                }

                @Override
                public TestEnum parse(final Object object) {
                    return TestEnum.parse(object);
                }

                @Override
                public Object serialize(final TestEnum object) {
                    return TestEnum.serialize(object);
                }
            };
}
