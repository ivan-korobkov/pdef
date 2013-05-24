package pdef.test;
import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncInterface extends io.pdef.Interface {
    ListenableFuture<Void> method();

    ListenableFuture<Integer> sum(@io.pdef.Name("i0") int i0, @io.pdef.Name("i1") int i1);

    ListenableFuture<String> echo(@io.pdef.Name("text") String text);

}