package io.pdef.test;

/** Test message with fields of all data types. */
public class TestMessage extends io.pdef.GeneratedMessage {
    protected io.pdef.test.TestEnum anEnum;
    protected boolean aBool;
    protected short anInt16;
    protected int anInt32;
    protected long anInt64;
    protected float aFloat;
    protected double aDouble;
    protected String aString;
    protected java.util.List<String> aList;
    protected java.util.Set<String> aSet;
    protected java.util.Map<String, String> aMap;
    protected io.pdef.test.TestMessage aMessage;
    protected Object anObject;

    public TestMessage() {}

    public TestMessage(final Builder builder) {
        super(builder);
        this.anEnum = builder.anEnum;
        this.aBool = builder.aBool;
        this.anInt16 = builder.anInt16;
        this.anInt32 = builder.anInt32;
        this.anInt64 = builder.anInt64;
        this.aFloat = builder.aFloat;
        this.aDouble = builder.aDouble;
        this.aString = builder.aString;
        this.aList = builder.aList == null ? null
                : com.google.common.collect.ImmutableList.copyOf(builder.aList);
        this.aSet = builder.aSet == null ? null
                : com.google.common.collect.ImmutableSet.copyOf(builder.aSet);
        this.aMap = builder.aMap == null ? null
                : com.google.common.collect.ImmutableMap.copyOf(builder.aMap);
        this.aMessage = builder.aMessage;
        this.anObject = builder.anObject;
    }

    protected TestMessage(final java.util.Map<?, ?> map) {
        super(map);
        this.anEnum = io.pdef.test.TestEnum.parse(map.get("anEnum"));
        this.aBool = io.pdef.descriptors.Descriptors.bool.parse(map.get("aBool"));
        this.anInt16 = io.pdef.descriptors.Descriptors.int16.parse(map.get("anInt16"));
        this.anInt32 = io.pdef.descriptors.Descriptors.int32.parse(map.get("anInt32"));
        this.anInt64 = io.pdef.descriptors.Descriptors.int64.parse(map.get("anInt64"));
        this.aFloat = io.pdef.descriptors.Descriptors.float0.parse(map.get("aFloat"));
        this.aDouble = io.pdef.descriptors.Descriptors.double0.parse(map.get("aDouble"));
        this.aString = io.pdef.descriptors.Descriptors.string.parse(map.get("aString"));
        this.aList = io.pdef.descriptors.Descriptors.list(io.pdef.descriptors.Descriptors.string).parse(map.get("aList"));
        this.aSet = io.pdef.descriptors.Descriptors.set(io.pdef.descriptors.Descriptors.string).parse(map.get("aSet"));
        this.aMap = io.pdef.descriptors.Descriptors.map(io.pdef.descriptors.Descriptors.string, io.pdef.descriptors.Descriptors.string).parse(map.get("aMap"));
        this.aMessage = io.pdef.test.TestMessage.parse(map.get("aMessage"));
        this.anObject = io.pdef.descriptors.Descriptors.object.parse(map.get("anObject"));
    }
    
    public static TestMessage parse(final Object object) {
        if (object == null) return null;
        return parse((java.util.Map<?, ?>) object);
    }
    
    public static TestMessage parse(final java.util.Map<?, ?> map) {
        return new TestMessage(map);
    }

    public static TestMessage parseFromJson(final String s) {
        Object object = io.pdef.json.Json.parse(s);
        return parse(object);
    }

    public io.pdef.test.TestEnum getAnEnum() {
        return anEnum != null ? anEnum : io.pdef.test.TestEnum.instance();
    }

    public boolean hasAnEnum() {
        return anEnum != null;
    }

    public boolean getABool() {
        return aBool;
    }

    public boolean hasABool() {
        return true;
    }

    public short getAnInt16() {
        return anInt16;
    }

    public boolean hasAnInt16() {
        return true;
    }

    public int getAnInt32() {
        return anInt32;
    }

    public boolean hasAnInt32() {
        return true;
    }

    public long getAnInt64() {
        return anInt64;
    }

    public boolean hasAnInt64() {
        return true;
    }

    public float getAFloat() {
        return aFloat;
    }

    public boolean hasAFloat() {
        return true;
    }

    public double getADouble() {
        return aDouble;
    }

    public boolean hasADouble() {
        return true;
    }

    @javax.annotation.Nullable
    public String getAString() {
        return aString != null ? aString : null;
    }

    public boolean hasAString() {
        return aString != null;
    }

    public java.util.List<String> getAList() {
        return aList != null ? aList : com.google.common.collect.ImmutableList.<String>of();
    }

    public boolean hasAList() {
        return aList != null;
    }

    public java.util.Set<String> getASet() {
        return aSet != null ? aSet : com.google.common.collect.ImmutableSet.<String>of();
    }

    public boolean hasASet() {
        return aSet != null;
    }

    public java.util.Map<String, String> getAMap() {
        return aMap != null ? aMap : com.google.common.collect.ImmutableMap.<String, String>of();
    }

    public boolean hasAMap() {
        return aMap != null;
    }

    public io.pdef.test.TestMessage getAMessage() {
        return aMessage != null ? aMessage : io.pdef.test.TestMessage.instance();
    }

    public boolean hasAMessage() {
        return aMessage != null;
    }

    @javax.annotation.Nullable
    public Object getAnObject() {
        return anObject != null ? anObject : null;
    }

    public boolean hasAnObject() {
        return anObject != null;
    }

    @Override
    public java.util.Map<String, Object> serialize() {
        java.util.Map<String, Object> map = super.serialize();
        if (hasAnEnum()) map.put("anEnum", io.pdef.test.TestEnum.serialize(this.anEnum));
        if (hasABool()) map.put("aBool", io.pdef.descriptors.Descriptors.bool.serialize(this.aBool));
        if (hasAnInt16()) map.put("anInt16", io.pdef.descriptors.Descriptors.int16.serialize(this.anInt16));
        if (hasAnInt32()) map.put("anInt32", io.pdef.descriptors.Descriptors.int32.serialize(this.anInt32));
        if (hasAnInt64()) map.put("anInt64", io.pdef.descriptors.Descriptors.int64.serialize(this.anInt64));
        if (hasAFloat()) map.put("aFloat", io.pdef.descriptors.Descriptors.float0.serialize(this.aFloat));
        if (hasADouble()) map.put("aDouble", io.pdef.descriptors.Descriptors.double0.serialize(this.aDouble));
        if (hasAString()) map.put("aString", io.pdef.descriptors.Descriptors.string.serialize(this.aString));
        if (hasAList()) map.put("aList", io.pdef.descriptors.Descriptors.list(io.pdef.descriptors.Descriptors.string).serialize(this.aList));
        if (hasASet()) map.put("aSet", io.pdef.descriptors.Descriptors.set(io.pdef.descriptors.Descriptors.string).serialize(this.aSet));
        if (hasAMap()) map.put("aMap", io.pdef.descriptors.Descriptors.map(io.pdef.descriptors.Descriptors.string, io.pdef.descriptors.Descriptors.string).serialize(this.aMap));
        if (hasAMessage()) map.put("aMessage", io.pdef.test.TestMessage.serialize(this.aMessage));
        if (hasAnObject()) map.put("anObject", io.pdef.descriptors.Descriptors.object.serialize(this.anObject));
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
    public io.pdef.descriptors.Descriptor<? extends TestMessage> descriptorForType() {
        return DESCRIPTOR;
    }

    public static java.util.Map<String, Object> serialize(final TestMessage object) {
        return object == null ? null : object.serialize();
    }

    public static class Builder extends io.pdef.GeneratedMessage.Builder {
        protected io.pdef.test.TestEnum anEnum;
        protected boolean aBool;
        protected short anInt16;
        protected int anInt32;
        protected long anInt64;
        protected float aFloat;
        protected double aDouble;
        protected String aString;
        protected java.util.List<String> aList;
        protected java.util.Set<String> aSet;
        protected java.util.Map<String, String> aMap;
        protected io.pdef.test.TestMessage aMessage;
        protected Object anObject;

        protected Builder() {}

        protected Builder(final TestMessage message) {
            super(message);
            this.anEnum = message.anEnum;
            this.aBool = message.aBool;
            this.anInt16 = message.anInt16;
            this.anInt32 = message.anInt32;
            this.anInt64 = message.anInt64;
            this.aFloat = message.aFloat;
            this.aDouble = message.aDouble;
            this.aString = message.aString;
            this.aList = message.aList;
            this.aSet = message.aSet;
            this.aMap = message.aMap;
            this.aMessage = message.aMessage;
            this.anObject = message.anObject;
            
        }

        public io.pdef.test.TestEnum getAnEnum() {
            return anEnum != null ? anEnum : io.pdef.test.TestEnum.instance();
        }

        public Builder setAnEnum(final io.pdef.test.TestEnum value) {
            this.anEnum = value;
            return this;
        }

        public Builder clearAnEnum() {
            this.anEnum = null;
            return this;
        }

        public boolean hasAnEnum() {
            return anEnum != null;
        }

        public boolean getABool() {
            return aBool;
        }

        public Builder setABool(final boolean value) {
            this.aBool = value;
            return this;
        }

        public Builder clearABool() {
            this.aBool = false;
            return this;
        }

        public boolean hasABool() {
            return true;
        }

        public short getAnInt16() {
            return anInt16;
        }

        public Builder setAnInt16(final short value) {
            this.anInt16 = value;
            return this;
        }

        public Builder clearAnInt16() {
            this.anInt16 = (short) 0;
            return this;
        }

        public boolean hasAnInt16() {
            return true;
        }

        public int getAnInt32() {
            return anInt32;
        }

        public Builder setAnInt32(final int value) {
            this.anInt32 = value;
            return this;
        }

        public Builder clearAnInt32() {
            this.anInt32 = 0;
            return this;
        }

        public boolean hasAnInt32() {
            return true;
        }

        public long getAnInt64() {
            return anInt64;
        }

        public Builder setAnInt64(final long value) {
            this.anInt64 = value;
            return this;
        }

        public Builder clearAnInt64() {
            this.anInt64 = 0L;
            return this;
        }

        public boolean hasAnInt64() {
            return true;
        }

        public float getAFloat() {
            return aFloat;
        }

        public Builder setAFloat(final float value) {
            this.aFloat = value;
            return this;
        }

        public Builder clearAFloat() {
            this.aFloat = 0f;
            return this;
        }

        public boolean hasAFloat() {
            return true;
        }

        public double getADouble() {
            return aDouble;
        }

        public Builder setADouble(final double value) {
            this.aDouble = value;
            return this;
        }

        public Builder clearADouble() {
            this.aDouble = 0.0;
            return this;
        }

        public boolean hasADouble() {
            return true;
        }

        public String getAString() {
            return aString != null ? aString : null;
        }

        public Builder setAString(final String value) {
            this.aString = value;
            return this;
        }

        public Builder clearAString() {
            this.aString = null;
            return this;
        }

        public boolean hasAString() {
            return aString != null;
        }

        public java.util.List<String> getAList() {
            return aList != null ? aList : com.google.common.collect.ImmutableList.<String>of();
        }

        public Builder setAList(final java.util.List<String> value) {
            this.aList = value;
            return this;
        }

        public Builder clearAList() {
            this.aList = null;
            return this;
        }

        public boolean hasAList() {
            return aList != null;
        }

        public java.util.Set<String> getASet() {
            return aSet != null ? aSet : com.google.common.collect.ImmutableSet.<String>of();
        }

        public Builder setASet(final java.util.Set<String> value) {
            this.aSet = value;
            return this;
        }

        public Builder clearASet() {
            this.aSet = null;
            return this;
        }

        public boolean hasASet() {
            return aSet != null;
        }

        public java.util.Map<String, String> getAMap() {
            return aMap != null ? aMap : com.google.common.collect.ImmutableMap.<String, String>of();
        }

        public Builder setAMap(final java.util.Map<String, String> value) {
            this.aMap = value;
            return this;
        }

        public Builder clearAMap() {
            this.aMap = null;
            return this;
        }

        public boolean hasAMap() {
            return aMap != null;
        }

        public io.pdef.test.TestMessage getAMessage() {
            return aMessage != null ? aMessage : io.pdef.test.TestMessage.instance();
        }

        public Builder setAMessage(final io.pdef.test.TestMessage value) {
            this.aMessage = value;
            return this;
        }

        public Builder clearAMessage() {
            this.aMessage = null;
            return this;
        }

        public boolean hasAMessage() {
            return aMessage != null;
        }

        public Object getAnObject() {
            return anObject != null ? anObject : null;
        }

        public Builder setAnObject(final Object value) {
            this.anObject = value;
            return this;
        }

        public Builder clearAnObject() {
            this.anObject = null;
            return this;
        }

        public boolean hasAnObject() {
            return anObject != null;
        }

        @Override
        public TestMessage build() {
            return new TestMessage(this);
        }

        @Override
        public String toString() {
            return com.google.common.base.Objects.toStringHelper(this)
                    .add("anEnum", anEnum)
                    .add("aBool", aBool)
                    .add("anInt16", anInt16)
                    .add("anInt32", anInt32)
                    .add("anInt64", anInt64)
                    .add("aFloat", aFloat)
                    .add("aDouble", aDouble)
                    .add("aString", aString)
                    .add("aList", aList)
                    .add("aSet", aSet)
                    .add("aMap", aMap)
                    .add("aMessage", aMessage)
                    .add("anObject", anObject)
                    .toString();
        }

        @Override
        public boolean equals(final Object o) {
                        if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            TestMessage cast = (TestMessage) o;

            if (this.anEnum != null
                    ? !this.anEnum.equals(cast.anEnum)
                    : cast.anEnum != null) return false;
            if (this.aBool != cast.aBool) return false;
            if (this.anInt16 != cast.anInt16) return false;
            if (this.anInt32 != cast.anInt32) return false;
            if (this.anInt64 != cast.anInt64) return false;
            if (this.aFloat != cast.aFloat) return false;
            if (this.aDouble != cast.aDouble) return false;
            if (this.aString != null
                    ? !this.aString.equals(cast.aString)
                    : cast.aString != null) return false;
            if (this.aList != null
                    ? !this.aList.equals(cast.aList)
                    : cast.aList != null) return false;
            if (this.aSet != null
                    ? !this.aSet.equals(cast.aSet)
                    : cast.aSet != null) return false;
            if (this.aMap != null
                    ? !this.aMap.equals(cast.aMap)
                    : cast.aMap != null) return false;
            if (this.aMessage != null
                    ? !this.aMessage.equals(cast.aMessage)
                    : cast.aMessage != null) return false;
            if (this.anObject != null
                    ? !this.anObject.equals(cast.anObject)
                    : cast.anObject != null) return false;
            
            return true;
            
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            return com.google.common.base.Objects.hashCode(result,
                    this.anEnum, 
                    this.aBool, 
                    this.anInt16, 
                    this.anInt32, 
                    this.anInt64, 
                    this.aFloat, 
                    this.aDouble, 
                    this.aString, 
                    this.aList, 
                    this.aSet, 
                    this.aMap, 
                    this.aMessage, 
                    this.anObject); 
                    
        }
    }

    @Override
    public String toString() {
        return com.google.common.base.Objects.toStringHelper(this)
                .add("anEnum", anEnum)
                .add("aBool", aBool)
                .add("anInt16", anInt16)
                .add("anInt32", anInt32)
                .add("anInt64", anInt64)
                .add("aFloat", aFloat)
                .add("aDouble", aDouble)
                .add("aString", aString)
                .add("aList", aList)
                .add("aSet", aSet)
                .add("aMap", aMap)
                .add("aMessage", aMessage)
                .add("anObject", anObject)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        TestMessage cast = (TestMessage) o;

        if (this.anEnum != null
                ? !this.anEnum.equals(cast.anEnum)
                : cast.anEnum != null) return false;
        if (this.aBool != cast.aBool) return false;
        if (this.anInt16 != cast.anInt16) return false;
        if (this.anInt32 != cast.anInt32) return false;
        if (this.anInt64 != cast.anInt64) return false;
        if (this.aFloat != cast.aFloat) return false;
        if (this.aDouble != cast.aDouble) return false;
        if (this.aString != null
                ? !this.aString.equals(cast.aString)
                : cast.aString != null) return false;
        if (this.aList != null
                ? !this.aList.equals(cast.aList)
                : cast.aList != null) return false;
        if (this.aSet != null
                ? !this.aSet.equals(cast.aSet)
                : cast.aSet != null) return false;
        if (this.aMap != null
                ? !this.aMap.equals(cast.aMap)
                : cast.aMap != null) return false;
        if (this.aMessage != null
                ? !this.aMessage.equals(cast.aMessage)
                : cast.aMessage != null) return false;
        if (this.anObject != null
                ? !this.anObject.equals(cast.anObject)
                : cast.anObject != null) return false;

        return true;
    }

    @Override
    protected int generateHashCode() {
        int result = super.generateHashCode();
        return com.google.common.base.Objects.hashCode(result,
                this.anEnum, 
                this.aBool, 
                this.anInt16, 
                this.anInt32, 
                this.anInt64, 
                this.aFloat, 
                this.aDouble, 
                this.aString, 
                this.aList, 
                this.aSet, 
                this.aMap, 
                this.aMessage, 
                this.anObject); 
                
    }

    public static Builder builder() {
        return new Builder();
    }

    public static TestMessage instance() {
        return INSTANCE;
    }
    
    private static final TestMessage INSTANCE = new TestMessage();
    public static final io.pdef.descriptors.Descriptor<TestMessage> DESCRIPTOR =
            new io.pdef.descriptors.Descriptor<TestMessage>() {
                @Override
                public Class<TestMessage> getJavaClass() {
                    return TestMessage.class;
                }

                @Override
                public TestMessage getDefault() {
                    return TestMessage.instance();
                }

                @Override
                public TestMessage parse(final Object object) {
                    return TestMessage.parse(object);
                }

                @Override
                public Object serialize(final TestMessage value) {
                    return TestMessage.serialize(value);
                }
            };
}
