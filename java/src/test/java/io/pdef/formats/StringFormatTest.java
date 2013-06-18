package io.pdef.formats;

import io.pdef.Pdef;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StringFormatTest {
	private StringFormat format;
	private Pdef pdef;

	@Before
	public void setUp() throws Exception {
		format = new StringFormat();
		pdef = new Pdef();
	}

	@Test
	public void testRead() throws Exception {
		assertEquals(true, format.read(boolean.class, "true"));
		assertEquals(-10, (short) format.read(short.class, "-10"));
		assertEquals(-11, (int) format.read(int.class, "-11"));
		assertEquals(-12, (long) format.read(long.class, "-12"));
		assertEquals(-1.5f, format.read(float.class, "-1.5"), 0.00001);
		assertEquals(-2.5, format.read(double.class, "-2.5"), 0.00001);
		assertEquals("hello", format.read(String.class, "hello"));
		//assertEquals(TestEnum.THREE, format.read(TestEnum.class, "three"));
	}

	@Test
	public void testRead_null() throws Exception {
		assertEquals(false, format.read(boolean.class, null));
		assertEquals(0, (short) format.read(short.class, null));
		assertEquals(0, (int) format.read(int.class, null));
		assertEquals(0, (long) format.read(long.class, null));
		assertEquals(0.0f, format.read(float.class, null), 0.00001);
		assertEquals(0.0d, format.read(double.class, null), 0.00001);
		assertEquals("", format.read(String.class, null));
		//assertEquals(TestEnum.ONE, format.read(TestEnum.class, null));
	}

	@Test
	public void testWrite() throws Exception {
		assertEquals("true", format.write(true));
		assertEquals("-10", format.write((short) -10));
		assertEquals("-11", format.write(-11));
		assertEquals("-12", format.write(-12L));
		assertEquals("-1.5", format.write(-1.5f));
		assertEquals("-2.5", format.write(-2.5d));
		assertEquals("hello", format.write("hello"));
		//assertEquals("one", format.write(TestEnum.ONE));
	}

	@Test
	public void testWrite_null() throws Exception {
		assertEquals("false", format.write(pdef.get(boolean.class), null));
		assertEquals("0", format.write(pdef.get(short.class), null));
		assertEquals("0", format.write(pdef.get(int.class), null));
		assertEquals("0", format.write(pdef.get(long.class), null));
		assertEquals("0.0", format.write(pdef.get(float.class), null));
		assertEquals("0.0", format.write(pdef.get(double.class), null));
		assertEquals("", format.write(pdef.get(String.class), null));
		//assertEquals("one", format.write(pdef.get(TestEnum.class), null));
	}
}
