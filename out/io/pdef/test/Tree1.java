package io.pdef.test;

public class Tree1 extends io.pdef.test.Tree0 {

    public Tree1() {}

    public Tree1(final Builder builder) {
        super(builder);
    }

    protected Tree1(final java.util.Map<?, ?> map) {
        super(map);
    }
    
    public static Tree1 parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static Tree1 parse(final java.util.Map<?, ?> map) {
        io.pdef.test.TreeType type = io.pdef.test.TreeType.parse(map.get("type"));
        if (type != null) {
            switch (type) {
                case TWO: return io.pdef.test.Tree2.parse(map);
            }
        }
        return new Tree1(map);
    }

    public static Tree1 parseFromJson(final String s) {
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
    public io.pdef.descriptors.Descriptor<? extends Tree1> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final Tree1 object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.test.Tree0.Builder {

        protected Builder() {
            super(io.pdef.test.TreeType.ONE);
        }

        protected Builder(final Tree1 message) {
            super(message);
            
        }

        protected Builder(final io.pdef.test.TreeType type) {
            this();
            this.type = type;
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
        public Tree1 build() {
            return new Tree1(this);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
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

    public static Tree1 instance() {
        return INSTANCE;
    }
    
    private static final Tree1 INSTANCE = new Tree1();
    public static final io.pdef.descriptors.Descriptor<Tree1> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<Tree1>() {
                @Override
                public Class<Tree1> getJavaClass() {
                    return Tree1.class;
                }

                @Override
                public Tree1 getDefault() {
                    return Tree1.instance();
                }

                @Override
                public Tree1 parse(final Object object) {
                    return Tree1.parse(object);
                }

                @Override
                public Object serialize(final Tree1 value) {
                    return Tree1.serialize(value);
                }
            };
}
