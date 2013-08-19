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
        return cls.__descriptor__.parse(d)

    def to_json(self, indent=None):
        '''Convert this message to a json string.'''
        return self.__descriptor__.serialize_to_json(self, indent)

    def to_dict(self):
        '''Convert this message to a dictionary (serialize each field).'''
        return self.__descriptor__.serialize_to_dict(self)

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
