package io.pdef.descriptors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.pdef.Interface;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class InterfaceDescriptor extends AbstractDescriptor {
	private List<InterfaceDescriptor> bases;
	private Map<String, MethodDescriptor> declaredMethods;
	private Map<String, MethodDescriptor> methods;

	public InterfaceDescriptor(final Class<?> cls, final DescriptorPool pool) {
		super(cls, DescriptorType.INTERFACE, pool);
		checkArgument(Interface.class.isAssignableFrom(cls));
	}

	@Override
	public Class<?> getJavaType() {
		return (Class<?>) super.getJavaType();
	}

	public List<InterfaceDescriptor> getBases() {
		return bases;
	}

	public Map<String, MethodDescriptor> getDeclaredMethods() {
		return declaredMethods;
	}

	public Map<String, MethodDescriptor> getMethods() {
		return methods;
	}

	@Override
	protected void doLink() {
		linkBases();
		linkDeclaredMethods();
		linkMethods();
	}

	private void linkBases() {
		ImmutableList.Builder<InterfaceDescriptor> builder = ImmutableList.builder();
		for (Class<?> base : getJavaType().getInterfaces()) {
			if (!Interface.class.isAssignableFrom(base)) continue;

			InterfaceDescriptor descriptor = (InterfaceDescriptor) pool.getDescriptor(base);
			builder.add(descriptor);
		}
		bases = builder.build();
	}

	private void linkDeclaredMethods() {
		ImmutableMap.Builder<String, MethodDescriptor> builder = ImmutableMap.builder();
		for (Method method : getJavaType().getDeclaredMethods()) {
			MethodDescriptor descriptor = new MethodDescriptor(method, pool);
			builder.put(descriptor.getName(), descriptor);
		}
		declaredMethods = builder.build();
	}

	private void linkMethods() {
		ImmutableMap.Builder<String, MethodDescriptor> builder = ImmutableMap.builder();
		for (InterfaceDescriptor base : bases) {
			builder.putAll(base.getMethods());
		}
		builder.putAll(declaredMethods);
		methods = builder.build();
	}
}
