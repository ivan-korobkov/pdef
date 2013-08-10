# encoding: utf-8
from pdef import _json as json


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
        if s is None:
            return None

        d = json.loads(s)
        return cls.parse_dict(d)

    @classmethod
    def parse_dict(cls, d):
        desc = cls.__descriptor__

        discriminator = desc.discriminator
        if discriminator:
            type0 = discriminator.type.parse(d.get(discriminator.name))
            subtype_supplier = desc.subtypes.get(type0)
            if subtype_supplier:
                return subtype_supplier().parse_dict(d)

        instance = cls()
        instance.merge_dict(d)
        return instance

    def to_json(self, indent=None):
        d = self.to_dict()
        return json.dumps(d, indent=indent)

    def to_dict(self):
        '''Convert this message to a dictionary (serialize each field).'''
        d = {}

        fields = self.__descriptor__.fields
        for field in fields:
            if not field.is_set(self):
                continue

            value = field.get(self)
            data = field.type.serialize(value)
            d[field.name] = data
        return d

    def merge_dict(self, d):
        '''Merge this message with a dictionary (parse each field).'''
        fields = self.__descriptor__.fields
        for field in fields:
            if field.name not in d:
                continue

            data = d[field.name]
            value = field.type.parse(data)
            field.set(self, value)
        return self

    def __eq__(self, other):
        if other is None or self.__class__ is not other.__class__:
            return False
        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        return not self == other


class Enum(object):
    __descriptor__ = None

    @classmethod
    def parse_json(cls, s):
        if s is None:
            return None

        value = json.loads(s)
        return cls.parse_string(value)

    @classmethod
    def parse_string(cls, s):
        if s is None:
            return None
        return cls.__descriptor__.parse_string(s)


class BaseException(Exception, Message):
    '''Base generated pdef exception.'''
    pass


class Interface(object):
    __descriptor__ = None

    @classmethod
    def rpc_client(cls, handler):
        pass

    @classmethod
    def http_client(cls, url):
        pass
