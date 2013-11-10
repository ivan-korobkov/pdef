package io.pdef.descriptors;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class DescriptorsTest {
	@Test
	public void testList() throws Exception {
		ListDescriptor<Integer> descriptor = Descriptors.list(Descriptors.int32);

		assertEquals(List.class, descriptor.getJavaClass());
		assertEquals(Descriptors.int32, descriptor.getElement());
	}

	@Test
	public void testSet() throws Exception {
		SetDescriptor<Integer> descriptor = Descriptors.set(Descriptors.int32);

		assertEquals(Set.class, descriptor.getJavaClass());
		assertEquals(Descriptors.int32, descriptor.getElement());
	}

	@Test
	public void testMap() throws Exception {
		MapDescriptor<Integer, String> descriptor = Descriptors
				.map(Descriptors.int32, Descriptors.string);

		assertEquals(Map.class, descriptor.getJavaClass());
		assertEquals(Descriptors.int32, descriptor.getKey());
		assertEquals(Descriptors.string, descriptor.getValue());
	}
}
