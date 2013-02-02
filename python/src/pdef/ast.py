# encoding: utf-8


class Node(object):
    def to_raw(self):
        '''Only for debug'''
        return _to_raw(self)


class Package(Node):
    def __init__(self, name, imports=None, definitions=None):
        self.name = name
        self.imports = _default_tuple(imports)
        self.definitions = _default_tuple(definitions)


class Import(Node):
    def __init__(self, name, alias=None):
        self.name = name
        self.alias = alias


class Type(Node):
    def __init__(self, name, args=None):
        self.name = name
        self.args = _default_tuple(args)

    def __repr__(self):
        return '<Type "%s" %s>' % (self.name, ', '.join(str(arg) for arg in self.args))


class Native(Node):
    def __init__(self, name, variables=None, options_kv_tuple=None):
        self.name = name
        self.variables = tuple(variables) if variables else ()
        self.options = dict(options_kv_tuple) if options_kv_tuple else {}


class Enum(Node):
    def __init__(self, name, values=None):
        self.name = name
        self.values = _default_tuple(values)


class Message(Node):
    def __init__(self, name, variables=None, base=None, fields=None, options_kv_tuple=None):
        self.name = name
        self.variables = _default_tuple(variables)
        self.base = base
        self.fields = _default_tuple(fields)
        self.options = dict(options_kv_tuple) if options_kv_tuple else {}


class Field(Node):
    def __init__(self, name, type, options_kv_tuple=None):
        self.name = name
        self.type = type
        self.options = dict(options_kv_tuple) if options_kv_tuple else {}


def _default_tuple(iterable):
    '''Returns a tuple from an iterable or an empty tuple.'''
    return tuple(iterable) if iterable else ()


def _to_raw(o):
    '''Converts an object to its raw presentation.'''
    if isinstance(o, basestring):
        return o

    elif isinstance(o, Node):
        d = {}
        for k, v in o.__dict__.items():
            d[k] = _to_raw(v)
        return d

    try:
        iterator = iter(o)
    except TypeError:
        iterator = None

    if not iterator:
        return o

    s = []
    for item in iterator:
        r = _to_raw(item)
        s.append(r)

    return s
