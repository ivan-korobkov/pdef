# encoding: utf-8
import logging
import os.path
from pdef.types import Type
from pdef.compiler.translator import AbstractTranslator, mkdir_p


def translate(out, package):
    '''Translates a package into python code.'''
    return PythonTranslator(out).translate(package)


class PythonTranslator(AbstractTranslator):
    def __init__(self, out):
        super(PythonTranslator, self).__init__(out)

        self.module_template = self.read_template('module.template')
        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def translate(self, package):
        pymodules = [PythonModule(module) for module in package.modules.values()]
        root = self._build_directory_tree(pymodules)
        root.write()

    def _build_directory_tree(self, pymodules):
        root = Dir(self.out, None)

        for pymodule in pymodules:
            name = pymodule.name
            directory = root
            for part in name.split('.'):
                directory = root.child(part)
            directory.code = pymodule.render(self)

        return root


class Dir(object):
    def __init__(self, path, name):
        self.path = path
        self.name = name

        self.code = None
        self.dirs = {}

    def child(self, name):
        if name not in self.dirs:
            self.dirs[name] = Dir(os.path.join(self.path, name), name)
        return self.dirs[name]

    def write(self):
        filepath = None
        if self.code:
            filepath = os.path.join(self.path, '__init__.py') if self.dirs else self.path + '.py'

        if self.dirs:
            mkdir_p(self.path)

        if filepath:
            mkdir_p(os.path.dirname(filepath))

            with open(filepath, 'wt') as f:
                f.write(self.code)
                logging.info('Created %s', filepath)

        for d in self.dirs.values():
            d.write()

class PythonModule(object):
    def __init__(self, module):
        self.name = module.name
        self.imports = [import0.module.name for import0 in module.imports.values()]
        self.definitions = [pydef(def0) for def0 in module.definitions.values()]
        self.modules = []

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
        self.subtypes = [(pyref(subtype), pyref(submessage))
                         for subtype, submessage in msg.subtypes.items()]
        self.discriminator_name = msg.discriminator.name if msg.discriminator else None

        self.fields = [PythonField(field) for field in msg.fields.values()]
        self.inherited_fields = [PythonField(field) for field in msg.inherited_fields.values()]
        self.declared_fields = [PythonField(field) for field in msg.declared_fields.values()]

    def render(self, translator):
        return translator.message_template.render(**self.__dict__)


class PythonField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = pyref(field.type)


class PythonInterface(object):
    def __init__(self, iface):
        self.name = iface.name
        self.base = pyref(iface.base) if iface.base else None
        self.methods = [PythonMethod(m) for m in iface.methods.values()]
        self.declared_methods = [PythonMethod(m) for m in iface.declared_methods.values()]
        self.inherited_methods = [PythonMethod(m)for m in iface.inherited_methods.values()]

    def render(self, translator):
        return translator.interface_template.render(**self.__dict__)


class PythonMethod(object):
    def __init__(self, method):
        self.name = method.name
        self.result = pyref(method.result)
        self.args = [(arg.name, pyref(arg.type)) for arg in method.args.values()]


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
