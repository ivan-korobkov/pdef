package io.pdef.test;


/** Interface inheritance tree. */
public interface InterfaceTree0 {

    String method0();

    static io.pdef.descriptors.InterfaceDescriptor<InterfaceTree0> DESCRIPTOR =
            new io.pdef.descriptors.GeneratedInterfaceDescriptor<InterfaceTree0>(InterfaceTree0.class) {
        private final java.util.Map<String, io.pdef.descriptors.MethodDescriptor> methods;
        {
            methods = methods(
                    method(this, "method0")
                            .result(io.pdef.descriptors.Descriptors.string)
                            .build());
        }

		@Override
		public java.util.Map<String, io.pdef.descriptors.MethodDescriptor> getMethods() {
			return methods;
		}
    };
}
