package io.pdef.descriptors;

import com.google.common.collect.ImmutableMap;
import io.pdef.fixtures.GenericObject;
import io.pdef.fixtures.Image;
import io.pdef.fixtures.ObjectType;
import io.pdef.fixtures.User;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SubtypesDescriptorTest {

	@Test
	public void testConstructor() throws Exception {
		DescriptorPool pool = new DefaultDescriptorPool();
		MessageDescriptor descriptor = new MessageDescriptor(GenericObject.class, pool);
		descriptor.link();

		SubtypesDescriptor subtypes = descriptor.getSubtypes();
		assertEquals(ImmutableMap.<Enum<?>, Class<?>>of(ObjectType.OBJECT, GenericObject.class,
				ObjectType.IMAGE, Image.class,
				ObjectType.USER, User.class), subtypes.getMap());

		assertEquals(descriptor.getFields().get("type"), subtypes.getField());
	}
}
