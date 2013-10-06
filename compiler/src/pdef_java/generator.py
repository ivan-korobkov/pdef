# encoding: utf-8
import logging
import os.path

from pdef_code import generator
from pdef_code.ast import TypeEnum


class JavaGenerator(generator.Generator):
    def __init__(self, out):
        self.out = out
        self.templates = jtemplates()

    def generate(self, package):
        '''Generate java source code for a pdef package.'''
        for module in package.modules:
            for def0 in module.definitions:
                jdef = JavaDefinition.create(def0)
                jdef.write(self.out, self.templates)


class JavaDefinition(object):
    template_name = None

    @classmethod
    def create(cls, def0):
        if def0.is_enum:
            return JavaEnum(def0)

        elif def0.is_message:
            return JavaMessage(def0)

        elif def0.is_interface:
            return JavaInterface(def0)

        raise ValueError('Unsupported definition %r' % def0)

    def __init__(self, def0):
        self.name = def0.name
        self.type = def0.type
        self.package = def0.module.name
        self.doc = def0.doc

    def render(self, templates):
        template = templates.get(self.template_name)
        return template.render(**self.__dict__)

    def write(self, out, templates):
        # Render this definition into a string.
        code = self.render(templates)

        # Create all module directories.
        dirs = self.name.split('.')
        fulldir = os.path.join(out, os.path.join(*dirs))
        generator.mkdir_p(fulldir)

        # Write a file.
        filename = '%s.java' % self.name
        path = os.path.join(fulldir, filename)
        with open(path, 'wt') as f:
            f.write(code)

        logging.debug('Created %s', path)


class JavaEnum(JavaDefinition):
    template_name = 'enum.template'

    def __init__(self, enum):
        super(JavaEnum, self).__init__(enum)
        self.values = [val.name for val in enum.values]


class JavaMessage(JavaDefinition):
    template_name = 'message.template'

    def __init__(self, msg):
        super(JavaMessage, self).__init__(msg)
        self.base = jreference(msg.base)
        self.discriminator_value = jreference(msg.discriminator_value)
        self.discriminator = JavaField(msg.discriminator) if msg.discriminator else None
        self.subtypes = tuple(jreference(stype) for stype in msg.subtypes)

        self.fields = [JavaField(f) for f in msg.fields]
        self.declared_fields = [JavaField(f) for f in msg.declared_fields]
        self.inherited_fields = [JavaField(f) for f in msg.inherited_fields]

        self.is_exception = msg.is_exception
        self.is_form = msg.is_form

        self.base_or_root = self.base or \
            ('pdef.GeneratedException' if msg.is_exception else 'pdef.GeneratedMessage')


class JavaField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = jreference(field.type)
        self.is_discriminator = field.is_discriminator

        self.get = 'get%s' % generator.upper_first(self.name)
        self.set = 'set%s' % generator.upper_first(self.name)
        self.present = 'has%s' % generator.upper_first(self.name)


class JavaInterface(JavaDefinition):
    template_name = 'interface.template'

    def __init__(self, iface):
        super(JavaInterface, self).__init__(iface)

        self.exc = jreference(iface.exc)
        self.declared_methods = [JavaMethod(method) for method in iface.declared_methods]


class JavaMethod(object):
    def __init__(self, method,):
        self.name = method.name
        self.doc = method.doc
        self.args = [JavaArg(arg, jreference) for arg in method.args]
        self.result = jreference(method.result)

        self.is_post = method.is_post
        self.is_index = method.is_index


class JavaArg(object):
    def __init__(self, arg, reference):
        self.name = arg.name
        self.type = reference(arg.type)


class JavaReference(object):
    @classmethod
    def list(cls, type0):
        element = jreference(type0.element)

        name = 'java.util.List<%s>' % element
        default = 'com.google.common.collect.ImmutableList.<%s>of()' % element
        descriptor = 'Descriptors.list(%s)' % element.descriptor

        return JavaReference(name, default=default, descriptor=descriptor, is_list=True)

    @classmethod
    def set(cls, type0):
        element = jreference(type0.element)

        name = 'java.util.Set<%s>' % element
        default = 'com.google.common.collect.ImmutableSet.<%s>of()' % element
        descriptor = 'Descriptors.set(%s)' % element.descriptor

        return JavaReference(name, default=default, descriptor=descriptor, is_set=True)

    @classmethod
    def map(cls, type0):
        key = jreference(type0.key)
        value = jreference(type0.value)

        name = 'java.util.Map<%s, %s>' % (key, value)
        default = 'com.google.common.collect.ImmutableMap.<%s, %s>of()' % (key, value)
        descriptor = 'Descriptors.map(%s, %s)' % (key.descriptor, value.descriptor)

        return JavaReference(name, default=default, descriptor=descriptor, is_map=True)

    @classmethod
    def enum_value(cls, type0):
        name = '%s.%s' % (jreference(type0.enum), type0.name)
        return JavaReference(name)

    @classmethod
    def definition(cls, type0):
        name = '%s.%s' % (type0.module.name, type0.name)

        if type0.is_interface:
            default = None
            descriptor = '%s.DESCRIPTOR' % name
        else:
            default = '%s.instance()' % name
            descriptor = '%s.descriptor()' % name

        return JavaReference(name, default=default, descriptor=descriptor)

    def __init__(self, name, unboxed=None, default='null', descriptor=None,
                 is_list=False, is_set=False, is_map=False):
        self.name = name
        self.unboxed = unboxed or self
        self.default = default
        self.descriptor = descriptor

        self.is_list = is_list
        self.is_set = is_set
        self.is_map = is_map

    def __str__(self):
        return self.name


NATIVE_TYPES = {
    TypeEnum.BOOL: JavaReference('Boolean', 'boolean', 'false', 'Descriptors.bool'),
    TypeEnum.INT16: JavaReference('Short', 'short', '(short) 0', 'Descriptors.int16'),
    TypeEnum.INT32: JavaReference('Integer', 'int', '0', 'Descriptors.int32'),
    TypeEnum.INT64: JavaReference('Long', 'long', '0L', 'Descriptors.int64'),
    TypeEnum.FLOAT: JavaReference('Float', 'float', '0f', 'Descriptors.float0'),
    TypeEnum.DOUBLE: JavaReference('Double', 'double', '0.0', 'Descriptors.double0'),
    TypeEnum.STRING: JavaReference('String', default='""', descriptor='Descriptors.string'),
    TypeEnum.OBJECT: JavaReference('Object', descriptor='Descriptors.object'),
    TypeEnum.VOID: JavaReference('Void', 'void', descriptor='Descriptors.void0')
}


def jreference(type0):
    '''Create a java reference.'''
    if type0 is None:
        return None

    elif type0.is_native:
        return NATIVE_TYPES[type0.type]

    elif type0.is_list:
        return JavaReference.list(type0)

    elif type0.is_set:
        return JavaReference.set(type0)

    elif type0.is_map:
        return JavaReference.map(type0)

    elif type0.is_enum_value:
        return JavaReference.enum_value(type0)

    return JavaReference.definition(type0)


def jtemplates():
    '''Create java generator templates.'''
    return generator.Templates(__file__)
