package io.pdef.http;

import io.pdef.Name;
import io.pdef.Pdef;
import io.pdef.test.User;
import io.pdef.test.interfaces.App;
import io.pdef.test.interfaces.Calc;
import io.pdef.test.interfaces.Date;
import io.pdef.test.interfaces.Users;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Main {

	public static void main(String[] args) throws Exception {
		App app = new App() {
			@Override
			public Users users() {
				return new Users() {
					@Override
					public User get(@Name("id") final int id) {
						return null;
					}

					@Override
					public User register(@Name("name") final String name,
							@Name("email") final String email,
							@Name("birthday") final Date birthday) {
						return null;
					}
				};
			}

			@Override
			public Calc calc() {
				return new Calc() {
					@Override
					public int sum(@Name("i0") final int i0, @Name("i1") final int i1) {
						return i0 + i1;
					}
				};
			}
		};

		Pdef pdef = new Pdef();
		Server server = new Server(8080);
		ServletHandler handler = new ServletHandler();
		server.setHandler(handler);
		ServletHolder holder = new ServletHolder(new HttpServletHandler<App>(app, App.class, pdef));
		handler.addServletWithMapping(holder, "/*");
		server.start();
		server.join();
	}
}
