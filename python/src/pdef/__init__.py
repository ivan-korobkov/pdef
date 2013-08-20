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
    DATATYPES = PRIMITIVES + (OBJECT, LIST, MAP, SET, DEFINITION, ENUM, MESSAGE, EXCEPTION)


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
        return self.__descriptor__.to_dict(self)

    def merge_dict(self, d):
        '''Merge this message with a dictionary (parse each field).'''
        self.__descriptor__.merge_dict(self, d)
        return self

    def __eq__(self, other):
        if other is None or self.__class__ is not other.__class__:
            return False
        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        return not self == other


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


class ClientProxy(object):
    '''Proxy for interface invocations. '''
    def __init__(self, interface, handler, parent_invocation=None):
        self.__interface = interface
        self.__handler = handler
        self.__parent_invocation = parent_invocation or Invocation.root()

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.__interface)

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
        return ClientProxy(method.result, self.__handler, invocation)


class Invocation(object):
    '''Immutable chained RPC invocation.'''
    @classmethod
    def root(cls):
        '''Create an empty root invocation.'''
        return Invocation(None, None, None)

    def __init__(self, method, params, parent=None):
        '''Create an rpc invocation.

        @param method: The method descriptor this invocation has been invoked on.
        @param params: {} of arguments.
        @param parent: a parent invocation, nullable.
        '''
        self.method = method
        self.params = dict(params) if params else {}
        self.parent = parent

    @property
    def exc(self):
        return self.method.exc

    @property
    def result(self):
        return self.method.result

    @property
    def is_root(self):
        return self.method is None

    def next(self, method, *args, **kwargs):
        params = self._build_param_dict(method, args, kwargs)
        return Invocation(self, method, params)

    def invoke(self, obj):
        '''Invoke this invocation on a given object and return the result.'''
        return self.method.invoke(obj, **self.params)

    @staticmethod
    def _build_param_dict(method, args, kwargs):
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
