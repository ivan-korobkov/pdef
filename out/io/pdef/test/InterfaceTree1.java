package io.pdef.test;


public interface InterfaceTree1 extends io.pdef.test.InterfaceTree0 {

    String method1();

    static io.pdef.descriptors.InterfaceDescriptor<InterfaceTree1> DESCRIPTOR =
            new io.pdef.descriptors.GeneratedInterfaceDescriptor<InterfaceTree1>(InterfaceTree1.class) {
        private final java.util.Map<String, io.pdef.descriptors.MethodDescriptor> methods;
        {
            methods = methods(
                    method(this, "method0")
                            .result(io.pdef.descriptors.Descriptors.string)
                            .build(), 
                    method(this, "method1")
                            .result(io.pdef.descriptors.Descriptors.string)
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
