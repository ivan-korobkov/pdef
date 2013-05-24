package pdef.test;

@io.pdef.Discriminator("type_field")
@io.pdef.Subtypes({
        @io.pdef.Subtype(type = "third", value = pdef.test.SubMessage3.class)
})
public class SubMessage2 extends pdef.test.Message {
    private static final SubMessage2 instance = new SubMessage2();

    protected int sub_field1;

    public SubMessage2() {
        this(new Builder());
    }

    public SubMessage2(final Builder builder) {
        super(builder);
        this.sub_field1 = builder.sub_field1;
    }

    public int getSub_field1() {
        return sub_field1;
    }

    public boolean hasSub_field1() {
        return true;
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
        builder.setSub_field1(this.sub_field1);
    }

    public static SubMessage2 getInstance() {
        return instance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends pdef.test.Message.Builder {
        protected int sub_field1;

        public Builder() {
            super(pdef.test.Enum.SECOND);
        }

        public Builder(final pdef.test.Enum type_field) {
            this.type_field = type_field;
        }

        public int getSub_field1() {
            return sub_field1;
        }

        public Builder setSub_field1(final int value) {
            this.sub_field1 = value;
            return this;
        }

        public Builder clearSub_field1() {
            this.sub_field1 = 0;
            return this;
        }

        public boolean hasSub_field1() {
            return true;
        }

        @Override
        public Builder setType_field(final pdef.test.Enum value) {
            super.setType_field(value);
            return this;
        }

        @Override
        public Builder clearType_field() {
            super.clearType_field();
            return this;
        }

        @Override
        public Builder setBool_field(final boolean value) {
            super.setBool_field(value);
            return this;
        }

        @Override
        public Builder clearBool_field() {
            super.clearBool_field();
            return this;
        }

        @Override
        public Builder setInt16_field(final short value) {
            super.setInt16_field(value);
            return this;
        }

        @Override
        public Builder clearInt16_field() {
            super.clearInt16_field();
            return this;
        }

        @Override
        public Builder setInt32_field(final int value) {
            super.setInt32_field(value);
            return this;
        }

        @Override
        public Builder clearInt32_field() {
            super.clearInt32_field();
            return this;
        }

        @Override
        public Builder setInt64_field(final long value) {
            super.setInt64_field(value);
            return this;
        }

        @Override
        public Builder clearInt64_field() {
            super.clearInt64_field();
            return this;
        }

        @Override
        public Builder setFloat_field(final float value) {
            super.setFloat_field(value);
            return this;
        }

        @Override
        public Builder clearFloat_field() {
            super.clearFloat_field();
            return this;
        }

        @Override
        public Builder setDouble_field(final double value) {
            super.setDouble_field(value);
            return this;
        }

        @Override
        public Builder clearDouble_field() {
            super.clearDouble_field();
            return this;
        }

        @Override
        public Builder setDecimal_field(final decimal value) {
            super.setDecimal_field(value);
            return this;
        }

        @Override
        public Builder clearDecimal_field() {
            super.clearDecimal_field();
            return this;
        }

        @Override
        public Builder setDate_field(final date value) {
            super.setDate_field(value);
            return this;
        }

        @Override
        public Builder clearDate_field() {
            super.clearDate_field();
            return this;
        }

        @Override
        public Builder setDatetime_field(final datetime value) {
            super.setDatetime_field(value);
            return this;
        }

        @Override
        public Builder clearDatetime_field() {
            super.clearDatetime_field();
            return this;
        }

        @Override
        public Builder setString_field(final String value) {
            super.setString_field(value);
            return this;
        }

        @Override
        public Builder clearString_field() {
            super.clearString_field();
            return this;
        }

        @Override
        public Builder setUuid_field(final uuid value) {
            super.setUuid_field(value);
            return this;
        }

        @Override
        public Builder clearUuid_field() {
            super.clearUuid_field();
            return this;
        }

        @Override
        public Builder setObject_field(final Object value) {
            super.setObject_field(value);
            return this;
        }

        @Override
        public Builder clearObject_field() {
            super.clearObject_field();
            return this;
        }

        @Override
        public Builder setList_field(final java.util.List<pdef.test.SubMessage1> value) {
            super.setList_field(value);
            return this;
        }

        @Override
        public Builder clearList_field() {
            super.clearList_field();
            return this;
        }

        @Override
        public SubMessage2 build() {
            return new SubMessage2(this);
        }


        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("type_field", type_field)
                    .add("bool_field", bool_field)
                    .add("int16_field", int16_field)
                    .add("int32_field", int32_field)
                    .add("int64_field", int64_field)
                    .add("float_field", float_field)
                    .add("double_field", double_field)
                    .add("decimal_field", decimal_field)
                    .add("date_field", date_field)
                    .add("datetime_field", datetime_field)
                    .add("string_field", string_field)
                    .add("uuid_field", uuid_field)
                    .add("object_field", object_field)
                    .add("list_field", list_field)
                    .add("sub_field1", sub_field1)
                    .toString();
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("type_field", type_field)
                .add("bool_field", bool_field)
                .add("int16_field", int16_field)
                .add("int32_field", int32_field)
                .add("int64_field", int64_field)
                .add("float_field", float_field)
                .add("double_field", double_field)
                .add("decimal_field", decimal_field)
                .add("date_field", date_field)
                .add("datetime_field", datetime_field)
                .add("string_field", string_field)
                .add("uuid_field", uuid_field)
                .add("object_field", object_field)
                .add("list_field", list_field)
                .add("sub_field1", sub_field1)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SubMessage2 cast = (SubMessage2) o;

        if (this.sub_field1 != cast.sub_field1) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.sub_field1); 
                
    }
}
