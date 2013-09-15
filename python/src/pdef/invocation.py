# encoding: utf-8
from collections import deque


def proxy(interface, handler):
    '''Create a interface proxy using a callable invocation handler.

    @param interface:   Class with a __descriptor__.
    @param handler:     Callable(Invocation) => InvocationResult.
    '''
    descriptor = interface.__descriptor__
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

    def __init__(self, method, parent, args=None, kwargs=None):
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
        self.__unicode = None  # Cache invocation unicode repr.

        self._check_arg_types()

    def _check_arg_types(self):
        if self.is_root:
            return

        method = self.method
        for arg in method.args:
            value = self.args.get(arg.name)
            descriptor = arg.type

            if not descriptor.is_valid_type(value):
                raise TypeError('Wrong method arguments, method=%s, args=%r' % (method, self.args))

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
                obj = inv.invoke_single(obj)
        except exc_class, e:
            # Catch the expected application exception.
            # It's valid to write 'except None, e' when no application exception.
            return InvocationResult(e, ok=False)

        return InvocationResult(obj)

    def invoke_single(self, obj):
        '''Invoke only this invocation (not a chain) on an object.'''
        method = self.method
        result = method.invoke(obj, **self.args)

        if not method.result.is_valid_type(result):
            raise TypeError('Wrong method result, method=%s, result=%r' % (method, result))

        return result

    def __str__(self):
        return unicode(self).encode('utf-8', 'replace')

    def __unicode__(self):
        if self.__unicode:
            return self.__unicode

        s = []
        first = True
        for inv in self.to_chain():
            if first:
                first = False
            else:
                s.append(u'.')
            s.append(inv.method.name)
            s.append(u'(')

            first_arg = True
            for arg in inv.method.args:
                if first_arg:
                    first_arg = False
                else:
                    s.append(u', ')
                value = inv.args.get(arg.name)
                s.append(arg.name)
                s.append(u'=')
                s.append(unicode(value))
            s.append(u')')
        self.__unicode = u''.join(s)
        return self.__unicode

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
    '''Combines success and exception invocation results.'''
    def __init__(self, data, ok=True):
        self.data = data
        self.ok = ok


class InvocationProxy(object):
    '''Reflective client proxy.'''
    def __init__(self, descriptor, callable_handler, parent_invocation=None):
        self._descriptor = descriptor
        self._handler = callable_handler
        self._invocation = parent_invocation or Invocation.root()

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.__interface)

    def __getattr__(self, name):
        '''Get a pdef method by name and return a callable which proxies its calls.'''
        for m in self._descriptor.methods:
            if m.name == name:
                return lambda *args, **kwargs: self._invoke(m, *args, **kwargs)

        raise AttributeError('Method not found: %s' % name)

    def _invoke(self, method, *args, **kwargs):
        '''Handle a pdef method invocation.
        First, capture the invocation. Then, handle it if the method is remote,
        and return the result if invocation_result is OK, or raise the result exception.
        If the method is not remote, create a next proxy for the method result interface.
        '''
        invocation = self._capture(method, *args, **kwargs)
        if method.is_remote:
            # The method result is a data type or void.
            return self._handle(invocation)

        # The method result is an interface, so create a new client for it.
        return self._next_proxy(invocation)

    def _capture(self, method, *args, **kwargs):
        return self._invocation.next(method, *args, **kwargs)

    def _handle(self, invocation):
        result = self._handler(invocation)
        assert result

        if result.ok:
            return result.data

        raise result.data

    def _next_proxy(self, invocation):
        return InvocationProxy(invocation.method.result, self._handler, invocation)


class Invoker(object):
    '''Executes an invocation and returns InvocationResult.'''
    def __init__(self, provider):
        self.provider = provider

    def __call__(self, invocation):
        return self.invoke(invocation)

    def invoke(self, invocation):
        obj = self.provider()
        return invocation.invoke(obj)
