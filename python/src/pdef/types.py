# encoding: utf-8
import copy
import pdef.formats


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
    DATA_TYPES = PRIMITIVES + (LIST, MAP, SET, DEFINITION, ENUM, MESSAGE, EXCEPTION)


class Message(object):
    DESCRIPTOR = None

    @classmethod
    def from_json(cls, s, **kwargs):
        '''Parse a message from a json string.'''
        return pdef.json_format.from_json(s, cls.DESCRIPTOR, **kwargs)

    @classmethod
    def from_json_stream(cls, fp, **kwargs):
        '''Parse a message from a json file-like object.'''
        return pdef.json_format.from_json_stream(fp, cls.DESCRIPTOR, **kwargs)

    @classmethod
    def from_dict(cls, d):
        '''Parse a message from a dictionary.'''
        return pdef.object_format.parse(d, cls.DESCRIPTOR)

    def to_json(self, indent=None, **kwargs):
        '''Convert this message to a json string.'''
        return pdef.json_format.to_json(self, self.DESCRIPTOR, indent=indent)

    def to_json_stream(self, fp, indent=None, **kwargs):
        '''Serialize this message as a json string to a file-like stream.'''
        return pdef.json_format.to_json_stream(self, self.DESCRIPTOR, fp, indent=indent, **kwargs)

    def to_dict(self):
        '''Convert this message to a dictionary (serialize each field).'''
        return pdef.object_format.serialize(self, self.DESCRIPTOR)

    def __eq__(self, other):
        if other is None or self.__class__ is not other.__class__:
            return False
        return self.__dict__ == other.__dict__

    def __ne__(self, other):
        return not self == other

    def __str__(self):
        return unicode(self).encode('utf-8', errors='replace')

    def __copy__(self):
        msg = self.__class__()
        msg.__dict__ = copy.copy(self.__dict__)
        return msg

    def __deepcopy__(self, memo=None):
        msg = self.__class__()
        msg.__dict__ = copy.deepcopy(self.__dict__, memo)
        return msg

    def __unicode__(self):
        s = [u'<', self.__class__.__name__, u' ']

        first = True
        for field in self.DESCRIPTOR.fields:
            value = field.get(self)
            if value is None:
                continue

            if first:
                first = False
            else:
                s.append(u', ')

            s.append(field.name)
            s.append('=')
            s.append(unicode(value))
        s.append(u'>')
        return u''.join(s)


class Exc(Exception, Message):
    '''Base generated pdef exception.'''
    pass


class Enum(object):
    DESCRIPTOR = None

    @classmethod
    def parse_json(cls, s):
        return pdef.json_format.from_json(s, cls.DESCRIPTOR)


class Interface(object):
    DESCRIPTOR = None
