package pdef.test;

public class Message0 extends io.pdef.GeneratedMessage {
    private static final Message0 instance = new Message0();


    public Message0() {
        this(new Builder());
    }

    public Message0(final Builder builder) {
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

    public static Message0 getInstance() {
        return instance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends io.pdef.GeneratedMessage.Builder {

        public Builder() {
            
        }

        @Override
        public Message0 build() {
            return new Message0(this);
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
