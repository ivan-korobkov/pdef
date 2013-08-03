package io.pdef.test;


/** Test interface with method of all types. */
public interface TestInterface {

    /** Void method w/o args. */
    void void0();

    /** Camel-case method w/o args with a string result. */
    String camelCase(String firstArg, String secondArg);

    /** Method which accepts and returns a message. */
    io.pdef.test.TestMessage message0(io.pdef.test.TestMessage msg);

    /** Method which accepts and returns primitives. */
    int sum(int i0, int i1);

    /** Interface method. */
    io.pdef.test.TestInterface1 interface0();

    static io.pdef.descriptors.InterfaceDescriptor<TestInterface> DESCRIPTOR =
            new io.pdef.descriptors.GeneratedInterfaceDescriptor<TestInterface>(TestInterface.class) {
        private final java.util.Map<String, io.pdef.descriptors.MethodDescriptor> methods;
        {
            methods = methods(
                    method(this, "void0")
                            .result(io.pdef.descriptors.Descriptors.void0)
                            .build(), 
                    method(this, "camelCase")
                            .arg("firstArg", io.pdef.descriptors.Descriptors.string)
                            .arg("secondArg", io.pdef.descriptors.Descriptors.string)
                            .result(io.pdef.descriptors.Descriptors.string)
                            .build(), 
                    method(this, "message0")
                            .arg("msg", io.pdef.test.TestMessage.DESCRIPTOR)
                            .result(io.pdef.test.TestMessage.DESCRIPTOR)
                            .build(), 
                    method(this, "sum")
                            .arg("i0", io.pdef.descriptors.Descriptors.int32)
                            .arg("i1", io.pdef.descriptors.Descriptors.int32)
                            .result(io.pdef.descriptors.Descriptors.int32)
                            .build(), 
                    method(this, "interface0")
                            .next(new com.google.common.base.Supplier<io.pdef.descriptors.InterfaceDescriptor<?>>() {
                                @Override
                                public io.pdef.descriptors.InterfaceDescriptor<?> get() {
                                    return io.pdef.test.TestInterface1.DESCRIPTOR;
                                }
                            })
                            .build());
        }
        @Nullable
        @Override
        public Descriptor<?> getExc() {
            return io.pdef.test.Exception;
        }

		@Override
		public java.util.Map<String, io.pdef.descriptors.MethodDescriptor> getMethods() {
			return methods;
		}
    };
}
