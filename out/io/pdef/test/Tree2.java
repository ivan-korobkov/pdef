package io.pdef.test;

public class Tree2 extends io.pdef.test.Tree1 {
    protected io.pdef.test.TreeType1 type1;

    public Tree2() {}

    public Tree2(final Builder builder) {
        super(builder);
        this.type1 = builder.type1;
    }

    protected Tree2(final java.util.Map<?, ?> map) {
        super(map);
        this.type1 = io.pdef.test.TreeType1.parse(map.get("type1"));
    }
    
    public static Tree2 parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static Tree2 parse(final java.util.Map<?, ?> map) {
        io.pdef.test.TreeType1 type = io.pdef.test.TreeType1.parse(map.get("type1"));
        if (type != null) {
            switch (type) {
                case A: return io.pdef.test.TreeA.parse(map);
                case B: return io.pdef.test.TreeB.parse(map);
            }
        }
        return new Tree2(map);
    }

    public static Tree2 parseFromJson(final String s) {
        Object object = io.pdef.json.Json.parse(s);
        return parse(object);
    }

    public io.pdef.test.TreeType1 getType1() {
        return type1 != null ? type1 : io.pdef.test.TreeType1.instance();
    }

    public boolean hasType1() {
        return type1 != null;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<String, Object> map = super.serialize();
        if (hasType1()) map.put("type1", io.pdef.test.TreeType1.serialize(this.type1));
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
    public io.pdef.descriptors.Descriptor<? extends Tree2> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final Tree2 object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.test.Tree1.Builder {
        protected io.pdef.test.TreeType1 type1;

        protected Builder() {
            super(io.pdef.test.TreeType.TWO);
        }

        protected Builder(final Tree2 message) {
            super(message);
            this.type1 = message.type1;
            
        }

        protected Builder(final io.pdef.test.TreeType1 type1) {
            this();
            this.type1 = type1;
        }

        public io.pdef.test.TreeType1 getType1() {
            return type1 != null ? type1 : io.pdef.test.TreeType1.instance();
        }

        public Builder setType1(final io.pdef.test.TreeType1 value) {
            this.type1 = value;
            return this;
        }

        public Builder clearType1() {
            this.type1 = null;
            return this;
        }

        public boolean hasType1() {
            return type1 != null;
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
        public Tree2 build() {
            return new Tree2(this);
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
                        if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Tree2 cast = (Tree2) o;

            if (this.type1 != null
                    ? !this.type1.equals(cast.type1)
                    : cast.type1 != null) return false;
            
            return true;
            
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return com.google.common.base.Objects.hashCode(result,
                    this.type1); 
                    
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Tree2 cast = (Tree2) o;

        if (this.type1 != null
                ? !this.type1.equals(cast.type1)
                : cast.type1 != null) return false;

        return true;
    }

    @Override
    protected int generateHashCode() {
        int result = super.generateHashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.type1); 
                
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Tree2 instance() {
        return INSTANCE;
    }
    
    private static final Tree2 INSTANCE = new Tree2();
    public static final io.pdef.descriptors.Descriptor<Tree2> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<Tree2>() {
                @Override
                public Class<Tree2> getJavaClass() {
                    return Tree2.class;
                }

                @Override
                public Tree2 getDefault() {
                    return Tree2.instance();
                }

                @Override
                public Tree2 parse(final Object object) {
                    return Tree2.parse(object);
                }

                @Override
                public Object serialize(final Tree2 value) {
                    return Tree2.serialize(value);
                }
            };
}
