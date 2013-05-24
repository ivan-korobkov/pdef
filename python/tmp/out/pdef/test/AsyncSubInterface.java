package pdef.test;
import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncSubInterface extends pdef.test.AsyncInterface, pdef.test.AsyncSupport {

    ListenableFuture<pdef.test.SubMessage3> submethod(@io.pdef.Name("msg1") pdef.test.SubMessage1 msg1, @io.pdef.Name("msg2") pdef.test.SubMessage2 msg2);

}