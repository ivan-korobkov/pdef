# encoding: utf-8
import os


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


class PdefException(Exception):
    '''General Pdef exception.'''
    pass


def upper_first(s):
    '''Uppercase the first letter in a string.'''
    if not s:
        return s
    return s[0].upper() + s[1:]



def mkdir_p(dirname):
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)
