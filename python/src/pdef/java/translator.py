# encoding: utf-8
import logging
import os.path

from pdef import Type
from pdef.compiler.translator import AbstractTranslator, upper_first, mkdir_p


class JavaTranslator(AbstractTranslator):
    def __init__(self, out):
        super(JavaTranslator, self).__init__(out)

        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def translate(self, package):
        '''Translate package definitions and write them to files.'''
        for module in package.modules.values():
            for def0 in module.definitions.values():
                jdef = self.definition(def0)
                self.write(jdef.package, '%s.java' % jdef.name, jdef.code)

    def definition(self, def0):
        '''Return a java definition for a pdef definition.'''
        t = def0.type
        if t == Type.ENUM:
            return JavaEnum(def0, self)
        if t == Type.MESSAGE:
            return JavaMessage(def0, self)
        if t == Type.INTERFACE:
            return JavaInterface(def0, self)
        raise ValueError('Unsupported definition %s' % def0)

    def ref(self, obj_or_none):
        '''Return a java reference for a pdef type.'''
        if not obj_or_none:
            return None
        return JavaType.create(obj_or_none, self)

    def field(self, field_or_none):
        '''Return a java field for a pdef field.'''
        if not field_or_none:
            return None
        return JavaField(field_or_none, self)

    def method(self, method_or_none):
        '''Return a java method for a pdef method.'''
        if not method_or_none:
            return None
        return JavaMethod(method_or_none, self)

    def message_base(self, msg):
        return self.ref(msg.base) \
            if msg.base else 'pdef.GeneratedException' \
            if msg.is_exception else 'pdef.GeneratedMessage'

    def write(self, module_name, file_name, code):
        '''Writes a code to a file in a module directory.'''
        dirs = module_name.split('.')
        fulldir = os.path.join(self.out, os.path.join(*dirs))
        mkdir_p(fulldir)

        fullpath = os.path.join(fulldir, file_name)
        with open(fullpath, 'wt') as f:
            f.write(code)

        logging.debug('  Created %s', fullpath)


class JavaDefinition(object):
    def __init__(self, obj, translator):
        self.name = obj.name
        self.type = obj.type
        self.package = obj.module.name
        self.doc = obj.doc

        self.translator = translator
        self.template = None

    @property
    def code(self):
        return self.template.render(**self.__dict__)


class JavaEnum(JavaDefinition):
    def __init__(self, enum, translator):
        super(JavaEnum, self).__init__(enum, translator)
        self.values = [val.name for val in enum.values.values()]
        self.template = translator.enum_template


class JavaMessage(JavaDefinition):
    def __init__(self, msg, translator):
        super(JavaMessage, self).__init__(msg, translator)
        self.template = translator.message_template

        self.base = translator.message_base(msg)
        self.base_type = translator.ref(msg.base_type)
        self.base_discriminator = translator.field(msg.base.discriminator) if msg.base else None;
        self.discriminator = translator.field(msg.discriminator)

        # Keys are simple enum values so that they can be used in the switch statement.
        self.subtypes = tuple((key.name, translator.ref(val)) for key, val in msg.subtypes.items())

        self.fields = [translator.field(f) for f in msg.fields.values()]
        self.declared_fields = [translator.field(f) for f in msg.declared_fields.values()]
        self.inherited_fields = [translator.field(f) for f in msg.inherited_fields.values()]
        self.is_exception = msg.is_exception


class JavaField(object):
    def __init__(self, field, translator):
        self.translator = translator
        self.name = field.name
        self.type = translator.ref(field.type)

        self.get = 'get%s' % upper_first(self.name)
        self.set = 'set%s' % upper_first(self.name)
        self.present = 'has%s' % upper_first(self.name)


class JavaInterface(JavaDefinition):
    def __init__(self, iface, translator):
        super(JavaInterface, self).__init__(iface, translator)
        self.template = translator.interface_template

        self.base = translator.ref(iface.base) if iface.base else None
        self.exc = translator.ref(iface.exc) if iface.exc else None
        self.methods = [translator.method(method) for method in iface.methods.values()]
        self.declared_methods = [translator.method(method)
                                 for method in iface.declared_methods.values()]


class JavaMethod(object):
    def __init__(self, method, translator):
        self.name = method.name
        self.args = [JavaArg(arg, translator) for arg in method.args.values()]
        self.result = translator.ref(method.result)
        self.doc = method.doc


class JavaArg(object):
    def __init__(self, arg, translator):
        self.name = arg.name
        self.is_query = arg.is_query
        self.type = translator.ref(arg.type)


class JavaType(object):
    @classmethod
    def create(cls, obj, translator):
        '''Returns a java reference for a pdef type.'''
        if obj.type in NATIVE_MAP:
            return NATIVE_MAP[obj.type]
        javatype = {
            Type.LIST: JavaType.list,
            Type.SET: JavaType.set,
            Type.MAP: JavaType.map,
            Type.ENUM: JavaType.enum,
            Type.ENUM_VALUE: JavaType.enum_value,
            Type.MESSAGE: JavaType.message,
            Type.INTERFACE: JavaType.interface
        }.get(obj.type)

        if javatype:
            return javatype(obj, translator)
        raise ValueError('Unsupported type %s' % obj)

    @classmethod
    def list(cls, obj, translator):
        element = translator.ref(obj.element)
        name = 'java.util.List<%s>' % element
        default = 'com.google.common.collect.ImmutableList.<%s>of()' % element
        descriptor = 'Descriptors.list(%s)' % element.descriptor
        return JavaType(Type.LIST, name=name, default=default, descriptor=descriptor)

    @classmethod
    def set(cls, obj, translator):
        element = translator.ref(obj.element)
        name = 'java.util.Set<%s>' % element
        default = 'com.google.common.collect.ImmutableSet.<%s>of()' % element
        descriptor = 'Descriptors.set(%s)' % element.descriptor
        return JavaType(Type.SET, name=name, default=default, descriptor=descriptor)

    @classmethod
    def map(cls, obj, translator):
        key = translator.ref(obj.key)
        value = translator.ref(obj.value)
        name = 'java.util.Map<%s, %s>' % (key, value)
        default = 'com.google.common.collect.ImmutableMap.<%s, %s>of()' % (key, value)
        descriptor = 'Descriptors.map(%s, %s)' % (key.descriptor, value.descriptor)
        return JavaType(Type.MAP, name=name, default=default, descriptor=descriptor)

    @classmethod
    def enum_value(cls, obj, translator):
        name = '%s.%s' % (translator.ref(obj.enum), obj.name)
        return JavaType(Type.ENUM_VALUE, name=name)

    @classmethod
    def enum(cls, obj, translator):
        name = cls._default_name(obj)
        default = '%s.instance()' % name
        descriptor = '%s.descriptor()' % name
        return JavaType(Type.ENUM, name=name, default=default, descriptor=descriptor)

    @classmethod
    def message(cls, obj, translator):
        name = cls._default_name(obj)
        default = '%s.instance()' % name
        descriptor = '%s.descriptor()' % name
        return JavaType(Type.MESSAGE, name, default=default, descriptor=descriptor)

    @classmethod
    def interface(cls, obj, translator):
        name = cls._default_name(obj)
        descriptor = '%s.DESCRIPTOR' % name
        return JavaType(Type.INTERFACE, name, descriptor=descriptor)

    @classmethod
    def _default_name(cls, obj):
        return '%s.%s' % (obj.module.name, obj.name) if obj.module else obj.name

    def __init__(self, type, name, unboxed=None, default='null', is_primitive=False, descriptor=None):
        self.type = type
        self.name = name
        self.unboxed = unboxed if unboxed else self
        self.default = default
        self.descriptor = descriptor

        self.is_primitive = is_primitive
        self.is_nullable = default == 'null'

        self.is_interface = type == Type.INTERFACE
        self.is_list = type == Type.LIST
        self.is_set = type == Type.SET
        self.is_map = type == Type.MAP

    def __str__(self):
        return self.name

    @property
    def parse(self):
        if self.type == Type.MESSAGE or self.type == Type.ENUM:
            return '%s.parse' % self
        return '%s.parse' % self.descriptor

    @property
    def serialize(self):
        if self.type == Type.MESSAGE or self.type == Type.ENUM:
            return '%s.serialize' % self
        return '%s.serialize' % self.descriptor


NATIVE_MAP = {
    Type.BOOL: JavaType(Type.BOOL, 'Boolean', 'boolean', default='false', is_primitive=True,
                        descriptor='Descriptors.bool'),

    Type.INT16: JavaType(Type.INT16, 'Short', 'short', default='(short) 0', is_primitive=True,
                         descriptor='Descriptors.int16'),

    Type.INT32: JavaType(Type.INT32, 'Integer', 'int', default='0', is_primitive=True,
                         descriptor='Descriptors.int32'),

    Type.INT64: JavaType(Type.INT64, 'Long', 'long', default='0L', is_primitive=True,
                         descriptor='Descriptors.int64'),

    Type.FLOAT: JavaType(Type.FLOAT, 'Float', 'float', default='0f', is_primitive=True,
                         descriptor='Descriptors.float0'),

    Type.DOUBLE: JavaType(Type.DOUBLE, 'Double', 'double', default='0.0', is_primitive=True,
                          descriptor='Descriptors.double0'),

    Type.STRING: JavaType(Type.STRING, 'String', descriptor='Descriptors.string'),

    Type.OBJECT: JavaType(Type.OBJECT, 'Object', descriptor='Descriptors.object'),

    Type.VOID: JavaType(Type.VOID, 'Void', 'void', is_primitive=True,
                        descriptor='Descriptors.void0')
}
