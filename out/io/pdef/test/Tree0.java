package io.pdef.test;

/** First inheritance tree. */
public class Tree0 extends io.pdef.GeneratedMessage {
    protected io.pdef.test.TreeType type;

    public Tree0() {}

    public Tree0(final Builder builder) {
        super(builder);
        this.type = builder.type;
    }

    protected Tree0(final java.util.Map<?, ?> map) {
        super(map);
        this.type = io.pdef.test.TreeType.parse(map.get("type"));
    }
    
    public static Tree0 parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static Tree0 parse(final java.util.Map<?, ?> map) {
        io.pdef.test.TreeType type = io.pdef.test.TreeType.parse(map.get("type"));
        if (type != null) {
            switch (type) {
                case ONE: return io.pdef.test.Tree1.parse(map);
                case TWO: return io.pdef.test.Tree2.parse(map);
            }
        }
        return new Tree0(map);
    }

    public static Tree0 parseFromJson(final String s) {
        Object object = io.pdef.json.Json.parse(s);
        return parse(object);
    }

    public io.pdef.test.TreeType getType() {
        return type != null ? type : io.pdef.test.TreeType.instance();
    }

    public boolean hasType() {
        return type != null;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<String, Object> map = super.serialize();
        if (hasType()) map.put("type", io.pdef.test.TreeType.serialize(this.type));
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
    public io.pdef.descriptors.Descriptor<? extends Tree0> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final Tree0 object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.GeneratedMessage.Builder {
        protected io.pdef.test.TreeType type;

        protected Builder() {}

        protected Builder(final Tree0 message) {
            super(message);
            this.type = message.type;
            
        }

        protected Builder(final io.pdef.test.TreeType type) {
            this();
            this.type = type;
        }

        public io.pdef.test.TreeType getType() {
            return type != null ? type : io.pdef.test.TreeType.instance();
        }

        public Builder setType(final io.pdef.test.TreeType value) {
            this.type = value;
            return this;
        }

        public Builder clearType() {
            this.type = null;
            return this;
        }

        public boolean hasType() {
            return type != null;
        }

        @Override
        public Tree0 build() {
            return new Tree0(this);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("type", type)
                    .toString();
        }

        @Override
        public boolean equals(final Object o) {
                        if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Tree0 cast = (Tree0) o;

            if (this.type != null
                    ? !this.type.equals(cast.type)
                    : cast.type != null) return false;
            
            return true;
            
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return com.google.common.base.Objects.hashCode(result,
                    this.type); 
                    
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Tree0 cast = (Tree0) o;

        if (this.type != null
                ? !this.type.equals(cast.type)
                : cast.type != null) return false;

        return true;
    }

    @Override
    protected int generateHashCode() {
        int result = super.generateHashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.type); 
                
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Tree0 instance() {
        return INSTANCE;
    }
    
    private static final Tree0 INSTANCE = new Tree0();
    public static final io.pdef.descriptors.Descriptor<Tree0> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<Tree0>() {
                @Override
                public Class<Tree0> getJavaClass() {
                    return Tree0.class;
                }

                @Override
                public Tree0 getDefault() {
                    return Tree0.instance();
                }

                @Override
                public Tree0 parse(final Object object) {
                    return Tree0.parse(object);
                }

                @Override
                public Object serialize(final Tree0 value) {
                    return Tree0.serialize(value);
                }
            };
}
