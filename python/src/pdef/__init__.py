# encoding: utf-8
from collections import deque


def proxy(interface, client):
    '''Create a interface proxy using a callable client.'''
    descriptor = interface.__descriptor__
    return Proxy(descriptor, client)


def rest_client(interface, url, session=None):
    '''Create a default REST client.'''
    from pdef.rest import RestClient, RestClientRequestsSender

    sender = RestClientRequestsSender(url, session=session)
    client = RestClient(sender)
    return proxy(interface, client)


def rest_server(interface, service_or_supplier):
    '''Create a REST server.'''
    from pdef.rest import RestServer

    descriptor = interface.__descriptor__
    return RestServer(descriptor, service_or_supplier)


def wsgi_server(interface, service_or_supplier):
    '''Create a WSGI REST server.'''
    from pdef.rest import WsgiRestServer

    server = rest_server(interface, service_or_supplier)
    return WsgiRestServer(server)


class Type(object):
    '''Pdef type enum.'''

    # Base value types.
    BOOL = 'bool'
    INT16 = 'int16'
    INT32 = 'int32'
    INT64 = 'int64'
    FLOAT = 'float'
    DOUBLE = 'double'
    STRING = 'string'

    # Collection types.
    LIST = 'list'
    MAP = 'map'
    SET = 'set'

    # Special data type.
    OBJECT = 'object'

    # User defined data types.
    DEFINITION = 'definition' # Abstract definition type, used in references.
    ENUM = 'enum'
    ENUM_VALUE = 'enum_value'
    MESSAGE = 'message'
    EXCEPTION = 'exception'

    # Interface and void.
    INTERFACE = 'interface'
    VOID = 'void'

    PRIMITIVES = (BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING)
    DATA_TYPES = PRIMITIVES + (OBJECT, LIST, MAP, SET, DEFINITION, ENUM, MESSAGE, EXCEPTION)


class Message(object):
    __descriptor__ = None

    @classmethod
    def parse_json(cls, s):
        '''Parse a message from a json string.'''
        return cls.__descriptor__.parse_json(s)

    @classmethod
    def parse_dict(cls, d):
        '''Parse a message from a dictionary.'''
        return cls.__descriptor__.parse_object(d)

    def to_json(self, indent=None):
        '''Convert this message to a json string.'''
        return self.__descriptor__.to_json(self, indent)

    def to_dict(self):
        '''Convert this message to a dictionary (serialize each field).'''
        return self.__descriptor__.to_object(self)

    def __eq__(self, other):
        if other is None or self.__class__ is not other.__class__:
            return False
        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        return not self == other

    def __str__(self):
        return unicode(self).encode('utf-8', errors='replace')

    def __unicode__(self):
        s = [u'<', self.__class__.__name__, u' ']

        first = True
        for field in self.__descriptor__.fields:
            value = field.get(self)
            if value is None:
                continue

            if first:
                first = False
            else:
                s.append(u', ')

            s.append(field.name)
            s.append('=')
            s.append(unicode(value))
        s.append(u'>')
        return u''.join(s)


class Exc(Exception, Message):
    '''Base generated pdef exception.'''
    pass


class Enum(object):
    __descriptor__ = None

    @classmethod
    def parse_json(cls, s):
        return cls.__descriptor__.parse_json(s)

    @classmethod
    def parse_string(cls, s):
        return cls.__descriptor__.parse_string(s)


class Interface(object):
    __descriptor__ = None


class Proxy(object):
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
        return Proxy(invocation.method.result, self._handler, invocation)


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
