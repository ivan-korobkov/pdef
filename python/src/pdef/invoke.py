# encoding: utf-8
from collections import deque


def proxy(interface, handler):
    '''Create a interface proxy using a callable invocation handler.

    @param interface:   Class with a DESCRIPTOR.
    @param handler:     Callable(Invocation) => InvocationResult.
    '''
    descriptor = interface.DESCRIPTOR
    return InvocationProxy(descriptor, handler)


def invoker(service_or_provider):
    '''Create an invoker for a service or a callable service provider.'''
    provider = service_or_provider if callable(service_or_provider) else lambda: service_or_provider
    return Invoker(provider)


class Invocation(object):
    '''Immutable chained method invocation.'''
    @classmethod
    def root(cls):
        '''Create an empty root invocation.'''
        return Invocation(None, None, None)

    def __init__(self, method, args=None, kwargs=None, parent=None):
        '''Create an rpc invocation.

        @param method: The method descriptor this invocation has been invoked on.
        @param args: {} of arguments.
        @param parent: a parent invocation, nullable.
        '''
        self.method = method
        self.parent = parent
        self.args = self._build_args(method, args, kwargs) if method else {}

        self.result = method.result if method else None
        self.exc = method.exc if method else (parent.exc if parent else None)

        self.is_root = method is None

    @property
    def is_remote(self):
        return self.method.is_remote if self.method else False

    def next(self, method, *args, **kwargs):
        '''Create a child invocation.'''
        if self.method and self.method.is_remote:
            raise TypeError('Cannot create next invocation for a remote method invocation: %s'
                            % self)
        return Invocation(method, parent=self, args=args, kwargs=kwargs)

    def to_chain(self):
        '''Return a list of invocations.'''
        chain = [] if not self.parent else self.parent.to_chain()
        if not self.is_root:
            chain.append(self)
        return chain

    def invoke(self, obj):
        '''Invoke this invocation chain on an object.'''
        chain = self.to_chain()
        exc_class = self.exc.pyclass if self.exc else None

        try:
            for inv in chain:
                obj = inv.method.invoke(obj, **inv.args)
        except exc_class, e:
            # Catch the expected application exception.
            # It's valid to write 'except None, e' when no application exception.
            return InvocationResult.exception(e)

        return InvocationResult.ok(obj)

    def __repr__(self):
        return '<Invocation %r args=%r>' % (self.method.name, self.args)

    @staticmethod
    def _build_args(method, args=None, kwargs=None):
        '''Convert args and kwargs into a param dictionary, check their number and types.'''
        def wrong_args():
            return TypeError('Wrong method arguments, %s, got args=%s, kwargs=%s' %
                             (method, args, kwargs))
        params = {}
        args = args if args else ()
        kwargs = kwargs if kwargs else {}
        method_args = deque(method.args)

        # Check that the number of args and kwargs is less or equal to the method args number.
        if len(method_args) < (len(args) + len(kwargs)):
            raise wrong_args()

        # Consume all positional arguments.
        for value in args:
            arg = method_args.popleft()
            params[arg.name] = value


        # Add keyword arguments using the remaining method args.
        consumed_kwargs = {}
        for arg in method_args:
            if arg.name not in kwargs:
                params[arg.name] = None
                continue

            value = kwargs.get(arg.name)
            params[arg.name] = value
            consumed_kwargs[arg.name] = value

        # Check that all kwargs have been consumed.
        if consumed_kwargs.keys() != kwargs.keys():
            raise wrong_args()

        return params


class InvocationResult(object):
    '''InvocationResult combines the returned value and the exception.'''
    @classmethod
    def ok(cls, data):
        return InvocationResult(True, data=data)

    @classmethod
    def exception(cls, exc):
        return InvocationResult(False, exc=exc)

    def __init__(self, success, data=None, exc=None):
        self.success = success
        self.data = data
        self.exc = exc


class InvocationProxy(object):
    '''Reflective client proxy.'''
    def __init__(self, descriptor, handler, invocation=None):
        self._descriptor = descriptor
        self._handler = handler
        self._invocation = invocation

    def __repr__(self):
        return '<InvocationProxy %s>' % self._interface

    def __getattr__(self, name):
        '''Get a pdef method by name and return a callable which proxies its calls.'''
        method = self._descriptor.find_method(name)
        if not method:
            raise AttributeError('Method not found %r' % name)

        return _ProxyMethod(self._invocation, self._handler, method)


class _ProxyMethod(object):
    def __init__(self, invocation, handler, method):
        self.invocation = invocation
        self.handler = handler
        self.method = method

    def __repr__(self):
        return '<ProxyMethod %r>' % self.method.name

    def __call__(self, *args, **kwargs):
        method = self.method
        if self.invocation:
            invocation = self.invocation.next(method, *args, **kwargs)
        else:
            invocation = Invocation(method, args=args, kwargs=kwargs)

        if not method.is_remote:
            # This is a method, which returns an interface.
            # Create a next invocation proxy.
            return InvocationProxy(method.result, self.handler, invocation)

        # The method result is a data type or void.
        result = self.handler(invocation)
        if result.success:
            return result.data

        raise result.exc


class Invoker(object):
    '''Executes an invocation and returns InvocationResult.'''
    def __init__(self, provider):
        self.provider = provider

    def __call__(self, invocation):
        return self.invoke(invocation)

    def invoke(self, invocation):
        obj = self.provider()
        return invocation.invoke(obj)
