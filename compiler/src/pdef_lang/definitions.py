# encoding: utf-8
import pdef_lang.validation as validation


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
    ENUM = 'enum'
    ENUM_VALUE = 'enum_value'
    MESSAGE = 'message'
    EXCEPTION = 'exception'

    # Interface and void.
    INTERFACE = 'interface'
    VOID = 'void'

    PRIMITIVES = (BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING)
    DATA_TYPES = PRIMITIVES + (OBJECT, LIST, MAP, SET, ENUM, MESSAGE, EXCEPTION)


class Definition(object):
    '''Base definition.'''
    location = None
    is_exception = False  # The flag is set in a message constructor.

    def __init__(self, type0, name, doc=None, location=None):
        self.type = type0
        self.name = name
        self.doc = doc
        self.location = location

        self.module = None

        self.is_primitive = self.type in Type.PRIMITIVES
        self.is_data_type = self.type in Type.DATA_TYPES
        self.is_interface = self.type == Type.INTERFACE
        self.is_message = self.type == Type.MESSAGE

        self.is_enum = self.type == Type.ENUM

        self.is_list = self.type == Type.LIST
        self.is_set = self.type == Type.SET
        self.is_map = self.type == Type.MAP

    def __repr__(self):
        return '<%s %s at %s>' % (self.__class__.__name__, self.name, hex(id(self)))

    def __str__(self):
        return self.name

    def link(self, linker):
        return []

    def validate(self):
        return []

    def _validate_is_defined_before(self, another):
        '''Validate that a definition is reference before another one.'''
        if not self.module or not another.module:
            return []

        if self.module is another.module:
            # They are in the same module.

            for def0 in self.module.definitions:
                if def0 is self:
                    return []

                if def0 is another:
                    return [validation.error(self, '%s must be defined before %s. Move it above '
                                                   'in the file.', self, another)]

            raise AssertionError('Wrong module state')

        if self.module._has_import_circle(another.module):
            return [validation.error(self,
                   '%s must be referenced before %s, but their modules circularly '
                   'import each other. Move %s into another module.', self, another, self)]

        return []


class NativeType(Definition):
    '''Native type definition, i.e. it defines a native language type such as string, int, etc.'''
    def __init__(self, type0):
        super(NativeType, self).__init__(type0, type0)


class NativeTypes(object):
    '''Native types.'''
    BOOL = NativeType(Type.BOOL)
    INT16 = NativeType(Type.INT16)
    INT32 = NativeType(Type.INT32)
    INT64 = NativeType(Type.INT64)
    FLOAT = NativeType(Type.FLOAT)
    DOUBLE = NativeType(Type.DOUBLE)
    STRING = NativeType(Type.STRING)

    OBJECT = NativeType(Type.OBJECT)
    VOID = NativeType(Type.VOID)

    _BY_TYPE = {
        Type.BOOL: BOOL,
        Type.INT16: INT16,
        Type.INT32: INT32,
        Type.INT64: INT64,
        Type.FLOAT: FLOAT,
        Type.DOUBLE: DOUBLE,
        Type.STRING: STRING,
        Type.OBJECT: OBJECT,
        Type.VOID: VOID
    }

    @classmethod
    def get_by_type(cls, t):
        '''Returns a value by its type or none.'''
        return cls._BY_TYPE.get(t)
