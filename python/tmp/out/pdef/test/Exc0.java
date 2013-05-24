package pdef.test;

public class Exc0 extends io.pdef.GeneratedException {
    private static final Exc0 instance = new Exc0();


    public Exc0() {
        this(new Builder());
    }

    public Exc0(final Builder builder) {
        super(builder);
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
    }

    public static Exc0 getInstance() {
        return instance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends io.pdef.GeneratedException.Builder {

        public Builder() {
            
        }

        @Override
        public Exc0 build() {
            return new Exc0(this);
        }


        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .toString();
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
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
