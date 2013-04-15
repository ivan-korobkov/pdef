package pdef.descriptors;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class SetDescriptorTest {
	private Set<String> set;

	@Test
	public void testConstruct() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		SetDescriptor descriptor = new SetDescriptor(getType(), pool);
		assertEquals(getType(), descriptor.getSetType());
		assertEquals(String.class, descriptor.getElementType());
	}

	@Test
	public void testLink() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		SetDescriptor descriptor = new SetDescriptor(getType(), pool);
		descriptor.link();
	}

	private ParameterizedType getType() {
		try {
			return (ParameterizedType) SetDescriptorTest.class.getDeclaredField("set")
					.getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
