package pdef.rpc;

import pdef.fixtures.interfaces.App;
import pdef.fixtures.interfaces.Calc;
import pdef.fixtures.interfaces.Users;

class TestApp implements App {

	@Override
	public Users users() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Calc calc() {
		return new TestCalc();
	}
}
