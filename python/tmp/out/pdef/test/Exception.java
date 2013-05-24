package pdef.test;

public class Exception extends io.pdef.GeneratedException {
    private static final Exception instance = new Exception();

    protected String code;

    public Exception() {
        this(new Builder());
    }

    public Exception(final Builder builder) {
        super(builder);
        this.code = builder.code;
    }

    @javax.annotation.Nullable
    public String getCode() {
        return code != null ? code : null;
    }

    public boolean hasCode() {
        return code != null;
    }

    @Override
    public Builder newBuilderForType() {
        return builder();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = newBuilderForType();
        fill(builder);
        return builder;
    }

    protected void fill(Builder builder) {
        super.fill(builder);
        builder.setCode(this.code);
    }

    public static Exception getInstance() {
        return instance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends io.pdef.GeneratedException.Builder {
        protected String code;

        public Builder() {
            
        }

        public String getCode() {
            return code != null ? code : null;
        }

        public Builder setCode(final String value) {
            this.code = value;
            return this;
        }

        public Builder clearCode() {
            this.code = null;
            return this;
        }

        public boolean hasCode() {
            return code != null;
        }

        @Override
        public Exception build() {
            return new Exception(this);
        }


        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("code", code)
                    .toString();
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("code", code)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Exception cast = (Exception) o;

        if (this.code != null ? !this.code.equals(cast.code)
                : cast.code != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.code); 
                
    }
}
