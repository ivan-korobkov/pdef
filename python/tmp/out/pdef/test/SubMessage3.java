package pdef.test;

public class SubMessage3 extends pdef.test.SubMessage2 {
    private static final SubMessage3 instance = new SubMessage3();

    protected int sub_field2;

    public SubMessage3() {
        this(new Builder());
    }

    public SubMessage3(final Builder builder) {
        super(builder);
        this.sub_field2 = builder.sub_field2;
    }

    public int getSub_field2() {
        return sub_field2;
    }

    public boolean hasSub_field2() {
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
        builder.setSub_field2(this.sub_field2);
    }

    public static SubMessage3 getInstance() {
        return instance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends pdef.test.SubMessage2.Builder {
        protected int sub_field2;

        public Builder() {
            super(pdef.test.Enum.THIRD);
        }

        public int getSub_field2() {
            return sub_field2;
        }

        public Builder setSub_field2(final int value) {
            this.sub_field2 = value;
            return this;
        }

        public Builder clearSub_field2() {
            this.sub_field2 = 0;
            return this;
        }

        public boolean hasSub_field2() {
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
        public Builder setSub_field1(final int value) {
            super.setSub_field1(value);
            return this;
        }

        @Override
        public Builder clearSub_field1() {
            super.clearSub_field1();
            return this;
        }

        @Override
        public SubMessage3 build() {
            return new SubMessage3(this);
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
                    .add("sub_field2", sub_field2)
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
                .add("sub_field2", sub_field2)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SubMessage3 cast = (SubMessage3) o;

        if (this.sub_field2 != cast.sub_field2) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.sub_field2); 
                
    }
}
