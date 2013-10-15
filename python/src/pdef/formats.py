# encoding: utf-8
import json as _json


class NativeFormat(object):
    def parse(self, obj, descriptor):
        pass

    def _parse_message(self, d):
        '''Parse a message from a dictionary.'''
        if d is None:
            return None

        discriminator = self.discriminator
        if discriminator:
            type0 = discriminator.type.parse_object(d.get(discriminator.name))
            subtype_supplier = self.subtypes.get(type0)
            if subtype_supplier:
                return subtype_supplier().parse_dict(d)

        message = self.pyclass()
        for field in self.fields:
            if field.name not in d:
                continue

            data = d[field.name]
            value = field.type.parse_object(data)
            field.set(message, value)
        return message

    def _serialize_message(self, message):
        '''Serialize a message into a dict.'''
        self.check_type(message)

        if message is None:
            return None

        d = {}
        for field in self.fields:
            value = field.get(message)
            data = field.type.to_object(value)
            if data is None:
                continue

            d[field.name] = data
        return d


class JsonFormat(object):
    def loads(self, s, descriptor):
        if s is None:
            return None

        value = _json.loads(s)
        return native.parse(value, descriptor)

    def dumps(self, object, descriptor, indent=False):
        value = self.to_object(obj)
        return json.dumps(value, indent=indent)


native = NativeFormat()
