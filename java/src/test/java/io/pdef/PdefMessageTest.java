package io.pdef;

import com.google.common.collect.ImmutableSet;
import io.pdef.test.*;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class PdefMessageTest {
	private Pdef pdef;

	@Before
	public void setUp() throws Exception {
		pdef = new Pdef();
	}

	@Test
	public void testFields_declared() throws Exception {
		PdefMessage msg = new PdefMessage(TestSimpleMessage.class, pdef);
		assertEquals(ImmutableSet.of("firstField", "secondField", "thirdField"),
				msg.getDeclaredFieldMap().keySet());
	}

	@Test
	public void testField_declaredPlusInherited() throws Exception {
		PdefMessage tree2 = new PdefMessage(Tree2.class, pdef);
		assertEquals(ImmutableSet.of("type", "type1"), tree2.getFieldMap().keySet());
	}

	/** Should return null when a message does not have a base. */
	@Test
	public void testBase_null() throws Exception {
		PdefMessage msg = new PdefMessage(Tree0.class, pdef);
		assertNull(msg.getBase());
	}

	/** Should return a message base. */
	@Test
	public void testBase() throws Exception {
		PdefMessage tree1 = new PdefMessage(Tree1.class, pdef);
		PdefMessage tree0 = (PdefMessage) pdef.get(Tree0.class);

		assertTrue(tree1.getBase() == tree0);
	}

	/** Should initialize message subtype trees. */
	@Test
	public void testBase_tree() throws Exception {
		PdefMessage t0 = (PdefMessage) pdef.get(Tree0.class);
		PdefMessage t1 = (PdefMessage) pdef.get(Tree1.class);
		PdefMessage t2 = (PdefMessage) pdef.get(Tree2.class);
		PdefMessage ta = (PdefMessage) pdef.get(TreeA.class);
		PdefMessage tb = (PdefMessage) pdef.get(TreeB.class);

		assertTrue(t1.getBase() == t0);
		assertTrue(t2.getBase() == t1);
		assertTrue(ta.getBase() == t2);
		assertTrue(tb.getBase() == t2);
	}

	/** Should initialize message subtypes. */
	@Test
	public void testSubtypes() throws Exception {
		PdefMessage t0 = (PdefMessage) pdef.get(Tree0.class);
		PdefMessage t1 = (PdefMessage) pdef.get(Tree1.class);
		PdefMessage t2 = (PdefMessage) pdef.get(Tree2.class);
		PdefMessage ta = (PdefMessage) pdef.get(TreeA.class);
		PdefMessage tb = (PdefMessage) pdef.get(TreeB.class);

		assertEquals(ImmutableSet.of(t1, t2), ImmutableSet.copyOf(t0.getSubtypes().values()));
		assertEquals(ImmutableSet.of(t2), ImmutableSet.copyOf(t1.getSubtypes().values()));
		assertEquals(ImmutableSet.of(ta, tb), ImmutableSet.copyOf(t2.getSubtypes().values()));
	}

	/** Should return a message discriminator. */
	@Test
	public void testDiscriminator() throws Exception {
		PdefMessage t0 = (PdefMessage) pdef.get(Tree0.class);
		PdefMessage t1 = (PdefMessage) pdef.get(Tree1.class);
		PdefMessage t2 = (PdefMessage) pdef.get(Tree2.class);
		PdefMessage ta = (PdefMessage) pdef.get(TreeA.class);
		PdefMessage tb = (PdefMessage) pdef.get(TreeB.class);

		assertEquals(t0.getField("type"), t0.getDiscriminator());
		assertEquals(t1.getField("type"), t1.getDiscriminator());
		assertEquals(t2.getField("type1"), t2.getDiscriminator());
		assertNull(ta.getDiscriminator());
		assertNull(tb.getDiscriminator());
	}

	@Test
	public void testCreateBuilder() throws Exception {
		PdefMessage msg = new PdefMessage(TestSimpleMessage.class, pdef);
		TestSimpleMessage.Builder builder = (TestSimpleMessage.Builder) msg.createBuilder();
		assertEquals(TestSimpleMessage.getInstance(), builder.build());
	}

	@Test
	public void testDefaultValue() throws Exception {
		PdefMessage msg = new PdefMessage(TestSimpleMessage.class, pdef);
		assertTrue(TestSimpleMessage.getInstance() == msg.getDefaultValue());
	}

	/** Should get a field by its name. */
	@Test
	public void testGetField() throws Exception {
		PdefMessage msg = new PdefMessage(TestSimpleMessage.class, pdef);
		PdefField field = msg.getField("firstField");
		assertNotNull(field);
	}
}
