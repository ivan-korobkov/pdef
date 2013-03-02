package pdef.fixtures;

public class Main {

	public static void main(String[] args) {
		User user = new User.Builder()
				.setId(new Id.Builder().setValue(10).build())
				.setImage(new Image.Builder().setId(new Id.Builder().setValue(23).build()).build())
				.setRoot(new RootNode.Builder<Integer>().setElement(123).build())
				.build();

		System.out.println(user);
	}
}
