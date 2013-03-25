package pdef.formats;

public interface Format extends Serializer {

	Object parse(Object object);
}
