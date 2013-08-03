package io.pdef.test;

public class TreeB extends io.pdef.test.Tree2 {

    public TreeB() {}

    public TreeB(final Builder builder) {
        super(builder);
    }

    protected TreeB(final java.util.Map<?, ?> map) {
        super(map);
    }
    
    public static TreeB parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static TreeB parse(final java.util.Map<?, ?> map) {
        return new TreeB(map);
    }

    public static TreeB parseFromJson(final String s) {
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
    public io.pdef.descriptors.Descriptor<? extends TreeB> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final TreeB object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.test.Tree2.Builder {

        protected Builder() {
            super(io.pdef.test.TreeType1.B);
        }

        protected Builder(final TreeB message) {
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
        public TreeB build() {
            return new TreeB(this);
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

    public static TreeB instance() {
        return INSTANCE;
    }
    
    private static final TreeB INSTANCE = new TreeB();
    public static final io.pdef.descriptors.Descriptor<TreeB> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TreeB>() {
                @Override
                public Class<TreeB> getJavaClass() {
                    return TreeB.class;
                }

                @Override
                public TreeB getDefault() {
                    return TreeB.instance();
                }

                @Override
                public TreeB parse(final Object object) {
                    return TreeB.parse(object);
                }

                @Override
                public Object serialize(final TreeB value) {
                    return TreeB.serialize(value);
                }
            };
}
