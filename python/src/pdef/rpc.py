# encoding: utf-8
import pdef
from pdef import descriptors


class RpcStatus(pdef.Enum):
    """
    RPC status.
    OK - successful response.
    EXCEPTION - successful response, expected application exception.
    """

    OK = 'OK'
    EXCEPTION = 'EXCEPTION'

    __descriptor__ = pdef.descriptors.enum(lambda: RpcStatus,
        values=[OK, EXCEPTION])


class RpcResult(pdef.Message):
    def __init__(self, status=None, data=None): 
        self.status = status
        self.data = data

    __descriptor__ = descriptors.message(lambda: RpcResult,
        declared_fields=[
            descriptors.field('status', lambda: RpcStatus.__descriptor__), 
            descriptors.field('data', lambda: descriptors.object0)
        ],
    )


class RpcError(pdef.Exc):
    """
    Internal exceptions which can be used in clients and servers.
    They should be mapped to corresponding rpc statuses in responses.
    """

    def __init__(self, text=None): 
        self.text = text

    __descriptor__ = descriptors.message(lambda: RpcError,
        declared_fields=[
            descriptors.field('text', lambda: descriptors.string)
        ],
    )


class ServerError(RpcError):
    """Internal server error."""

    def __init__(self, text=None): 
        super(ServerError, self).__init__(
            text=text) 
        pass

    __descriptor__ = descriptors.message(lambda: ServerError,
        base=RpcError.__descriptor__,
        discriminator_value=None,
    )


class ServiceUnavailableError(RpcError):
    """Temporary network error, service unavailable, the request can be repeated."""

    def __init__(self, text=None): 
        super(ServiceUnavailableError, self).__init__(
            text=text) 
        pass

    __descriptor__ = descriptors.message(lambda: ServiceUnavailableError,
        base=RpcError.__descriptor__,
        discriminator_value=None,
    )


class ClientError(RpcError):
    """Client error (bad request), the request should not be repeated."""

    def __init__(self, text=None): 
        super(ClientError, self).__init__(
            text=text) 
        pass

    __descriptor__ = descriptors.message(lambda: ClientError,
        base=RpcError.__descriptor__,
        discriminator_value=None,
    )


class MethodNotFoundError(ClientError):
    """Method not found."""

    def __init__(self, text=None): 
        super(MethodNotFoundError, self).__init__(
            text=text) 
        pass

    __descriptor__ = descriptors.message(lambda: MethodNotFoundError,
        base=ClientError.__descriptor__,
        discriminator_value=None,
    )


class WrongMethodArgsError(ClientError):
    """Wrong method arguments."""

    def __init__(self, text=None): 
        super(WrongMethodArgsError, self).__init__(
            text=text) 
        pass

    __descriptor__ = descriptors.message(lambda: WrongMethodArgsError,
        base=ClientError.__descriptor__,
        discriminator_value=None,
    )


class MethodNotAllowedError(ClientError):
    """HTTP method is not allowed, for example, a get request to a post method."""

    def __init__(self, text=None): 
        super(MethodNotAllowedError, self).__init__(
            text=text) 
        pass

    __descriptor__ = descriptors.message(lambda: MethodNotAllowedError,
        base=ClientError.__descriptor__,
        discriminator_value=None,
    )


