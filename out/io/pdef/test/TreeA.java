package io.pdef.test;

/** Second inheritance tree. */
public class TreeA extends io.pdef.test.Tree2 {

    public TreeA() {}

    public TreeA(final Builder builder) {
        super(builder);
    }

    protected TreeA(final java.util.Map<?, ?> map) {
        super(map);
    }
    
    public static TreeA parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static TreeA parse(final java.util.Map<?, ?> map) {
        return new TreeA(map);
    }

    public static TreeA parseFromJson(final String s) {
        Object object = io.pdef.json.Json.parse(s);
        return parse(object);
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<String, Object> map = super.serialize();
        return map;
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    @Override
    public Builder builderForType() {
        return builder();
    }
    
    @Override
    public io.pdef.descriptors.Descriptor<? extends TreeA> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final TreeA object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.test.Tree2.Builder {

        protected Builder() {
            super(io.pdef.test.TreeType1.A);
        }

        protected Builder(final TreeA message) {
            super(message);
            
        }

        protected Builder(final io.pdef.test.TreeType1 type1) {
            this();
            this.type1 = type1;
        }

        @Override
        public Builder setType1(final io.pdef.test.TreeType1 value) {
            super.setType1(value);
            return this;
        }

        @Override
        public Builder clearType1() {
            super.clearType1();
            return this;
        }

        @Override
        public Builder setType(final io.pdef.test.TreeType value) {
            super.setType(value);
            return this;
        }

        @Override
        public Builder clearType() {
            super.clearType();
            return this;
        }

        @Override
        public TreeA build() {
            return new TreeA(this);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("type1", type1)
                    .add("type", type)
                    .toString();
        }

        @Override
        public boolean equals(final Object o) {
            return super.equals(o);
            
        }

        @Override
        public int hashCode() {
            return super.hashCode();
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("type1", type1)
                .add("type", type)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        return super.equals(o);
    }

    @Override
    protected int generateHashCode() {
        return super.generateHashCode();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TreeA instance() {
        return INSTANCE;
    }
    
    private static final TreeA INSTANCE = new TreeA();
    public static final io.pdef.descriptors.Descriptor<TreeA> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TreeA>() {
                @Override
                public Class<TreeA> getJavaClass() {
                    return TreeA.class;
                }

                @Override
                public TreeA getDefault() {
                    return TreeA.instance();
                }

                @Override
                public TreeA parse(final Object object) {
                    return TreeA.parse(object);
                }

                @Override
                public Object serialize(final TreeA value) {
                    return TreeA.serialize(value);
                }
            };
}
