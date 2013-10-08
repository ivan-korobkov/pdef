# encoding: utf-8
import logging
import os.path

from pdefc import generators
from pdefc.ast import TypeEnum


def generate(package, out, namespaces=None, **kwargs):
    '''Java source code generator'''
    return JavaGenerator(out, namespaces=namespaces).generate(package)


class JavaGenerator(generators.Generator):
    def __init__(self, out, namespaces=None):
        self.out = out
        self.namespace = jnamespace(namespaces)
        self.templates = jtemplates()

    def generate(self, package):
        '''Generate java source code for a pdef package.'''
        ref = lambda type0: jreference(type0, self.namespace)

        jdefs = []
        for module in package.modules:
            for def0 in module.definitions:
                jdef = JavaDefinition.create(def0, ref)
                jdefs.append(jdef)

        for jdef in jdefs:
            jdef.write(self.out, self.templates)


class JavaDefinition(object):
    template_name = None

    @classmethod
    def create(cls, def0, ref):
        if def0.is_enum:
            return JavaEnum(def0, ref)

        elif def0.is_message:
            return JavaMessage(def0, ref)

        elif def0.is_interface:
            return JavaInterface(def0, ref)

        raise ValueError('Unsupported definition %r' % def0)

    def __init__(self, def0, ref):
        self.name = def0.name
        self.package = ref(def0.module.name) if ref else def0.module.name
        self.doc = def0.doc

    def render(self, templates):
        template = templates.get(self.template_name)
        return template.render(**self.__dict__)

    def write(self, out, templates):
        # Render this definition into a string.
        code = self.render(templates)

        # Create all module directories.
        dirpath = self.dirpath(out)
        generators.mkdir_p(dirpath)

        # Write a file.
        filepath = self.filepath(out)
        with open(filepath, 'wt') as f:
            f.write(code)

        logging.debug('Created %s', filepath)

    def dirpath(self, out):
        dirs = self.package.split('.')
        return os.path.join(out, os.path.join(*dirs))

    def filepath(self, out):
        dirpath = self.dirpath(out)
        filename = '%s.java' % self.name
        return os.path.join(dirpath, filename)


class JavaEnum(JavaDefinition):
    template_name = 'enum.template'

    def __init__(self, enum, ref):
        super(JavaEnum, self).__init__(enum, ref)
        self.values = [val.name for val in enum.values]


class JavaMessage(JavaDefinition):
    template_name = 'message.template'

    def __init__(self, msg, ref):
        super(JavaMessage, self).__init__(msg, ref)
        self.base = ref(msg.base)
        self.discriminator_value = ref(msg.discriminator_value)
        self.discriminator = JavaField(msg.discriminator, ref) if msg.discriminator else None
        self.subtypes = tuple(ref(stype) for stype in msg.subtypes)

        self.fields = [JavaField(f, ref) for f in msg.fields]
        self.declared_fields = [JavaField(f, ref) for f in msg.declared_fields]
        self.inherited_fields = [JavaField(f, ref) for f in msg.inherited_fields]

        self.is_exception = msg.is_exception
        self.is_form = msg.is_form

        self.base_or_root = self.base or \
                            ('io.pdef.GeneratedException' if msg.is_exception else 'io.pdef.GeneratedMessage')


class JavaField(object):
    def __init__(self, field, ref):
        self.name = field.name
        self.type = ref(field.type)
        self.is_discriminator = field.is_discriminator

        self.get = 'get%s' % generators.upper_first(self.name)
        self.set = 'set%s' % generators.upper_first(self.name)
        self.present = 'has%s' % generators.upper_first(self.name)


class JavaInterface(JavaDefinition):
    template_name = 'interface.template'

    def __init__(self, iface, ref):
        super(JavaInterface, self).__init__(iface, ref)

        self.exc = ref(iface.exc)
        self.declared_methods = [JavaMethod(method, ref) for method in iface.declared_methods]


class JavaMethod(object):
    def __init__(self, method, ref):
        self.name = method.name
        self.doc = method.doc
        self.args = [JavaArg(arg, ref) for arg in method.args]
        self.result = ref(method.result)

        self.is_post = method.is_post
        self.is_index = method.is_index


class JavaArg(object):
    def __init__(self, arg, ref):
        self.name = arg.name
        self.type = ref(arg.type)


class JavaReference(object):
    @classmethod
    def list(cls, type0, ref):
        element = ref(type0.element)

        name = 'java.util.List<%s>' % element
        default = 'com.google.common.collect.ImmutableList.<%s>of()' % element
        descriptor = 'Descriptors.list(%s)' % element.descriptor

        return JavaReference(name, default=default, descriptor=descriptor, is_list=True)

    @classmethod
    def set(cls, type0, ref):
        element = ref(type0.element)

        name = 'java.util.Set<%s>' % element
        default = 'com.google.common.collect.ImmutableSet.<%s>of()' % element
        descriptor = 'Descriptors.set(%s)' % element.descriptor

        return JavaReference(name, default=default, descriptor=descriptor, is_set=True)

    @classmethod
    def map(cls, type0, ref):
        key = ref(type0.key)
        value = ref(type0.value)

        name = 'java.util.Map<%s, %s>' % (key, value)
        default = 'com.google.common.collect.ImmutableMap.<%s, %s>of()' % (key, value)
        descriptor = 'Descriptors.map(%s, %s)' % (key.descriptor, value.descriptor)

        return JavaReference(name, default=default, descriptor=descriptor, is_map=True)

    @classmethod
    def enum_value(cls, type0, ref):
        name = '%s.%s' % (ref(type0.enum), type0.name)
        return JavaReference(name)

    @classmethod
    def definition(cls, type0, ref):
        name = '%s.%s' % (type0.module.name, type0.name)
        name = ref(name)

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
    TypeEnum.VOID: JavaReference('void', 'void', descriptor='Descriptors.void0')
}


def jreference(type0, namespace=None):
    '''Create a java reference.'''
    ref = lambda type0: jreference(type0, namespace)

    if type0 is None:
        return None

    elif isinstance(type0, basestring):
        return namespace(type0) if namespace else type0

    elif type0.is_native:
        return NATIVE_TYPES[type0.type]

    elif type0.is_list:
        return JavaReference.list(type0, ref)

    elif type0.is_set:
        return JavaReference.set(type0, ref)

    elif type0.is_map:
        return JavaReference.map(type0, ref)

    elif type0.is_enum_value:
        return JavaReference.enum_value(type0, ref)

    return JavaReference.definition(type0, ref)


def jtemplates():
    '''Create java generator templates.'''
    return generators.Templates(__file__)


def jnamespace(namespaces=None):
    '''Create java namespaces.'''
    return generators.Namespace(namespaces)

