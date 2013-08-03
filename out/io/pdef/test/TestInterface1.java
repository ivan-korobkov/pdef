package io.pdef.test;


/** An interface returned as a sub interface in TestInterface. */
public interface TestInterface1 {

    String hello(String firstName, String lastName);

    static io.pdef.descriptors.InterfaceDescriptor<TestInterface1> DESCRIPTOR =
            new io.pdef.descriptors.GeneratedInterfaceDescriptor<TestInterface1>(TestInterface1.class) {
        private final java.util.Map<String, io.pdef.descriptors.MethodDescriptor> methods;
        {
            methods = methods(
                    method(this, "hello")
                            .arg("firstName", io.pdef.descriptors.Descriptors.string)
                            .arg("lastName", io.pdef.descriptors.Descriptors.string)
                            .result(io.pdef.descriptors.Descriptors.string)
                            .build());
        }

		@Override
		public java.util.Map<String, io.pdef.descriptors.MethodDescriptor> getMethods() {
			return methods;
		}
    };
}
