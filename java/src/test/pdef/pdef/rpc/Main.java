package pdef.rpc;

import com.google.common.collect.ImmutableMap;
import pdef.InterfaceDescriptor;
import pdef.MethodDescriptor;

public class Main {

	public static void main(String[] args) {
		InterfaceDescriptor descriptor = App.Descriptor.getInstance();
		System.out.println(descriptor);
		System.out.println(descriptor.getMethods());
		System.out.println(descriptor.getDeclaredMethods());

		App app = new AppImpl();
		MethodDescriptor register = descriptor.getMethods().map().get("register");
		MethodDescriptor get = descriptor.getMethods().map().get("get");
		MethodDescriptor ping = descriptor.getMethods().map().get("ping");

		Object result = register.call(app, ImmutableMap.<String, Object>of(
				"nick", "ivan.korobkov",
				"email", "ivan.korobkov@gmail.com",
				"password", "qwerty"
		));
		System.out.println(result);

		result = get.call(app, ImmutableMap.<String, Object>of("id", 10));
		System.out.println(result);

		result = ping.call(app, ImmutableMap.<String, Object>of());
		System.out.println(result);
	}
}
