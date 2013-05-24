package pdef.test;
import com.google.common.util.concurrent.ListenableFuture;

public interface Interface extends io.pdef.Interface {
    void method();

    int sum(@io.pdef.Name("i0") int i0, @io.pdef.Name("i1") int i1);

    String echo(@io.pdef.Name("text") String text);

}