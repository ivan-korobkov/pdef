# encoding: utf-8
import copy
from collections import deque


def proxy(interface, invocation_handler):
    '''Create a interface proxy.

    The proxy captures method calls into chained invocations, and passes them
    to the handler on remote methods.

    @param interface:               Interface with a class DESCRIPTOR field.
    @param invocation_handler:      callable(Invocation): InvocationResult.
    '''
    descriptor = interface.DESCRIPTOR
    return InvocationProxy(descriptor, invocation_handler)


def invoker(service_or_provider):
    '''Create an invoker for a service or a callable service provider.'''
    provider = service_or_provider if callable(service_or_provider) else lambda: service_or_provider
    return Invoker(provider)


class Invocation(object):
    '''Immutable chained method invocation.'''
    @classmethod
    def root(cls, method, *args, **kwargs):
        return Invocation(method, args=args, kwargs=kwargs)

    def __init__(self, method, args=None, kwargs=None, parent=None):
        '''Create an rpc invocation.

        @param method: The method descriptor this invocation has been invoked on.
        @param args: {} of arguments.
        @param parent: a parent invocation, nullable.
        '''
        self.method = method
        self.parent = parent
        self.args = self._build_args(method, args, kwargs) if method else {}
        self.args = copy.deepcopy(self.args)    # Make defensive copies.

        self.result = method.result if method else None
        self.exc = method.exc if method else (parent.exc if parent else None)

    def __repr__(self):
        return '<Invocation %r args=%r>' % (self.method.name, self.args)

    @property
    def is_remote(self):
        return self.method.is_remote

    def next(self, method, *args, **kwargs):
        '''Create a child invocation.'''
        if self.method and self.method.is_remote:
            raise ValueError('Cannot create next invocation for a remote method invocation: %s'
                             % self)
        return Invocation(method, args=args, kwargs=kwargs, parent=self)

    def to_chain(self):
        '''Return a list of invocations.'''
        if not self.parent:
            return [self]

        chain = self.parent.to_chain()
        chain.append(self)
        return chain

    def invoke(self, obj):
        '''Invoke this invocation chain on an object and return InvocationResult.'''
        chain = self.to_chain()
        exc_class = self.exc.pyclass if self.exc else None

        try:
            for inv in chain:
                obj = inv.method.invoke(obj, **inv.args)
        except exc_class as e:
            # Catch the expected application exception.
            # Python support dynamic exceptions.
            # It's valid to write 'except None, e' when no application exception.
            return InvocationResult.from_exc(e)

        return InvocationResult.from_data(obj)

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
            argd = method_args.popleft()
            params[argd.name] = value

        # Add keyword arguments using the remaining method args.
        consumed_kwargs = {}
        for argd in method_args:
            if argd.name not in kwargs:
                params[argd.name] = None
                continue

            value = kwargs.get(argd.name)
            params[argd.name] = value
            consumed_kwargs[argd.name] = value

        # Check that all kwargs have been consumed.
        if consumed_kwargs.keys() != kwargs.keys():
            raise wrong_args()

        return params


class InvocationResult(object):
    '''InvocationResult combines the returned value and the exception.'''
    @classmethod
    def from_data(cls, data):
        return InvocationResult(True, data=data)

    @classmethod
    def from_exc(cls, exc):
        return InvocationResult(False, exc=exc)

    def __init__(self, ok, data=None, exc=None):
        self.ok = ok
        self.data = data
        self.exc = exc


class InvocationProxy(object):
    '''Reflective client proxy.'''
    def __init__(self, descriptor, handler, invocation=None):
        self._descriptor = descriptor
        self._handler = handler
        self._invocation = invocation

    def __repr__(self):
        return '<InvocationProxy %s>' % self._descriptor

    def __getattr__(self, name):
        '''Return a proxy method.'''
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
        if result.ok:
            return result.data

        raise result.exc


class Invoker(object):
    '''Invoker applies invocations to a service and returns InvocationResult.'''
    def __init__(self, provider):
        self.provider = provider

    def __call__(self, invocation):
        return self.invoke(invocation)

    def invoke(self, invocation):
        obj = self.provider()
        return invocation.invoke(obj)
