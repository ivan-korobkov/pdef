# encoding: utf-8
from collections import OrderedDict
from pdef.types import Type
from pdef.compiler.translator import AbstractTranslator


class PythonTranslator(AbstractTranslator):
    def __init__(self, out):
        super(PythonTranslator, self).__init__(out)

        self.module_template = self.read_template('module.template')
        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def translate(self, package):
        for module in package.modules.values():
            self.translate_module(module)

    def translate_module(self, module):
        pymodule = PythonModule(module)
        code = pymodule.render(self)
        print code


class PythonModule(object):
    def __init__(self, module):
        self.name = module.name
        self.imports = [import0.module.name for import0 in module.imports.values()]
        self.definitions = [pydef(def0) for def0 in module.definitions.values()]

    def render(self, translator):
        defs = []
        for def0 in self.definitions:
            code = def0.render(translator)
            defs.append(code)

        return translator.module_template.render(
            name=self.name,
            imports=list(self.imports),
            definitions=defs)


class PythonEnum(object):
    def __init__(self, def0):
        self.name = def0.name
        self.values = [value.name for value in def0.values.values()]

    def render(self, translator):
        return translator.enum_template.render(**self.__dict__)


class PythonMessage(object):
    def __init__(self, msg):
        self.name = msg.name
        self.is_exception = msg.is_exception

        self.base = pyref(msg.base) if msg.base else None
        self.base_type = pyref(msg.base_type) if msg.base_type else None
        self.discriminator_name = msg.discriminator.name if msg.discriminator else None

        self.fields = [PythonField(field) for field in msg.fields.values()]

    def render(self, translator):
        return translator.message_template.render(**self.__dict__)


class PythonField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = pyref(field.type)


class PythonInterface(object):
    def __init__(self, interface):
        self.name = interface.name
        self.base = pyref(interface.base) if interface.base else None
        self.methods = [PythonMethod(method) for method in interface.methods.values()]

    def render(self, translator):
        return translator.interface_template.render(**self.__dict__)


class PythonMethod(object):
    def __init__(self, method):
        self.name = method.name
        self.result = pyref(method.result)
        self.args = OrderedDict((arg.name, pyref(arg.type)) for arg in method.args.values())


class PythonRef(object):
    def __init__(self, name, descriptor):
        self.name = name
        self.descriptor = descriptor


def pydef(def0):
    '''Create a python definition.'''
    if def0.is_message or def0.is_exception:
        return PythonMessage(def0)
    elif def0.is_enum:
        return PythonEnum(def0)
    elif def0.is_interface:
        return PythonInterface(def0)
    raise ValueError('Unsupported definition %s' % def0)


def pyref(def0):
    '''Create a python reference.'''
    type0 = def0.type
    if type0 in NATIVE:
        return NATIVE[type0]

    if def0.is_list:
        descriptor = 'pdef.descriptors.list(%s)' % pyref(def0.element).descriptor
        return PythonRef('list', descriptor)

    elif def0.is_set:
        descriptor = 'pdef.descriptors.set(%s)' % pyref(def0.element).descriptor
        return PythonRef('set', descriptor)

    elif def0.is_map:
        key = pyref(def0.key)
        value = pyref(def0.value)
        descriptor = 'pdef.descriptors.map(%s, %s)' % (key.descriptor, value.descriptor)
        return PythonRef('dict', descriptor)

    elif def0.is_enum_value:
        enum = pyref(def0.enum)
        name = '%s.%s' % (enum.name, def0.name)
        return PythonRef(name, None)

    name = '%s.%s' % (def0.module.name, def0.name)
    descriptor = '%s.__descriptor__' % name
    return PythonRef(name, descriptor)


NATIVE = {
    Type.BOOL: PythonRef('bool', 'pdef.descriptors.bool0'),
    Type.INT16: PythonRef('int', 'pdef.descriptors.int16'),
    Type.INT32: PythonRef('int', 'pdef.descriptors.int32'),
    Type.INT64: PythonRef('int', 'pdef.descriptors.int64'),
    Type.FLOAT: PythonRef('float', 'pdef.descriptors.float0'),
    Type.DOUBLE: PythonRef('float', 'pdef.descriptors.double0'),
    Type.STRING: PythonRef('unicode', 'pdef.descriptors.string'),
    Type.OBJECT: PythonRef('object', 'pdef.descriptors.object0'),
    Type.VOID: PythonRef('object', 'pdef.descriptors.void'),
}
