# encoding: utf-8
import logging
import os.path

from pdef_compiler.lang import Type
from pdef_compiler.translator import AbstractTranslator, NameMapper, mkdir_p


def translate(out, package, module_name_map=None):
    '''Translates a package into python code.'''
    return PythonTranslator(out, module_name_map=module_name_map).translate(package)


class PythonTranslator(AbstractTranslator):
    def __init__(self, out, module_name_map=None):
        super(PythonTranslator, self).__init__(out)
        self.mapper = NameMapper(module_name_map)

        self.module_template = self.read_template('module.template')
        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def translate(self, package):
        pymodules = self._convert_package(package)
        tree = self._build_tree(pymodules)
        tree.write(self)

    def _convert_package(self, package):
        return [PythonModule(module, self.mapper) for module in package.modules]

    def _build_tree(self, pymodules):
        root = DirectoryOrFile(self.out, is_root=True)

        for pm in pymodules:
            node = root
            for name in pm.name.split('.'):
                node = node.child(name)
            node.module = pm

        return root

    def _write(self, pymodule):
        relpath = pymodule.name.replace('.', os.path.sep) + '.py'
        filepath = os.path.join(self.out, relpath)
        dirpath = os.path.dirname(filepath)

        mkdir_p(dirpath)
        with open(filepath, 'wt') as f:
            f.write(pymodule.render(self))
            logging.info('Created %s', relpath)


class DirectoryOrFile(object):
    def __init__(self, name, parent=None, is_root=False):
        self.name = name
        self.parent = parent
        self.children = {}
        self.is_directory = False
        self.is_root = is_root

        self.module = None

    def child(self, name):
        if name not in self.children:
            node = DirectoryOrFile(name, parent=self)
            self.children[name] = node
            self.is_directory = True

        return self.children[name]

    @property
    def dirpath(self):
        if self.is_directory:
            return os.path.join(self.parent.dirpath, self.name) if self.parent else self.name
        return self.parent.dirpath if self.parent else ''

    @property
    def filepath(self):
        if self.is_directory:
            return os.path.join(self.dirpath, '__init__.py')
        return os.path.join(self.dirpath, self.name + '.py')

    def write(self, translator):
        mkdir_p(self.dirpath)

        if not self.is_root:
            self._write_file(translator)

        for child in self.children.values():
            child.write(translator)

    def _write_file(self, translator):
        if self.module:
            code = self.module.render(translator)
        elif self.is_directory:
            code = '# encoding: utf-8\n'
        else:
            raise AssertionError

        filepath = self.filepath
        with open(filepath, 'wt') as f:
            f.write(code)
            logging.info('Created %s', filepath)


class PythonModule(object):
    def __init__(self, module, mapper=None, generator=None):
        # Create a module local reference lookup, which correctly handles
        # when definitions are referenced inside declaring modules.
        ref = lambda def0: pyref(def0, module, mapper)

        self.name = mapper(module.name) if mapper else module.name
        self.imports = [pyimport(import0, mapper) for import0 in module.imports]
        self.definitions = [pydef(def0, ref) for def0 in module.definitions]
        self.generator = generator

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
        self.values = [value.name for value in def0.values]

    def render(self, translator):
        return translator.enum_template.render(**self.__dict__)


class PythonMessage(object):
    def __init__(self, msg, ref):
        self.name = msg.name
        self.is_exception = msg.is_exception

        self.base = ref(msg.base) if msg.base else None
        self.subtypes = [(ref(stype.discriminator_value), ref(stype)) for stype in msg.subtypes]
        self.discriminator_value = ref(msg.discriminator_value) if msg.discriminator_value else None
        self.discriminator = PythonField(msg.discriminator, ref) if msg.discriminator else None

        self.is_form = msg.is_form

        self.fields = [PythonField(field, ref) for field in msg.fields]
        self.inherited_fields = [PythonField(field, ref) for field in msg.inherited_fields]
        self.declared_fields = [PythonField(field, ref) for field in msg.declared_fields]

        self.root_or_base = self.base if self.base else \
            'pdef.Exc' if self.is_exception else 'pdef.Message'

    def render(self, translator):
        return translator.message_template.render(**self.__dict__)


class PythonField(object):
    def __init__(self, field, ref):
        self.name = field.name
        self.type = ref(field.type)
        self.is_discriminator = field.is_discriminator


class PythonInterface(object):
    def __init__(self, iface, ref):
        self.name = iface.name
        self.base = ref(iface.base) if iface.base else None
        self.exc = ref(iface.exc) if iface.exc else None
        self.methods = [PythonMethod(m, ref) for m in iface.methods]
        self.declared_methods = [PythonMethod(m, ref) for m in iface.declared_methods]
        self.inherited_methods = [PythonMethod(m, ref)for m in iface.inherited_methods]

        self.root_or_base = self.base if self.base else 'pdef.Interface'

    def render(self, translator):
        return translator.interface_template.render(**self.__dict__)


class PythonMethod(object):
    def __init__(self, method, ref):
        self.name = method.name
        self.result = ref(method.result)
        self.args = [PythonArg(arg, ref) for arg in method.args]
        self.is_index = method.is_index
        self.is_post = method.is_post


class PythonArg(object):
    def __init__(self, arg, ref):
        self.name = arg.name
        self.type = ref(arg.type)
        self.is_query = arg.is_query


class PythonRef(object):
    def __init__(self, name, descriptor):
        self.name = name
        self.descriptor = descriptor

    def __str__(self):
        return str(self.name)


def pydef(def0, ref):
    '''Create a python definition.'''
    if def0.is_message or def0.is_exception:
        return PythonMessage(def0, ref)
    elif def0.is_enum:
        return PythonEnum(def0)
    elif def0.is_interface:
        return PythonInterface(def0, ref)
    raise ValueError('Unsupported definition %s' % def0)


def pyimport(import0, mapper=None):
    '''Create a python import string.'''
    if not mapper:
        return import0.module.name
    return mapper(import0.module.name)


def pyref(def0, module=None, mapper=None):
    '''Create a python reference.

    @param def0:    pdef definition.
    @param module:  pdef module in which the definition is referenced.
    @param mapper:  optional module name mapper.
    '''
    type0 = def0.type
    if type0 in NATIVE:
        return NATIVE[type0]

    if def0.is_list:
        element = pyref(def0.element, module, mapper)
        descriptor = 'descriptors.list0(%s)' % element.descriptor
        return PythonRef('list', descriptor)

    elif def0.is_set:
        element = pyref(def0.element, module, mapper)
        descriptor = 'descriptors.set0(%s)' % element.descriptor
        return PythonRef('set', descriptor)

    elif def0.is_map:
        key = pyref(def0.key, module, mapper)
        value = pyref(def0.value, module, mapper)
        descriptor = 'descriptors.map0(%s, %s)' % (key.descriptor, value.descriptor)
        return PythonRef('dict', descriptor)

    elif def0.is_enum_value:
        enum = pyref(def0.enum, module, mapper)
        name = '%s.%s' % (enum.name, def0.name)
        return PythonRef(name, None)

    if def0.module == module:
        # This definition is references from its own module.
        name = def0.name
    else:
        module_name = def0.module.name
        if mapper:
            module_name = mapper(module_name)

        name = '%s.%s' % (module_name, def0.name)

    descriptor = '%s.__descriptor__' % name
    return PythonRef(name, descriptor)


NATIVE = {
    Type.BOOL: PythonRef('bool', 'descriptors.bool0'),
    Type.INT16: PythonRef('int', 'descriptors.int16'),
    Type.INT32: PythonRef('int', 'descriptors.int32'),
    Type.INT64: PythonRef('int', 'descriptors.int64'),
    Type.FLOAT: PythonRef('float', 'descriptors.float0'),
    Type.DOUBLE: PythonRef('float', 'descriptors.double0'),
    Type.STRING: PythonRef('unicode', 'descriptors.string'),
    Type.OBJECT: PythonRef('object', 'descriptors.object0'),
    Type.VOID: PythonRef('object', 'descriptors.void'),
}
