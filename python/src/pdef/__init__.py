# encoding: utf-8


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
        s = ['<', self.__class__.__name__, ' ']
        for field in self.__descriptor__.fields:
            value = field.get(self)
            if value is None:
                continue
            s.append(' ')
            s.append(field.name)
            s.append('=')
            s.append(str(value))
            s.append(',')
        s.append('>')
        return ''.join(s)


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
        from pdef.rest import RestClient
        client = RestClient(url, session)
        return cls.create_proxy(client)

    def create_rest_server(self):
        '''Create a rest server.'''
        from pdef.rest import RestServer
        return RestServer(self.__descriptor__, self)

    def create_wsgi_server(self):
        pass


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

    def __init__(self, method, args, parent=None):
        '''Create an rpc invocation.

        @param method: The method descriptor this invocation has been invoked on.
        @param args: {} of arguments.
        @param parent: a parent invocation, nullable.
        '''
        self.method = method
        self.args = dict(args) if args else {}
        self.parent = parent

        self.exc = None #method.exc if method else None
        self.result = method.result if method else None
        self.is_root = method is None

    def next(self, method, *args, **kwargs):
        '''Create a child invocation.'''
        params = self._build_param_dict(method, args, kwargs)
        return Invocation(method, params, parent=self)

    def invoke(self, obj):
        '''Invoke this invocation on a given object and return the result.'''
        return self.method.invoke(obj, **self.args)

    def to_chain(self):
        '''Return a list of invocations.'''
        chain = [] if not self.parent else self.parent.to_chain()
        if not self.is_root:
            chain.append(self)
        return chain

    @staticmethod
    def _build_param_dict(method, args, kwargs):
        '''Convert args and kwargs into a param dictionary.'''
        params = {}

        # First, consume all positional arguments.
        for arg, value in zip(method.args, args):
            params[arg.name] = value

        # Then, add keyword arguments.
        for key, arg in kwargs.items():
            params[key] = arg

        return params
