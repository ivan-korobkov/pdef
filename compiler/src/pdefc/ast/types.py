# encoding: utf-8
from pdefc.ast.common import Located, Validatable


class TypeEnum(object):
    '''TypeEnum is an enumeration of all pdef type tokens.'''

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

    # User defined data types.
    ENUM = 'enum'
    ENUM_VALUE = 'enum_value'
    MESSAGE = 'message'
    EXCEPTION = 'exception'

    # Interface and void.
    INTERFACE = 'interface'
    VOID = 'void'

    PRIMITIVES = (BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING)
    DATA_TYPES = PRIMITIVES + (LIST, SET, MAP, ENUM, MESSAGE, EXCEPTION)
    NATIVE = PRIMITIVES + (VOID, )
    COLLECTION_TYPES = (LIST, SET, MAP)

    @classmethod
    def is_message(cls, type_enum):
        return type_enum in (cls.MESSAGE, cls.EXCEPTION)

    @classmethod
    def is_interface(cls, type_enum):
        return type_enum == cls.INTERFACE

    @classmethod
    def is_primitive(cls, type_enum):
        return type_enum in cls.PRIMITIVES

    @classmethod
    def is_data_type(cls, type_enum):
        return type_enum in cls.is_data_type(type_enum)

    @classmethod
    def is_collection(cls, type_enum):
        return type_enum in cls.COLLECTION_TYPES


class Type(Located, Validatable):
    '''Type is a common class for all pdef types. These include native types, definitions,
    collections, and enum values.'''
    def __init__(self, type0, location=None):
        self.type = type0
        self.location = location

        self.is_primitive = self.type in TypeEnum.PRIMITIVES
        self.is_data_type = self.type in TypeEnum.DATA_TYPES
        self.is_native = self.type in TypeEnum.NATIVE

        self.is_message = self.type == TypeEnum.MESSAGE or self.type == TypeEnum.EXCEPTION
        self.is_exception = self.type == TypeEnum.EXCEPTION
        self.is_interface = self.type == TypeEnum.INTERFACE
        self.is_enum = self.type == TypeEnum.ENUM
        self.is_enum_value = self.type == TypeEnum.ENUM_VALUE

        self.is_list = self.type == TypeEnum.LIST
        self.is_set = self.type == TypeEnum.SET
        self.is_map = self.type == TypeEnum.MAP

    def __str__(self):
        return self.type

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.type, hex(id(self)))


class NativeType(object):
    '''Native type singletons.'''
    BOOL = Type(TypeEnum.BOOL)
    INT16 = Type(TypeEnum.INT16)
    INT32 = Type(TypeEnum.INT32)
    INT64 = Type(TypeEnum.INT64)
    FLOAT = Type(TypeEnum.FLOAT)
    DOUBLE = Type(TypeEnum.DOUBLE)
    STRING = Type(TypeEnum.STRING)
    VOID = Type(TypeEnum.VOID)

    _BY_TYPE = {
        TypeEnum.BOOL: BOOL,
        TypeEnum.INT16: INT16,
        TypeEnum.INT32: INT32,
        TypeEnum.INT64: INT64,
        TypeEnum.FLOAT: FLOAT,
        TypeEnum.DOUBLE: DOUBLE,
        TypeEnum.STRING: STRING,
        TypeEnum.VOID: VOID
    }

    @classmethod
    def get(cls, name):
        '''Return a value by its type or none.'''
        return cls._BY_TYPE.get(name)

    @classmethod
    def all(cls):
        '''Return all native types.'''
        return list(cls._BY_TYPE.values())


class Definition(Type):
    '''Definition is a base for user-specified types. These include messages,
    interfaces and enums.'''

    def __init__(self, type0, name, doc=None, location=None):
        super(Definition, self).__init__(type0, location=location)
        self.name = name
        self.doc = doc
        self.module = None

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def __str__(self):
        return self.name

    def lookup(self, name):
        return self.module.lookup(name) if self.module else None

    def link(self, module):
        '''Link this definition references and return a list of errors.'''
        if self.module:
            raise ValueError('Definition is already linked')

        self.module = module
        return []

    def build(self):
        '''Build this definition after linking and return a list of errors.'''
        return []

    def _is_defined_after(self, another):
        '''Return true if this definition is after another one in one module.'''
        if not self.module or not another.module:
            return True

        if self.module is not another.module:
            return True

        for def0 in self.module.definitions:
            if def0 is another:
                return True

            if def0 is self:
                return False

        raise AssertionError('Unnreachable code')
