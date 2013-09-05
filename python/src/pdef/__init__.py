# encoding: utf-8
from collections import deque


class Type(object):
    '''Pdef types.'''

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
        s = ['<', self.__class__.__name__, ' ']

        first = True
        for field in self.__descriptor__.fields:
            value = field.get(self)
            if value is None:
                continue

            if first:
                first = False
            else:
                s.append(', ')

            s.append(field.name)
            s.append('=')
            s.append(unicode(value))
        s.append('>')
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

    @classmethod
    def create_proxy(cls, callable_client):
        '''Create a client with a given protocol.'''
        return Proxy(cls.__descriptor__, callable_client)

    @classmethod
    def create_rest_client(cls, url, session=None):
        '''Create a rest client.'''
        from pdef.rest import RestClient, RequestsSender
        sender = RequestsSender(url, session)
        client = RestClient(sender)
        return cls.create_proxy(client)

    def to_rest_server(self):
        '''Create a rest server.'''
        from pdef.rest import RestServer
        return RestServer(self.__descriptor__, self)

    def to_wsgi_server(self):
        '''Create a WSGI server.'''
        from pdef.rest import WsgiRestServer
        return WsgiRestServer(self.to_rest_server())


class Proxy(object):
    '''Reflective client proxy.'''
    def __init__(self, descriptor, callable_client, parent_invocation=None):
        self._descriptor = descriptor
        self._client = callable_client
        self._invocation = parent_invocation or Invocation.root()

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.__interface)

    def __getattr__(self, name):
        '''Get a method by name and return a callable which collects the arguments
        and handles remote invocations or creates intermediate chained clients.'''
        method = None
        for m in self._descriptor.methods:
            if m.name == name:
                method = m
                break

        if not method:
            raise AttributeError(name)

        return lambda *args, **kwargs: self._handle(method, *args, **kwargs)

    def _handle(self, method, *args, **kwargs):
        '''Capture a method invocation with the given arguments and return a new client
        if the method result is an interface or pass the invocation to the handler.
        '''
        invocation = self._invocation.next(method, *args, **kwargs)
        if method.is_remote:
            # The method result is a data type or void.
            return self._client(invocation)

        # The method result is an interface, so create a new client for it.
        return Proxy(method.result, self._client, invocation)


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
        for inv in chain:
            obj = inv.invoke_single(obj)
        return obj

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
        consumed_kargs = {}
        for arg in method_args:
            if arg.name not in kwargs:
                params[arg.name] = None
                continue

            value = kwargs.get(arg.name)
            params[arg.name] = value
            consumed_kargs[arg.name] = value

        # Check that all kwargs have been consumed.
        if consumed_kargs.keys() != kwargs.keys():
            raise wrong_args()

        return params
