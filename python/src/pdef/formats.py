# encoding: utf-8
from collections import OrderedDict
import json as _json
import pdef.types


class NativeFormat(object):
    '''NativeFormat parses/serializes pdef data types from/to native types and collections.'''
    def serialize(self, obj, descriptor):
        if obj is None:
            return None

        type0 = descriptor.type
        serialize = self.serialize
        Type = pdef.types.Type

        if type0 in Type.PRIMITIVES:
            return descriptor.pyclass(obj)

        elif type0 == Type.ENUM:
            return obj.lower()

        elif type0 == Type.LIST:
            elemd = descriptor.element
            return [serialize(elem, elemd) for elem in obj]

        elif type0 == Type.SET:
            elemd = descriptor.element
            return {serialize(elem, elemd) for elem in obj}

        elif type0 == Type.MAP:
            keyd = descriptor.key
            valued = descriptor.value
            return {serialize(k, keyd): serialize(v, valued) for k, v in obj.items()}

        elif type0 == Type.MESSAGE or type0 == Type.EXCEPTION:
            return self._serialize_message(obj)

        elif type0 == Type.VOID:
            return None

        raise ValueError('Unsupported type ' + descriptor)

    def _serialize_message(self, message):
        if message is None:
            return None

        result = OrderedDict()
        serialize = self.serialize
        descriptor = message.DESCRIPTOR  # Support polymorphic messages.

        for field in descriptor.fields:
            value = field.get(message)
            result[field.name] = serialize(value, field.type)

        return result

    def parse(self, obj, descriptor):
        if obj is None:
            return None

        type0 = descriptor.type
        parse = self.parse
        Type = pdef.types.Type

        if type0 in Type.PRIMITIVES:
            return descriptor.pyclass(obj)

        elif type0 == Type.ENUM:
            return descriptor.find_value(obj)

        elif type0 == Type.LIST:
            elemd = descriptor.element
            return [parse(elem, elemd) for elem in obj]

        elif type0 == Type.SET:
            elemd = descriptor.element
            return {parse(elem, elemd) for elem in obj}

        elif type0 == Type.MAP:
            keyd = descriptor.key
            valued = descriptor.value
            return {parse(k, keyd): parse(v, valued) for k, v in obj.items()}

        elif type0 == Type.MESSAGE or type0 == Type.EXCEPTION:
            return self._parse_message(obj, descriptor)

        elif type0 == Type.VOID:
            return None

        raise ValueError('Unsupported type ' + descriptor)

    def _parse_message(self, dict0, descriptor):
        '''Parse a message from a dictionary.'''
        if dict0 is None:
            return None

        parse = self.parse

        if descriptor.is_polymorphic:
            # Parse a discriminator value and find a subtype descriptor.
            discriminator = descriptor.discriminator
            serialized = dict0.get(discriminator.name)
            parsed = parse(serialized, discriminator.type)
            subtype = descriptor.find_subtype(parsed)
            descriptor = subtype.DESCRIPTOR

        message = descriptor.pyclass()
        for field in descriptor.fields:
            serialized = dict0.get(field.name)
            if serialized is None:
                continue

            parsed = parse(serialized, field.type)
            field.set(message, parsed)

        return message


class JsonFormat(object):
    '''JsonFormat parses/serializes pdef types from/to JSON strings.'''
    def __init__(self):
        self.native = NativeFormat()

    def parse(self, s, descriptor):
        '''Parse a pdef data type from a json string.'''
        if s is None:
            return None

        value = _json.loads(s)
        parsed = self.native.parse(value, descriptor)
        return parsed

    def parse_stream(self, fp, descriptor):
        '''Parse an pdef data type as a json string from a file-like object.'''
        value = _json.load(fp)
        parsed = self.native.parse(value, descriptor)
        return parsed

    def serialize(self, obj, descriptor, indent=None, **kwargs):
        '''Serialize a pdef object into a json string.'''
        serialized = self.native.serialize(obj, descriptor)
        s = _json.dumps(serialized, ensure_ascii=False, indent=indent, default=self._default,
                        **kwargs)
        return s

    def serialize_to_stream(self, obj, descriptor, fp, indent=None, **kwargs):
        '''Serialize a pdef object as a json string to a file-like object.'''
        serialized = self.native.serialize(obj, descriptor)
        return _json.dump(serialized, fp, ensure_ascii=False, indent=indent, default=self._default,
                          **kwargs)

    def _default(self, obj):
        if isinstance(obj, set):
            return list(obj)
        raise TypeError('%s is not JSON serializable' % type(obj))


native = NativeFormat()
json = JsonFormat()
