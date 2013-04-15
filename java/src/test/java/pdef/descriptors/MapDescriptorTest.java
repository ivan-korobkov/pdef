package pdef.descriptors;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MapDescriptorTest {
	private Map<String, Integer> map;

	@Test
	public void testConstruct() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		MapDescriptor descriptor = new MapDescriptor(getType(), pool);
		assertEquals(getType(), descriptor.getJavaType());
		assertEquals(String.class, descriptor.getKeyType());
		assertEquals(Integer.class, descriptor.getValueType());
	}

	@Test
	public void testLink() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		MapDescriptor descriptor = new MapDescriptor(getType(), pool);
		descriptor.link();
	}

	private ParameterizedType getType() {
		try {
			return (ParameterizedType) MapDescriptorTest.class.getDeclaredField("map")
					.getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
