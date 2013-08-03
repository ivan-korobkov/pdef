package io.pdef.test;

/** Test simple message. */
public class TestSimpleMessage extends io.pdef.GeneratedMessage {
    protected boolean firstField;
    protected String secondField;
    protected io.pdef.test.TestSimpleMessage thirdField;

    public TestSimpleMessage() {}

    public TestSimpleMessage(final Builder builder) {
        super(builder);
        this.firstField = builder.firstField;
        this.secondField = builder.secondField;
        this.thirdField = builder.thirdField;
    }

    protected TestSimpleMessage(final java.util.Map<?, ?> map) {
        super(map);
        this.firstField = io.pdef.descriptors.Descriptors.bool.parse(map.get("firstField"));
        this.secondField = io.pdef.descriptors.Descriptors.string.parse(map.get("secondField"));
        this.thirdField = io.pdef.test.TestSimpleMessage.parse(map.get("thirdField"));
    }
    
    public static TestSimpleMessage parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static TestSimpleMessage parse(final java.util.Map<?, ?> map) {
        return new TestSimpleMessage(map);
    }

    public static TestSimpleMessage parseFromJson(final String s) {
        Object object = io.pdef.json.Json.parse(s);
        return parse(object);
    }

    public boolean getFirstField() {
        return firstField;
    }

    public boolean hasFirstField() {
        return true;
    }

    @javax.annotation.Nullable
    public String getSecondField() {
        return secondField != null ? secondField : null;
    }

    public boolean hasSecondField() {
        return secondField != null;
    }

    public io.pdef.test.TestSimpleMessage getThirdField() {
        return thirdField != null ? thirdField : io.pdef.test.TestSimpleMessage.instance();
    }

    public boolean hasThirdField() {
        return thirdField != null;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<String, Object> map = super.serialize();
        if (hasFirstField()) map.put("firstField", io.pdef.descriptors.Descriptors.bool.serialize(this.firstField));
        if (hasSecondField()) map.put("secondField", io.pdef.descriptors.Descriptors.string.serialize(this.secondField));
        if (hasThirdField()) map.put("thirdField", io.pdef.test.TestSimpleMessage.serialize(this.thirdField));
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
    public io.pdef.descriptors.Descriptor<? extends TestSimpleMessage> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final TestSimpleMessage object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.GeneratedMessage.Builder {
        protected boolean firstField;
        protected String secondField;
        protected io.pdef.test.TestSimpleMessage thirdField;

        protected Builder() {}

        protected Builder(final TestSimpleMessage message) {
            super(message);
            this.firstField = message.firstField;
            this.secondField = message.secondField;
            this.thirdField = message.thirdField;
            
        }

        public boolean getFirstField() {
            return firstField;
        }

        public Builder setFirstField(final boolean value) {
            this.firstField = value;
            return this;
        }

        public Builder clearFirstField() {
            this.firstField = false;
            return this;
        }

        public boolean hasFirstField() {
            return true;
        }

        public String getSecondField() {
            return secondField != null ? secondField : null;
        }

        public Builder setSecondField(final String value) {
            this.secondField = value;
            return this;
        }

        public Builder clearSecondField() {
            this.secondField = null;
            return this;
        }

        public boolean hasSecondField() {
            return secondField != null;
        }

        public io.pdef.test.TestSimpleMessage getThirdField() {
            return thirdField != null ? thirdField : io.pdef.test.TestSimpleMessage.instance();
        }

        public Builder setThirdField(final io.pdef.test.TestSimpleMessage value) {
            this.thirdField = value;
            return this;
        }

        public Builder clearThirdField() {
            this.thirdField = null;
            return this;
        }

        public boolean hasThirdField() {
            return thirdField != null;
        }

        @Override
        public TestSimpleMessage build() {
            return new TestSimpleMessage(this);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
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
            TestSimpleMessage cast = (TestSimpleMessage) o;

            if (this.firstField != cast.firstField) return false;
            if (this.secondField != null
                    ? !this.secondField.equals(cast.secondField)
                    : cast.secondField != null) return false;
            if (this.thirdField != null
                    ? !this.thirdField.equals(cast.thirdField)
                    : cast.thirdField != null) return false;
            
            return true;
            
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return com.google.common.base.Objects.hashCode(result,
                    this.firstField, 
                    this.secondField, 
                    this.thirdField); 
                    
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
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
        TestSimpleMessage cast = (TestSimpleMessage) o;

        if (this.firstField != cast.firstField) return false;
        if (this.secondField != null
                ? !this.secondField.equals(cast.secondField)
                : cast.secondField != null) return false;
        if (this.thirdField != null
                ? !this.thirdField.equals(cast.thirdField)
                : cast.thirdField != null) return false;

        return true;
    }

    @Override
    protected int generateHashCode() {
        int result = super.generateHashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.firstField, 
                this.secondField, 
                this.thirdField); 
                
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TestSimpleMessage instance() {
        return INSTANCE;
    }
    
    private static final TestSimpleMessage INSTANCE = new TestSimpleMessage();
    public static final io.pdef.descriptors.Descriptor<TestSimpleMessage> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TestSimpleMessage>() {
                @Override
                public Class<TestSimpleMessage> getJavaClass() {
                    return TestSimpleMessage.class;
                }

                @Override
                public TestSimpleMessage getDefault() {
                    return TestSimpleMessage.instance();
                }

                @Override
                public TestSimpleMessage parse(final Object object) {
                    return TestSimpleMessage.parse(object);
                }

                @Override
                public Object serialize(final TestSimpleMessage value) {
                    return TestSimpleMessage.serialize(value);
                }
            };
}
