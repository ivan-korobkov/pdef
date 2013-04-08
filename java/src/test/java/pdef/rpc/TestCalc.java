package pdef.rpc;

import pdef.fixtures.interfaces.Calc;

class TestCalc implements Calc {
	@Override
	public int sum(final int i0, final int i1) {
		return i0 + i1;
	}
}
