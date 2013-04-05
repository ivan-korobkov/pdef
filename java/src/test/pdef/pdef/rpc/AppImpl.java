package pdef.rpc;

import pdef.fixtures.User;

class AppImpl implements App {
	@Override
	public String echo(final String text) {
		return text;
	}

	@Override
	public User register(final String nick, final String email, final String password) {
		return User.builder().setName(nick).build();
	}

	@Override
	public User login(final String email, final String password) {
		return User.builder().setName(email).build();
	}

	@Override
	public void ping() {
		System.out.println("Ping");
	}

	@Override
	public String get(final Integer integer) {
		return "Hello " + integer;
	}
}
