# encoding: utf-8
'''Internal json serialization/deserialization which supports python sets.'''
import json


def dumps(obj, indent=None):
    '''Serialize a python object into a json string.'''
    return json.dumps(obj, indent=indent, default=_default)



def loads(s):
    '''Deserialize a string into a python object.'''
    return json.loads(s)


def _default(obj):
    if isinstance(obj, set):
        return list(obj)
    raise TypeError(obj + ' is not JSON serializable')
