package pdef.test;
import com.google.common.util.concurrent.ListenableFuture;

public interface AsyncSupport extends io.pdef.Interface {
    ListenableFuture<Void> callSupport();

}