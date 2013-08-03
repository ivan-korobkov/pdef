package io.pdef.test;

/** Test simple submessage. */
public class TestSimpleSubmessage extends io.pdef.test.TestSimpleMessage {
    protected float forthField;

    public TestSimpleSubmessage() {}

    public TestSimpleSubmessage(final Builder builder) {
        super(builder);
        this.forthField = builder.forthField;
    }

    protected TestSimpleSubmessage(final java.util.Map<?, ?> map) {
        super(map);
        this.forthField = io.pdef.descriptors.Descriptors.float0.parse(map.get("forthField"));
    }
    
    public static TestSimpleSubmessage parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static TestSimpleSubmessage parse(final java.util.Map<?, ?> map) {
        return new TestSimpleSubmessage(map);
    }

    public static TestSimpleSubmessage parseFromJson(final String s) {
        Object object = io.pdef.json.Json.parse(s);
        return parse(object);
    }

    public float getForthField() {
        return forthField;
    }

    public boolean hasForthField() {
        return true;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<String, Object> map = super.serialize();
        if (hasForthField()) map.put("forthField", io.pdef.descriptors.Descriptors.float0.serialize(this.forthField));
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
    public io.pdef.descriptors.Descriptor<? extends TestSimpleSubmessage> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final TestSimpleSubmessage object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.test.TestSimpleMessage.Builder {
        protected float forthField;

        protected Builder() {}

        protected Builder(final TestSimpleSubmessage message) {
            super(message);
            this.forthField = message.forthField;
            
        }

        public float getForthField() {
            return forthField;
        }

        public Builder setForthField(final float value) {
            this.forthField = value;
            return this;
        }

        public Builder clearForthField() {
            this.forthField = 0f;
            return this;
        }

        public boolean hasForthField() {
            return true;
        }

        @Override
        public Builder setFirstField(final boolean value) {
            super.setFirstField(value);
            return this;
        }

        @Override
        public Builder clearFirstField() {
            super.clearFirstField();
            return this;
        }

        @Override
        public Builder setSecondField(final String value) {
            super.setSecondField(value);
            return this;
        }

        @Override
        public Builder clearSecondField() {
            super.clearSecondField();
            return this;
        }

        @Override
        public Builder setThirdField(final io.pdef.test.TestSimpleMessage value) {
            super.setThirdField(value);
            return this;
        }

        @Override
        public Builder clearThirdField() {
            super.clearThirdField();
            return this;
        }

        @Override
        public TestSimpleSubmessage build() {
            return new TestSimpleSubmessage(this);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("forthField", forthField)
                    .add("firstField", firstField)
                    .add("secondField", secondField)
                    .add("thirdField", thirdField)
                    .toString();
        }

        @Override
        public boolean equals(final Object o) {
                        if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            TestSimpleSubmessage cast = (TestSimpleSubmessage) o;

            if (this.forthField != cast.forthField) return false;
            
            return true;
            
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return com.google.common.base.Objects.hashCode(result,
                    this.forthField); 
                    
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("forthField", forthField)
                .add("firstField", firstField)
                .add("secondField", secondField)
                .add("thirdField", thirdField)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestSimpleSubmessage cast = (TestSimpleSubmessage) o;

        if (this.forthField != cast.forthField) return false;

        return true;
    }

    @Override
    protected int generateHashCode() {
        int result = super.generateHashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.forthField); 
                
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TestSimpleSubmessage instance() {
        return INSTANCE;
    }
    
    private static final TestSimpleSubmessage INSTANCE = new TestSimpleSubmessage();
    public static final io.pdef.descriptors.Descriptor<TestSimpleSubmessage> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TestSimpleSubmessage>() {
                @Override
                public Class<TestSimpleSubmessage> getJavaClass() {
                    return TestSimpleSubmessage.class;
                }

                @Override
                public TestSimpleSubmessage getDefault() {
                    return TestSimpleSubmessage.instance();
                }

                @Override
                public TestSimpleSubmessage parse(final Object object) {
                    return TestSimpleSubmessage.parse(object);
                }

                @Override
                public Object serialize(final TestSimpleSubmessage value) {
                    return TestSimpleSubmessage.serialize(value);
                }
            };
}
