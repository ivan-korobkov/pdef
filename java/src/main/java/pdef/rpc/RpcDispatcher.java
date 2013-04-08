package pdef.rpc;

import java.util.List;

public interface RpcDispatcher {

	Object dispatch(List<Call> calls, Object service);
}
