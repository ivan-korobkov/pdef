package pdef.rpc;

import java.util.List;

public interface RpcHandler {

	Object handle(List<Call> calls);
}
