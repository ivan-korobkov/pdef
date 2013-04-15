package io.pdef.descriptors;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Test;

import java.lang.reflect.ParameterizedType;

import static org.junit.Assert.assertEquals;

public class FutureDescriptorTest {
	private ListenableFuture<String> future;

	@Test
	public void test() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		FutureDescriptor descriptor = new FutureDescriptor(getType(), pool);
		descriptor.link();

		Descriptor str = pool.getDescriptor(String.class);
		assertEquals(getType(), descriptor.getJavaType());
		assertEquals(String.class, descriptor.getElementType());
		assertEquals(str, descriptor.getElement());
	}

	private ParameterizedType getType() {
		try {
			return (ParameterizedType) FutureDescriptorTest.class.getDeclaredField("future")
					.getGenericType();
		} catch (NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
