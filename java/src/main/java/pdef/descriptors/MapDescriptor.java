package pdef.descriptors;

import static com.google.common.base.Preconditions.*;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

public class MapDescriptor extends AbstractDescriptor {
	private final ParameterizedType mapType;
	private final Type key;
	private final Type element;

	public MapDescriptor(final ParameterizedType mapType, final DescriptorPool pool) {
		super(DescriptorType.MAP, pool);
		this.mapType = checkNotNull(mapType);
		checkArgument(mapType.getRawType() == Map.class);

		Type[] args = mapType.getActualTypeArguments();
		key = args[0];
		element = args[1];
	}

	public ParameterizedType getMapType() {
		return mapType;
	}

	public Type getKey() {
		return key;
	}

	public Type getElement() {
		return element;
	}

	@Override
	protected void doLink() {}
}
