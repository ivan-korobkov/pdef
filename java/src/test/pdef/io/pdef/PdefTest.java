package io.pdef;

import io.pdef.test.UserEvent;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class PdefTest {

	@Test
	public void testMessage() throws Exception {
		Pdef pdef = new Pdef();
		Pdef.MessageInfo info = (Pdef.MessageInfo) pdef.get(UserEvent.class);
		assertNotNull(info);
	}
}
