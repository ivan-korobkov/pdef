# encoding: utf-8
'''Internal json serialization/deserialization which supports python sets.'''
import json
import pdef


def dumps(obj, indent=2):
    '''Serialize a python object into a json string.'''
    return json.dumps(obj, indent=indent, default=_default)


def loads(s):
    '''Deserialize a string into a python object.'''
    return json.loads(s)


def _default(obj):
    if isinstance(obj, set):
        return list(obj)
    elif isinstance(obj, pdef.Message):
        return obj.to_dict()
    raise TypeError('%s is not JSON serializable' % obj)
