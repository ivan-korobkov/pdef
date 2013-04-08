package pdef.rpc;

import java.util.List;

public interface RpcSerializer {

	Object serialize(List<Call> calls);
}
