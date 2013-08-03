package io.pdef.test;

public class Exception extends io.pdef.GeneratedException {
    protected String text;

    public Exception() {}

    public Exception(final Builder builder) {
        super(builder);
        this.text = builder.text;
    }

    protected Exception(final java.util.Map<?, ?> map) {
        super(map);
        this.text = io.pdef.descriptors.Descriptors.string.parse(map.get("text"));
    }
    
    public static Exception parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static Exception parse(final java.util.Map<?, ?> map) {
        return new Exception(map);
    }

    public static Exception parseFromJson(final String s) {
        Object object = io.pdef.json.Json.parse(s);
        return parse(object);
    }

    @javax.annotation.Nullable
    public String getText() {
        return text != null ? text : null;
    }

    public boolean hasText() {
        return text != null;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<String, Object> map = super.serialize();
        if (hasText()) map.put("text", io.pdef.descriptors.Descriptors.string.serialize(this.text));
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
    public io.pdef.descriptors.Descriptor<? extends Exception> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final Exception object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.GeneratedException.Builder {
        protected String text;

        protected Builder() {}

        protected Builder(final Exception message) {
            super(message);
            this.text = message.text;
            
        }

        public String getText() {
            return text != null ? text : null;
        }

        public Builder setText(final String value) {
            this.text = value;
            return this;
        }

        public Builder clearText() {
            this.text = null;
            return this;
        }

        public boolean hasText() {
            return text != null;
        }

        @Override
        public Exception build() {
            return new Exception(this);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("text", text)
                    .toString();
        }

        @Override
        public boolean equals(final Object o) {
                        if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            Exception cast = (Exception) o;

            if (this.text != null
                    ? !this.text.equals(cast.text)
                    : cast.text != null) return false;
            
            return true;
            
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return com.google.common.base.Objects.hashCode(result,
                    this.text); 
                    
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("text", text)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Exception cast = (Exception) o;

        if (this.text != null
                ? !this.text.equals(cast.text)
                : cast.text != null) return false;

        return true;
    }

    @Override
    protected int generateHashCode() {
        int result = super.generateHashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.text); 
                
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Exception instance() {
        return INSTANCE;
    }
    
    private static final Exception INSTANCE = new Exception();
    public static final io.pdef.descriptors.Descriptor<Exception> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<Exception>() {
                @Override
                public Class<Exception> getJavaClass() {
                    return Exception.class;
                }

                @Override
                public Exception getDefault() {
                    return Exception.instance();
                }

                @Override
                public Exception parse(final Object object) {
                    return Exception.parse(object);
                }

                @Override
                public Object serialize(final Exception value) {
                    return Exception.serialize(value);
                }
            };
}
