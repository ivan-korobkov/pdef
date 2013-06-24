# encoding: utf-8
import logging
import os.path
from jinja2 import Environment
from pdef.common import Type, upper_first, mkdir_p


class JavaTranslator(object):
    def __init__(self, out, async=True):
        self.out = out
        self.async = async

        self.env = Environment(trim_blocks=True)
        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def __str__(self):
        return 'JavaTranslator'

    def write_definition(self, def0):
        '''Writes a definition to the output directory.'''
        jdef = self.definition(def0)
        self._write_file(jdef.package, jdef.name, jdef.code)

        if self.async and jdef.type == Type.INTERFACE:
            self._write_file(jdef.package, jdef.async_name, jdef.async_code)

    def _write_file(self, package_name, def_name, code):
        dirs = package_name.split('.')
        fulldir = os.path.join(self.out, os.path.join(*dirs))
        mkdir_p(fulldir)

        fullpath = os.path.join(fulldir, '%s.java' % def_name)
        with open(fullpath, 'wt') as f:
            f.write(code)

        logging.info('%s: created %s', self, fullpath)

    def definition(self, def0):
        '''Creates and returns a java definition.'''
        t = def0.type
        if t == Type.ENUM: return JavaEnum(def0, self.enum_template)
        elif t == Type.MESSAGE: return JavaMessage(def0, self.message_template)
        elif t == Type.INTERFACE: return JavaInterface(def0, self.interface_template)
        raise ValueError('Unsupported definition %s' % def0)

    def read_template(self, name):
        path = os.path.join(os.path.dirname(__file__), name)
        with open(path, 'r') as f:
            text = f.read()
        return self.env.from_string(text)


class JavaDefinition(object):
    def __init__(self, obj, template):
        self.name = obj.name
        self.type = obj.type
        self.package = obj.module.name
        self.doc = obj.doc
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

        self.base = ref(msg.base) if msg.base else \
                'io.pdef.GeneratedException' if msg.is_exception else 'io.pdef.GeneratedMessage'
        self.base_type = ref(msg.base_type) if msg.base_type else None
        self.base_builder = '%s.Builder' % self.base
        self.discriminator_field = JavaField(msg.polymorphic_discriminator_field) \
            if msg.polymorphic_discriminator_field else None
        self.subtypes = tuple((key.name.lower(), ref(val)) for key, val in msg.subtypes.items())

        self.fields = [JavaField(f) for f in msg.fields.values()]
        self.declared_fields = [JavaField(f) for f in msg.declared_fields.values()]
        self.inherited_fields = [JavaField(f) for f in msg.inherited_fields.values()]
        self.is_exception = msg.is_exception


class JavaField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = ref(field.type)

        self.get = 'get%s' % upper_first(self.name)
        self.set = 'set%s' % upper_first(self.name)
        self.clear = 'clear%s' % upper_first(self.name)
        self.present = 'has%s' % upper_first(self.name)


class JavaInterface(JavaDefinition):
    def __init__(self, iface, template):
        super(JavaInterface, self).__init__(iface, template)

        self.bases = [ref(base) for base in iface.bases]
        self.declared_methods = [JavaMethod(method) for method in iface.declared_methods.values()]
        self.async = False
        self.async_name = 'Async%s' % self.name

    @property
    def async_code(self):
        try:
            self.async = True
            return self.code
        finally:
            self.async = False


class JavaMethod(object):
    def __init__(self, method):
        self.name = method.name
        self.args = list((arg.name, ref(arg.type)) for arg in method.args.values())
        self.result = ref(method.result)
        self.doc = method.doc


def ref(obj):
    '''Returns a java reference for a pdef type.'''
    t = obj.type
    if t in NATIVE_MAP: return NATIVE_MAP[t]
    if t == Type.LIST: return JavaType.list(obj)
    if t == Type.SET: return JavaType.set(obj)
    if t == Type.MAP: return JavaType.map(obj)
    if t == Type.ENUM: return JavaType.enum(obj)
    if t == Type.ENUM_VALUE: return JavaType.enum_value(obj)
    if t == Type.MESSAGE: return JavaType.message(obj)
    if t == Type.INTERFACE: return JavaType.interface(obj)
    raise ValueError('Unsupported type %s' % obj)


class JavaType(object):
    @classmethod
    def list(cls, obj):
        element = ref(obj.element).boxed
        return JavaType(Type.LIST, name='java.util.List<%s>' % element,
            default='com.google.common.collect.ImmutableList.<%s>of()' % element,
            descriptor='io.pdef.Descriptors.list(%s)' % element.descriptor)

    @classmethod
    def set(cls, obj):
        element = ref(obj.element).boxed
        name = 'java.util.Set<%s>' % element
        default = 'com.google.common.collect.ImmutableSet.<%s>of()' % element
        descriptor = 'io.pdef.Descriptors.set(%s)' % element.descriptor
        return JavaType(Type.SET, name=name, default=default, descriptor=descriptor)

    @classmethod
    def map(cls, obj):
        key = ref(obj.key).boxed
        value = ref(obj.value).boxed
        name = 'java.util.Map<%s, %s>' % (key, value)
        default = 'com.google.common.collect.ImmutableMap.<%s, %s>of()' % (key, value)
        descriptor = 'io.pdef.Descriptors.map(%s, %s)' % (key.descriptor, value.descriptor)
        return JavaType(Type.MAP, name=name, default=default, descriptor=descriptor)

    @classmethod
    def enum_value(cls, obj):
        return JavaType(Type.ENUM_VALUE, name='%s.%s' % (ref(obj.enum), obj.name))

    @classmethod
    def enum(cls, obj):
        name = cls._default_name(obj)
        default = '%s.instance' % name
        descriptor = '%s.descriptor' % name
        return JavaType(Type.ENUM, name=name, default=default, descriptor=descriptor)

    @classmethod
    def message(cls, obj):
        name = cls._default_name(obj)
        default = '%s.instance' % name
        descriptor = '%s.descriptor' % name
        return JavaType(Type.MESSAGE, name, default=default, descriptor=descriptor)

    @classmethod
    def interface(cls, obj):
        name = cls._default_name(obj)
        descriptor = '%s.descriptor' % name
        async_name = '%s.Async%s' % (obj.module.name, obj.name) \
            if obj.module else 'Async%s' % obj.name
        return JavaType(Type.INTERFACE, name, descriptor=descriptor, async_name=async_name)

    @classmethod
    def _default_name(cls, obj):
        return '%s.%s' % (obj.module.name, obj.name) if obj.module else obj.name

    def __init__(self, type, name, boxed=None, default='null', is_primitive=False,
                 descriptor=None, async_name=None):
        self.type = type
        self.name = name
        self.boxed = boxed if boxed else self
        self.default = default
        self.async_name = async_name
        self.descriptor = descriptor

        self.is_primitive = is_primitive
        self.is_nullable = default == 'null'

        self.is_interface = type == Type.INTERFACE
        self.is_list = type == Type.LIST
        self.is_set = type == Type.SET
        self.is_map = type == Type.MAP

    def __str__(self):
        return self.name


NATIVE_MAP = {
    Type.BOOL: JavaType(Type.BOOL, 'boolean', 'Boolean', default='false', is_primitive=True,
            descriptor='io.pdef.Descriptors.bool'),

    Type.INT16: JavaType(Type.INT16, 'short', 'Short', default='(short) 0', is_primitive=True,
            descriptor='io.pdef.Descriptors.int16'),

    Type.INT32: JavaType(Type.INT32, 'int', 'Integer', default='0', is_primitive=True,
            descriptor='io.pdef.Descriptors.int32'),

    Type.INT64: JavaType(Type.INT64, 'long', 'Long', default='0L', is_primitive=True,
            descriptor='io.pdef.Descriptors.int64'),

    Type.FLOAT: JavaType(Type.FLOAT, 'float', 'Float', default='0f', is_primitive=True,
            descriptor='io.pdef.Descriptors.float0'),

    Type.DOUBLE: JavaType(Type.DOUBLE, 'double', 'Double', default='0.0', is_primitive=True,
            descriptor='io.pdef.Descriptors.double0'),

    Type.STRING: JavaType(Type.STRING, 'String', descriptor='io.pdef.Descriptors.string'),

    Type.OBJECT: JavaType(Type.OBJECT, 'Object', descriptor='io.pdef.Descriptors.object'),

    Type.VOID: JavaType(Type.VOID, 'void', 'Void', is_primitive=True,
            descriptor='io.pdef.Descriptors.void')
}
