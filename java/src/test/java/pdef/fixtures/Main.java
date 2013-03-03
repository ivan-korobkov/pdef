package pdef.fixtures;

public class Main {

	public static void main(String[] args) {
		int n = 100 * 1000 * 1000;
		Node node = Node.getDefaultInstance();
		RootNode rootNode = RootNode.getDefaultInstance();
		System.out.println(node);
		System.out.println(rootNode);
		System.out.println(node.getRoot());
		System.out.println(node.getRoot() == rootNode);
		System.out.println();

		User.getDefaultInstance();

		int r = 0;
		long t0 = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			User user = getUser();
			r += user.getId().getValue();
		}
		long t1 = System.currentTimeMillis();

		long t2 = System.currentTimeMillis();
		for (int i = 0; i < n; i++) {
			User user = getUser();
			r += user.getId().getValue();
		}
		long t3 = System.currentTimeMillis();
		System.out.println(t1 - t0);
		System.out.println(t3 - t2);
		System.out.println(r);
	}

	private static User getUser() {
		return User.getDefaultInstance();
	}

	private static User getUser2() {
		// int value = (int) (System.currentTimeMillis() / 1000);
		return new User.Builder()
					.setId(new Id.Builder().setValue(10).build())
					.setImage(new Image.Builder().setId(new Id.Builder().setValue(1).build()).build())
					.setRoot(new RootNode.Builder<Integer>().setElement(123).build())
					.build();
	}
}
