package pdef.fixtures;

public class Main {

	public static void main(String[] args) {
		int n = 10 * 1000 * 1000;

		bench(n);
		bench(n);
	}

	private static void bench(final int n) {
		long t0 = System.currentTimeMillis();
		int r = 0;
		for (int i = 0; i < n; i++) {
			User user = new User.Builder()
					.setAvatar(new Photo.Builder()
							.setOwner(new Base.Builder().build())
							.build())
					.setObject(new Base.Builder().build())
					.build();
			if (user.getDiscriminator() == null) {
				r++;
			}
		}
		System.out.println("r: " + r);
		long t1 = System.currentTimeMillis();
		System.out.println(t1 - t0);
	}
}
