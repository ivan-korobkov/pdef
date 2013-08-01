# encoding: utf-8


class Type(object):
    '''Pdef type enum.'''

    # Base value types.
    BOOL = 'bool'
    INT16 = 'int16'
    INT32 = 'int32'
    INT64 = 'int64'
    FLOAT = 'float'
    DOUBLE = 'double'
    DECIMAL = 'decimal'
    DATE = 'date'
    DATETIME = 'datetime'
    STRING = 'string'
    UUID = 'uuid'

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

    PRIMITIVES = (BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, DECIMAL, DATE, DATETIME, STRING, UUID)
    DATATYPES = PRIMITIVES + (OBJECT, LIST, MAP, SET, DEFINITION, ENUM, MESSAGE, EXCEPTION)


class PdefException(Exception):
    '''General Pdef exception.'''
    pass
