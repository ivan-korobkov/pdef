package com.ivankorobkov.pdef.data;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.ivankorobkov.pdef.DescriptorPool;
import com.ivankorobkov.pdef.Pdef;
import com.ivankorobkov.pdef.PackageDescriptor;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class BuiltinPackage implements PackageDescriptor {

	static {
		Pdef.getPool().add(new BuiltinPackage());
	}

	public static BuiltinPackage getInstance() {
		return Pdef.getPool().getPackage(BuiltinPackage.class);
	}

	private final ValueDescriptor<Boolean> bool;
	private final ValueDescriptor<Short> int16;
	private final ValueDescriptor<Integer> int32;
	private final ValueDescriptor<Long> int64;
	private final ValueDescriptor<Float> float0;
	private final ValueDescriptor<Double> double0;
	private final ValueDescriptor<String> string;
	private final ValueDescriptor<Void> void0;

	private final ListDescriptor<?> list;
	private final MapDescriptor<?, ?> map;
	private final SetDescriptor<?> set;

	private final ImmutableMap<Class<?>, DataTypeDescriptor> definitions;

	private BuiltinPackage() {
		bool = ValueDescriptor.create("bool", Boolean.class);
		int16 = ValueDescriptor.create("int16", Short.class);
		int32 = ValueDescriptor.create("int32", Integer.class);
		int64 = ValueDescriptor.create("int64", Long.class);
		float0 = ValueDescriptor.create("float", Float.class);
		double0 = ValueDescriptor.create("double", Double.class);
		string = ValueDescriptor.create("string", String.class);
		void0 = ValueDescriptor.create("void", Void.class);

		list = new ListDescriptor<Object>();
		map = new MapDescriptor<Object, Object>();
		set = new SetDescriptor<Object>();

		ImmutableMap.Builder<Class<?>, DataTypeDescriptor> builder = ImmutableMap.builder();
		builder.put(Boolean.class, bool);
		builder.put(boolean.class, bool);

		builder.put(Short.class, int16);
		builder.put(short.class, int16);

		builder.put(Integer.class, int32);
		builder.put(int.class, int32);

		builder.put(Long.class, int64);
		builder.put(long.class, int64);

		builder.put(Float.class, float0);
		builder.put(float.class, float0);

		builder.put(Double.class, double0);
		builder.put(double.class, double0);

		builder.put(Void.class, void0);
		builder.put(void.class, void0);

		builder.put(String.class, string);
		builder.put(List.class, list);
		builder.put(Map.class, map);
		builder.put(Set.class, set);
		definitions = builder.build();
	}

	@Override
	public String getName() {
		return "builtin";
	}

	@Override
	public Set<Class<? extends PackageDescriptor>> getDependencies() {
		return ImmutableSet.of();
	}

	@Override
	public Map<Class<?>, DataTypeDescriptor> getDefinitions() {
		return definitions;
	}

	public ListDescriptor<?> getList() {
		return list;
	}

	public MapDescriptor<?, ?> getMap() {
		return map;
	}

	public SetDescriptor<?> getSet() {
		return set;
	}

	@Override
	public void link(final DescriptorPool pool) {
		// Do nothing.
	}
}
