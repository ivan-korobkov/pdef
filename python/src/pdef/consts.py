# encoding: utf-8


class Type(object):
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

    # Special value types.
    OBJECT = 'object'
    VOID = 'void'

    # Collection types.
    LIST = 'list'
    MAP = 'map'
    SET = 'set'

    # User types.
    DEFINITION = 'definition' # Abstract definition type, used in references.
    ENUM = 'enum'
    ENUM_VALUE = 'enum_value'
    MESSAGE = 'message'
    EXCEPTION = 'exception'
    INTERFACE = 'interface'
