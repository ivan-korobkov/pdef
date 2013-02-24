package pdef.descriptors;

import org.junit.Test;
import pdef.fixtures.User;

public class MessageDescriptorTest {

	@Test
	public void testFields() throws Exception {
		MessageDescriptor descriptor = new User().getDescriptor();
		System.out.println(descriptor.getFields().list());
	}
}
