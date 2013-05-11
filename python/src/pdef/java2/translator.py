# encoding: utf-8
from collections import OrderedDict
import logging
import os.path
from jinja2 import Environment
from pdef.common import Type, upper_first, mkdir_p


class JavaTranslator(object):
    def __init__(self, out):
        self.out = out

        self.env = Environment(trim_blocks=True)
        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def write_definition(self, def0):
        '''Writes a java definition to the output directory.'''
        jdef = self.definition(def0)
        code = jdef.code

        dirs = jdef.package.split('.')
        fulldir = os.path.join(self.out, os.path.join(*dirs))
        fullpath = os.path.join(fulldir, jdef.name)

        mkdir_p(fulldir)
        with open(fullpath, 'wt') as f:
            f.write(code)

        logging.info('%s: created %s', self, fullpath)

    def definition(self, def0):
        '''Creates and returns a java definition.'''
        t = def0.type
        if t == Type.ENUM: return self.enum(def0)
        elif t == Type.MESSAGE: return self.message(def0)
        elif t == Type.INTERFACE: return self.interface(def0)
        raise ValueError('Unsupported definition %s' % def0)

    def enum(self, def0):
        return JavaEnum(def0, self.enum_template)

    def message(self, def0):
        return JavaMessage(def0, self.message_template)

    def interface(self, def0):
        return JavaInterface(def0, self.interface_template)

    def read_template(self, name):
        path = os.path.join(os.path.dirname(__file__), name)
        with open(path, 'r') as f:
            text = f.read()
        return self.env.from_string(text)


class JavaDefinition(object):
    def __init__(self, obj, template):
        self.name = obj.name
        self.package = obj.module.name
        self._template = template

    @property
    def code(self):
        return self._template.render(**self.__dict__)


class JavaEnum(JavaDefinition):
    def __init__(self, enum, template):
        super(JavaEnum, self).__init__(enum, template)
        self.values = [val.name for val in enum.values.values()]


class JavaMessage(JavaDefinition):
    def __init__(self, msg, template):
        super(JavaMessage, self).__init__(msg, template)

        self.base = ref(msg.base) if msg.base else 'io.pdef.GeneratedMessage'
        self.base_builder = '%s.Builder' % self.base

        self.declared_fields = [JavaField(f) for f in msg.declared_fields]
        self.inherited_fields = [JavaField(f) for f in msg.inherited_fields]
        self.is_exception = msg.is_exception


class JavaSubtypes(object):
    def __init__(self, subtypes):
        self.type = ref(subtypes.type)
        self.items = tuple((k.name.lower(), ref(v)) for k, v in subtypes.as_map().items())
        self.field = JavaField(subtypes.field)


class JavaField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = ref(field.type)
        self.is_discriminator = field.is_discriminator

        self.get = 'get%s' % upper_first(self.name)
        self.set = 'set%s' % upper_first(self.name)
        self.clear = 'clear%s' % upper_first(self.name)
        self.present = 'has%s' % upper_first(self.name)


class JavaInterface(JavaDefinition):
    def __init__(self, iface, template):
        super(JavaInterface, self).__init__(iface, template)

        self.bases = [ref(base) for base in iface.bases]
        self.declared_methods = [JavaMethod(method) for method in iface.declared_methods.values()]


class JavaMethod(object):
    def __init__(self, method):
        self.name = method.name
        self.args = list((arg.name, ref(arg.type)) for arg in method.args.values())
        self.result = ref(method.result)
        if not self.result.is_interface:
            self.result = 'ListenableFuture<%s>' % self.result.boxed


class JavaRef(object):
    def __init__(self, type, name, boxed=None, default='null', is_primitive=False,
                 is_interface =False):
        self.type = type
        self.name = name
        self.boxed = boxed if boxed else self
        self.default = default
        self.is_primitive = is_primitive
        self.is_interface = is_interface

    def __str__(self):
        return self.name


class JavaTypes(object):
    BOOL = JavaRef(Type.BOOL, 'boolean', 'Boolean', default='false', is_primitive=True)
    INT16 = JavaRef(Type.INT16, 'short', 'Short', default='(short) 0', is_primitive=True)
    INT32 = JavaRef(Type.INT32, 'int', 'Integer', default='0', is_primitive=True)
    INT64 = JavaRef(Type.INT64, 'long', 'Long', default='0L', is_primitive=True)
    FLOAT = JavaRef(Type.FLOAT, 'float', 'Float', default='0f', is_primitive=True)
    DOUBLE = JavaRef(Type.DOUBLE, 'double', 'Double', default='0.0', is_primitive=True)
    STRING = JavaRef(Type.STRING, 'String')
    OBJECT = JavaRef(Type.OBJECT, 'Object')
    VOID = JavaRef(Type.VOID, 'void', 'Void', is_primitive=True)

    _BY_TYPE = None

    @classmethod
    def get_by_type(cls, t):
        if cls._BY_TYPE is None:
            cls._BY_TYPE = {}
            for k, v in cls.__dict__.items():
                if not isinstance(v, JavaRef): continue
                cls._BY_TYPE[v.type] = v

        return cls._BY_TYPE.get(t)


class JavaList(JavaRef):
    def __init__(self, listref):
        super(JavaList, self).__init__(Type.LIST, 'java.lang.List')
        self.element = ref(listref.element)

    def __str__(self):
        return 'java.lang.List<%s>' % self.element.boxed


class JavaSet(JavaRef):
    def __init__(self, setref):
        super(JavaSet, self).__init__(Type.SET, 'java.lang.Set')
        self.element = ref(setref.element)

    def __str__(self):
        return 'java.lang.Set<%s>' % self.element.boxed


class JavaMap(JavaRef):
    def __init__(self, mapref):
        super(JavaMap, self).__init__(Type.MAP, 'java.lang.Map')
        self.key = ref(mapref.key)
        self.value = ref(mapref.value)

    def __str__(self):
        return 'java.lang.Map<%s, %s>' % (self.key.boxed, self.value.boxed)


class JavaEnumValue(JavaRef):
    def __init__(self, value):
        super(JavaEnumValue, self).__init__(Type.ENUM_VALUE, value.name)
        self.enum = ref(value.enum)

    def __str__(self):
        return '%s.%s' % (self.enum, self.name)


class JavaObject(JavaRef):
    def __init__(self, obj):
        super(JavaObject, self).__init__(Type.DEFINITION, obj.name)
        self.name = '%s.%s' % (obj.module.name, obj.name)


def ref(obj):
    t = obj.type
    if JavaTypes.get_by_type(t): return JavaTypes.get_by_type(t)
    elif t == Type.LIST: return JavaList(obj)
    elif t == Type.SET: return JavaSet(obj)
    elif t == Type.MAP: return JavaMap(obj)
    elif t == Type.ENUM_VALUE: return JavaEnumValue(obj)
    return JavaObject(obj)
