package io.pdef;

import io.pdef.test.TestSimpleMessage;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class PdefFieldTest {
	private Pdef pdef;

	@Before
	public void setUp() throws Exception {
		pdef = new Pdef();
	}

	@Test
	public void testConstructor() throws Exception {
		PdefField field = getField("firstField");

		assertEquals("firstField", field.getName());
		assertEquals(pdef.get(boolean.class), field.getDescriptor());
	}

	@Test
	public void testGet() throws Exception {
		PdefField field = getField("firstField");
		TestSimpleMessage msg = TestSimpleMessage.builder()
				.setFirstField(true)
				.build();

		Object result = field.get(msg);
		assertEquals(Boolean.TRUE, result);
	}

	@Test
	public void testIsSet() throws Exception {
		PdefField field = getField("thirdField");
		TestSimpleMessage msg = TestSimpleMessage.getInstance();
		assertFalse(field.isSet(msg));

		msg = TestSimpleMessage.builder()
				.setThirdField(TestSimpleMessage.getInstance())
				.build();
		assertTrue(field.isSet(msg));
	}

	@Test
	public void testSet() throws Exception {
		PdefField field = getField("secondField");
		TestSimpleMessage.Builder builder = TestSimpleMessage.builder();
		field.set(builder, "hello, world");

		TestSimpleMessage actual = builder.build();
		TestSimpleMessage expected = TestSimpleMessage.builder()
				.setSecondField("hello, world")
				.build();
		assertEquals(expected, actual);
	}

	private PdefField getField(final String name) {
		PdefMessage descriptor = new PdefMessage(TestSimpleMessage.class, pdef);
		return descriptor.getField(name);
	}
}
