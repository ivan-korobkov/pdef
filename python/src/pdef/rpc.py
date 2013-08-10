# encoding: utf-8


class RpcClient(object):
    '''RPC client which acts as a proxy, collects invocations,
    and passes the last to a handler. '''
    def __init__(self, interface, handler, parent_invocation=None):
        self.__interface = interface
        self.__handler = handler
        self.__parent_invocation = parent_invocation or RpcInvocation.root()

    def __getattr__(self, name):
        '''Get a method by name and return a callable which collects the arguments
        and handles remote invocations or creates intermediate chained clients.'''
        method = self.__interface.get_method(name)
        if not method:
            raise AttributeError(name)

        return lambda *args, **kwargs: self.__handle(method, *args, **kwargs)

    def __handle(self, method, *args, **kwargs):
        '''Capture a method invocation with the given arguments and return a new client
        if the method result is an interface or pass the invocation to the handler.
        '''
        invocation = self.__parent_invocation.next(method, *args, **kwargs)
        if method.is_remote:
            # The method result is a data type or void.
            return self.__handler(invocation)

        # The method result is an interface, so create a new client for it.
        return RpcClient(method.result, self.__handler, invocation)


class RpcInvocation(object):
    '''Immutable chained RPC invocation.'''
    @classmethod
    def root(cls):
        '''Create an empty root invocation.'''
        return RpcInvocation(None, None, None)

    def __init__(self, method, params, parent=None):
        '''Create an rpc invocation.

        @param method: The method descriptor this invocation has been invoked on.
        @param params: {} of arguments.
        @param parent: a parent invocation, nullable.
        '''
        self.method = method
        self.params = dict(params) if params else {}
        self.parent = parent

    def next(self, method, *args, **kwargs):
        params = build_param_dict(method, args, kwargs)
        return RpcInvocation(self, method, params)

    def invoke(self, obj):
        '''Invoke this invocation on a given object and return the result.'''
        return self.method.invoke(obj, **self.params)


def build_param_dict(method, args, kwargs):
    '''Convert args and kwargs into a param dictionary.'''
    params = {}

    # First, consume all positional arguments.
    args_iter = iter(args)
    param_iter = iter(method.args)
    for arg, name, type0 in zip(args_iter, param_iter):
        params[name] = arg

    # Then, add keyword arguments.
    for key, arg in kwargs.items():
        params[key] = arg

    return params


class HttpClient(object):
    def __init__(self, url, interface):
        self.url = url
        self.interface = interface

    def proxy(self):
        return RpcClient(self.interface, self._handle)

    def _handle(self, invocation):
        chain = invocation.to_chain()

        path = '/'
        for invocation in chain:
            method = invocation.method
            params = invocation.params

            path += method.name + '/'
            for name, type0 in method.args:
                param = params.get(name)
                if param is None:
                    param = 0

                s = type0.to_string(param)
                path += s + '/'
