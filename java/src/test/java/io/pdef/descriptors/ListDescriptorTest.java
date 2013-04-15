package io.pdef.descriptors;

import org.junit.Test;

import java.lang.reflect.ParameterizedType;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ListDescriptorTest {
	private List<String> list;

	@Test
	public void testConstruct() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		ListDescriptor d = new ListDescriptor(getType(), pool);

		assertEquals(getType(), d.getJavaType());
		assertEquals(String.class, d.getElementType());
	}

	@Test
	public void testLink() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		ListDescriptor d = new ListDescriptor(getType(), pool);
		d.link();
	}

	private ParameterizedType getType () {
		try {
			return (ParameterizedType) ListDescriptorTest.class.getDeclaredField("list")
					.getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
