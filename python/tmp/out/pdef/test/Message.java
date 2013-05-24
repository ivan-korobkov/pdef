package pdef.test;

@io.pdef.Discriminator("type_field")
@io.pdef.Subtypes({
        @io.pdef.Subtype(type = "first", value = pdef.test.SubMessage1.class), 
        @io.pdef.Subtype(type = "second", value = pdef.test.SubMessage2.class), 
        @io.pdef.Subtype(type = "third", value = pdef.test.SubMessage3.class)
})
public class Message extends io.pdef.GeneratedMessage {
    private static final Message instance = new Message();

    protected pdef.test.Enum type_field;
    protected boolean bool_field;
    protected short int16_field;
    protected int int32_field;
    protected long int64_field;
    protected float float_field;
    protected double double_field;
    protected decimal decimal_field;
    protected date date_field;
    protected datetime datetime_field;
    protected String string_field;
    protected uuid uuid_field;
    protected Object object_field;
    protected java.util.List<pdef.test.SubMessage1> list_field;
    protected java.util.Set<pdef.test.SubMessage2> set_field;
    protected java.util.Map<String, pdef.test.SubMessage3> map_field;

    public Message() {
        this(new Builder());
    }

    public Message(final Builder builder) {
        super(builder);
        this.type_field = builder.type_field;
        this.bool_field = builder.bool_field;
        this.int16_field = builder.int16_field;
        this.int32_field = builder.int32_field;
        this.int64_field = builder.int64_field;
        this.float_field = builder.float_field;
        this.double_field = builder.double_field;
        this.decimal_field = builder.decimal_field;
        this.date_field = builder.date_field;
        this.datetime_field = builder.datetime_field;
        this.string_field = builder.string_field;
        this.uuid_field = builder.uuid_field;
        this.object_field = builder.object_field;
        this.list_field = builder.list_field == null ? null
                : com.google.common.collect.ImmutableList.copyOf(builder.list_field);
        this.set_field = builder.set_field == null ? null
                : com.google.common.collect.ImmutableSet.copyOf(builder.set_field);
        this.map_field = builder.map_field == null ? null
                : com.google.common.collect.ImmutableMap.copyOf(builder.map_field);
    }

    @javax.annotation.Nullable
    public pdef.test.Enum getType_field() {
        return type_field != null ? type_field : null;
    }

    public boolean hasType_field() {
        return type_field != null;
    }

    public boolean getBool_field() {
        return bool_field;
    }

    public boolean hasBool_field() {
        return true;
    }

    public short getInt16_field() {
        return int16_field;
    }

    public boolean hasInt16_field() {
        return true;
    }

    public int getInt32_field() {
        return int32_field;
    }

    public boolean hasInt32_field() {
        return true;
    }

    public long getInt64_field() {
        return int64_field;
    }

    public boolean hasInt64_field() {
        return true;
    }

    public float getFloat_field() {
        return float_field;
    }

    public boolean hasFloat_field() {
        return true;
    }

    public double getDouble_field() {
        return double_field;
    }

    public boolean hasDouble_field() {
        return true;
    }

    @javax.annotation.Nullable
    public decimal getDecimal_field() {
        return decimal_field != null ? decimal_field : null;
    }

    public boolean hasDecimal_field() {
        return decimal_field != null;
    }

    @javax.annotation.Nullable
    public date getDate_field() {
        return date_field != null ? date_field : null;
    }

    public boolean hasDate_field() {
        return date_field != null;
    }

    @javax.annotation.Nullable
    public datetime getDatetime_field() {
        return datetime_field != null ? datetime_field : null;
    }

    public boolean hasDatetime_field() {
        return datetime_field != null;
    }

    @javax.annotation.Nullable
    public String getString_field() {
        return string_field != null ? string_field : null;
    }

    public boolean hasString_field() {
        return string_field != null;
    }

    @javax.annotation.Nullable
    public uuid getUuid_field() {
        return uuid_field != null ? uuid_field : null;
    }

    public boolean hasUuid_field() {
        return uuid_field != null;
    }

    @javax.annotation.Nullable
    public Object getObject_field() {
        return object_field != null ? object_field : null;
    }

    public boolean hasObject_field() {
        return object_field != null;
    }

    public java.util.List<pdef.test.SubMessage1> getList_field() {
        return list_field != null ? list_field : com.google.common.collect.ImmutableList.<pdef.test.SubMessage1>of();
    }

    public boolean hasList_field() {
        return list_field != null;
    }

    public java.util.Set<pdef.test.SubMessage2> getSet_field() {
        return set_field != null ? set_field : com.google.common.collect.ImmutableSet.<pdef.test.SubMessage2>of();
    }

    public boolean hasSet_field() {
        return set_field != null;
    }

    public java.util.Map<String, pdef.test.SubMessage3> getMap_field() {
        return map_field != null ? map_field : com.google.common.collect.ImmutableMap.<String, pdef.test.SubMessage3>of();
    }

    public boolean hasMap_field() {
        return map_field != null;
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
        builder.setType_field(this.type_field);
        builder.setBool_field(this.bool_field);
        builder.setInt16_field(this.int16_field);
        builder.setInt32_field(this.int32_field);
        builder.setInt64_field(this.int64_field);
        builder.setFloat_field(this.float_field);
        builder.setDouble_field(this.double_field);
        builder.setDecimal_field(this.decimal_field);
        builder.setDate_field(this.date_field);
        builder.setDatetime_field(this.datetime_field);
        builder.setString_field(this.string_field);
        builder.setUuid_field(this.uuid_field);
        builder.setObject_field(this.object_field);
        builder.setList_field(this.list_field);
        builder.setSet_field(this.set_field);
        builder.setMap_field(this.map_field);
    }

    public static Message getInstance() {
        return instance;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends io.pdef.GeneratedMessage.Builder {
        protected pdef.test.Enum type_field;
        protected boolean bool_field;
        protected short int16_field;
        protected int int32_field;
        protected long int64_field;
        protected float float_field;
        protected double double_field;
        protected decimal decimal_field;
        protected date date_field;
        protected datetime datetime_field;
        protected String string_field;
        protected uuid uuid_field;
        protected Object object_field;
        protected java.util.List<pdef.test.SubMessage1> list_field;
        protected java.util.Set<pdef.test.SubMessage2> set_field;
        protected java.util.Map<String, pdef.test.SubMessage3> map_field;

        public Builder() {
            
        }

        public Builder(final pdef.test.Enum type_field) {
            this.type_field = type_field;
        }

        public pdef.test.Enum getType_field() {
            return type_field != null ? type_field : null;
        }

        public Builder setType_field(final pdef.test.Enum value) {
            this.type_field = value;
            return this;
        }

        public Builder clearType_field() {
            this.type_field = null;
            return this;
        }

        public boolean hasType_field() {
            return type_field != null;
        }

        public boolean getBool_field() {
            return bool_field;
        }

        public Builder setBool_field(final boolean value) {
            this.bool_field = value;
            return this;
        }

        public Builder clearBool_field() {
            this.bool_field = false;
            return this;
        }

        public boolean hasBool_field() {
            return true;
        }

        public short getInt16_field() {
            return int16_field;
        }

        public Builder setInt16_field(final short value) {
            this.int16_field = value;
            return this;
        }

        public Builder clearInt16_field() {
            this.int16_field = (short) 0;
            return this;
        }

        public boolean hasInt16_field() {
            return true;
        }

        public int getInt32_field() {
            return int32_field;
        }

        public Builder setInt32_field(final int value) {
            this.int32_field = value;
            return this;
        }

        public Builder clearInt32_field() {
            this.int32_field = 0;
            return this;
        }

        public boolean hasInt32_field() {
            return true;
        }

        public long getInt64_field() {
            return int64_field;
        }

        public Builder setInt64_field(final long value) {
            this.int64_field = value;
            return this;
        }

        public Builder clearInt64_field() {
            this.int64_field = 0L;
            return this;
        }

        public boolean hasInt64_field() {
            return true;
        }

        public float getFloat_field() {
            return float_field;
        }

        public Builder setFloat_field(final float value) {
            this.float_field = value;
            return this;
        }

        public Builder clearFloat_field() {
            this.float_field = 0f;
            return this;
        }

        public boolean hasFloat_field() {
            return true;
        }

        public double getDouble_field() {
            return double_field;
        }

        public Builder setDouble_field(final double value) {
            this.double_field = value;
            return this;
        }

        public Builder clearDouble_field() {
            this.double_field = 0.0;
            return this;
        }

        public boolean hasDouble_field() {
            return true;
        }

        public decimal getDecimal_field() {
            return decimal_field != null ? decimal_field : null;
        }

        public Builder setDecimal_field(final decimal value) {
            this.decimal_field = value;
            return this;
        }

        public Builder clearDecimal_field() {
            this.decimal_field = null;
            return this;
        }

        public boolean hasDecimal_field() {
            return decimal_field != null;
        }

        public date getDate_field() {
            return date_field != null ? date_field : null;
        }

        public Builder setDate_field(final date value) {
            this.date_field = value;
            return this;
        }

        public Builder clearDate_field() {
            this.date_field = null;
            return this;
        }

        public boolean hasDate_field() {
            return date_field != null;
        }

        public datetime getDatetime_field() {
            return datetime_field != null ? datetime_field : null;
        }

        public Builder setDatetime_field(final datetime value) {
            this.datetime_field = value;
            return this;
        }

        public Builder clearDatetime_field() {
            this.datetime_field = null;
            return this;
        }

        public boolean hasDatetime_field() {
            return datetime_field != null;
        }

        public String getString_field() {
            return string_field != null ? string_field : null;
        }

        public Builder setString_field(final String value) {
            this.string_field = value;
            return this;
        }

        public Builder clearString_field() {
            this.string_field = null;
            return this;
        }

        public boolean hasString_field() {
            return string_field != null;
        }

        public uuid getUuid_field() {
            return uuid_field != null ? uuid_field : null;
        }

        public Builder setUuid_field(final uuid value) {
            this.uuid_field = value;
            return this;
        }

        public Builder clearUuid_field() {
            this.uuid_field = null;
            return this;
        }

        public boolean hasUuid_field() {
            return uuid_field != null;
        }

        public Object getObject_field() {
            return object_field != null ? object_field : null;
        }

        public Builder setObject_field(final Object value) {
            this.object_field = value;
            return this;
        }

        public Builder clearObject_field() {
            this.object_field = null;
            return this;
        }

        public boolean hasObject_field() {
            return object_field != null;
        }

        public java.util.List<pdef.test.SubMessage1> getList_field() {
            return list_field != null ? list_field : com.google.common.collect.ImmutableList.<pdef.test.SubMessage1>of();
        }

        public Builder setList_field(final java.util.List<pdef.test.SubMessage1> value) {
            this.list_field = value;
            return this;
        }

        public Builder clearList_field() {
            this.list_field = null;
            return this;
        }

        public boolean hasList_field() {
            return list_field != null;
        }

        public java.util.Set<pdef.test.SubMessage2> getSet_field() {
            return set_field != null ? set_field : com.google.common.collect.ImmutableSet.<pdef.test.SubMessage2>of();
        }

        public Builder setSet_field(final java.util.Set<pdef.test.SubMessage2> value) {
            this.set_field = value;
            return this;
        }

        public Builder clearSet_field() {
            this.set_field = null;
            return this;
        }

        public boolean hasSet_field() {
            return set_field != null;
        }

        public java.util.Map<String, pdef.test.SubMessage3> getMap_field() {
            return map_field != null ? map_field : com.google.common.collect.ImmutableMap.<String, pdef.test.SubMessage3>of();
        }

        public Builder setMap_field(final java.util.Map<String, pdef.test.SubMessage3> value) {
            this.map_field = value;
            return this;
        }

        public Builder clearMap_field() {
            this.map_field = null;
            return this;
        }

        public boolean hasMap_field() {
            return map_field != null;
        }

        @Override
        public Message build() {
            return new Message(this);
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
                    .add("set_field", set_field)
                    .add("map_field", map_field)
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
                .add("set_field", set_field)
                .add("map_field", map_field)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Message cast = (Message) o;

        if (this.type_field != null ? !this.type_field.equals(cast.type_field)
                : cast.type_field != null) return false;
        if (this.bool_field != cast.bool_field) return false;
        if (this.int16_field != cast.int16_field) return false;
        if (this.int32_field != cast.int32_field) return false;
        if (this.int64_field != cast.int64_field) return false;
        if (this.float_field != cast.float_field) return false;
        if (this.double_field != cast.double_field) return false;
        if (this.decimal_field != null ? !this.decimal_field.equals(cast.decimal_field)
                : cast.decimal_field != null) return false;
        if (this.date_field != null ? !this.date_field.equals(cast.date_field)
                : cast.date_field != null) return false;
        if (this.datetime_field != null ? !this.datetime_field.equals(cast.datetime_field)
                : cast.datetime_field != null) return false;
        if (this.string_field != null ? !this.string_field.equals(cast.string_field)
                : cast.string_field != null) return false;
        if (this.uuid_field != null ? !this.uuid_field.equals(cast.uuid_field)
                : cast.uuid_field != null) return false;
        if (this.object_field != null ? !this.object_field.equals(cast.object_field)
                : cast.object_field != null) return false;
        if (this.list_field != null ? !this.list_field.equals(cast.list_field)
                : cast.list_field != null) return false;
        if (this.set_field != null ? !this.set_field.equals(cast.set_field)
                : cast.set_field != null) return false;
        if (this.map_field != null ? !this.map_field.equals(cast.map_field)
                : cast.map_field != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.type_field, 
                this.bool_field, 
                this.int16_field, 
                this.int32_field, 
                this.int64_field, 
                this.float_field, 
                this.double_field, 
                this.decimal_field, 
                this.date_field, 
                this.datetime_field, 
                this.string_field, 
                this.uuid_field, 
                this.object_field, 
                this.list_field, 
                this.set_field, 
                this.map_field); 
                
    }
}
