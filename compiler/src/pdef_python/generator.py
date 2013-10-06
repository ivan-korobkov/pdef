# encoding: utf-8
import logging
import os.path

from pdef_code import generator
from pdef_code.ast import TypeEnum


class PythonGenerator(generator.Generator):
    '''Python source code generator.'''
    def __init__(self, out, module_name_map=None):
        self.out = out
        self.mapper = generator.NameMapper(module_name_map)
        self.templates = pytemplates()

    def generate(self, package):
        pymodules = [PythonModule(module, self.mapper) for module in package.modules]
        tree = DirectoryOrFile.from_modules(self.out, pymodules)
        tree.write(self.templates)


class PythonModule(object):
    '''Python module.'''
    template_name = 'module.template'

    def __init__(self, module, mapper=None):
        # Create a local module scope, which correctly handles
        # when definitions are referenced inside the declaring module.
        scope = lambda type0: pyreference(type0, module, mapper)

        self.name = mapper(module.name) if mapper else module.name
        self.doc = pydoc(module.doc)

        self.imports = [pyimport(im, mapper) for im in module.imported_modules]
        self.definitions = [PythonDefinition.create(def0, scope) for def0 in module.definitions]

    def render(self, templates):
        '''Render a module and return a string.'''
        defs = []
        for def0 in self.definitions:
            code = def0.render(templates)
            defs.append(code)

        template = templates.get(self.template_name)
        return template.render(name=self.name, doc=self.doc, imports=self.imports,
                               definitions=defs)


class PythonDefinition(object):
    template_name = None

    @classmethod
    def create(cls, def0, scope):
        '''Create a python definition.'''
        if def0.is_message:
            return PythonMessage(def0, scope)

        elif def0.is_enum:
            return PythonEnum(def0)

        elif def0.is_interface:
            return PythonInterface(def0, scope)

        raise ValueError('Unsupported definition %s' % def0)

    def render(self, templates):
        template = templates.get(self.template_name)
        return template.render(**self.__dict__)


class PythonEnum(PythonDefinition):
    template_name = 'enum.template'

    def __init__(self, enum):
        self.name = enum.name
        self.doc = pydoc(enum.doc)
        self.values = [value.name for value in enum.values]


class PythonMessage(PythonDefinition):
    template_name = 'message.template'

    def __init__(self, msg, scope):
        self.name = msg.name
        self.doc = pydoc(msg.doc)

        self.base = scope(msg.base)
        self.subtypes = [(scope(stype.discriminator_value), scope(stype))
                         for stype in msg.subtypes]
        self.discriminator_value = scope(msg.discriminator_value)
        self.discriminator = PythonField(msg.discriminator, scope) if msg.discriminator else None

        self.is_exception = msg.is_exception
        self.is_form = msg.is_form

        self.fields = [PythonField(field, scope) for field in msg.fields]
        self.inherited_fields = [PythonField(field, scope) for field in msg.inherited_fields]
        self.declared_fields = [PythonField(field, scope) for field in msg.declared_fields]

        self.root_or_base = self.base or ('pdef.Exc' if self.is_exception else 'pdef.Message')


class PythonField(object):
    def __init__(self, field, scope):
        self.name = field.name
        self.type = scope(field.type)
        self.is_discriminator = field.is_discriminator


class PythonInterface(PythonDefinition):
    template_name = 'interface.template'

    def __init__(self, iface, scope):
        self.name = iface.name
        self.doc = pydoc(iface.doc)
        self.exc = scope(iface.exc) if iface.exc else None
        self.declared_methods = [PythonMethod(m, scope) for m in iface.declared_methods]


class PythonMethod(object):
    def __init__(self, method, scope):
        self.name = method.name
        self.doc = pydoc(method.doc)

        self.result = scope(method.result)
        self.args = [PythonArg(arg, scope) for arg in method.args]

        self.is_index = method.is_index
        self.is_post = method.is_post


class PythonArg(object):
    def __init__(self, arg, scope):
        self.name = arg.name
        self.type = scope(arg.type)


class PythonReference(object):
    @classmethod
    def list(cls, type0, module, mapper):
        element = pyreference(type0.element, module, mapper)
        descriptor = 'descriptors.list0(%s)' % element.descriptor

        return PythonReference('list', descriptor)

    @classmethod
    def set(cls, type0, module, mapper):
        element = pyreference(type0.element, module, mapper)
        descriptor = 'descriptors.set0(%s)' % element.descriptor

        return PythonReference('set', descriptor)

    @classmethod
    def map(cls, type0, module, mapper):
        key = pyreference(type0.key, module, mapper)
        value = pyreference(type0.value, module, mapper)
        descriptor = 'descriptors.map0(%s, %s)' % (key.descriptor, value.descriptor)

        return PythonReference('dict', descriptor)

    @classmethod
    def enum_value(cls, type0, module, mapper):
        enum = pyreference(type0.enum, module, mapper)
        name = '%s.%s' % (enum.name, type0.name)

        return PythonReference(name, None)

    @classmethod
    def definition(cls, type0, module, mapper):
        if type0.module == module:
            # The definition is referenced from the declaring module.
            name = type0.name
        else:
            module_name = type0.module.name
            module_name = mapper(module_name) if mapper else module_name
            name = '%s.%s' % (module_name, type0.name)

        descriptor = '%s.__descriptor__' % name
        return PythonReference(name, descriptor)

    def __init__(self, name, descriptor):
        self.name = name
        self.descriptor = descriptor

    def __str__(self):
        return str(self.name)


NATIVE_TYPES = {
    TypeEnum.BOOL: PythonReference('bool', 'descriptors.bool0'),
    TypeEnum.INT16: PythonReference('int', 'descriptors.int16'),
    TypeEnum.INT32: PythonReference('int', 'descriptors.int32'),
    TypeEnum.INT64: PythonReference('int', 'descriptors.int64'),
    TypeEnum.FLOAT: PythonReference('float', 'descriptors.float0'),
    TypeEnum.DOUBLE: PythonReference('float', 'descriptors.double0'),
    TypeEnum.STRING: PythonReference('unicode', 'descriptors.string'),
    TypeEnum.OBJECT: PythonReference('object', 'descriptors.object0'),
    TypeEnum.VOID: PythonReference('object', 'descriptors.void'),
}


def pytemplates():
    '''Return python generator templates.'''
    return generator.Templates(__file__)


def pyreference(type0, module=None, mapper=None):
    '''Create a python reference.

        @param type0:   pdef definition.
        @param module:  pdef module in which the definition is referenced.
        @param mapper:  optional module name mapper.
        '''
    if type0 is None:
        return None

    elif type0.is_native:
        return NATIVE_TYPES[type0.type]

    elif type0.is_list:
        return PythonReference.list(type0, module, mapper)

    elif type0.is_set:
        return PythonReference.set(type0, module, mapper)

    elif type0.is_map:
        return PythonReference.map(type0, module, mapper)

    elif type0.is_enum_value:
        return PythonReference.enum_value(type0, module, mapper)

    return PythonReference.definition(type0, module, mapper)


def pyimport(imported_module, mapper=None):
    '''Create a python import string.'''
    if not mapper:
        return imported_module.module.name
    return mapper(imported_module.module.name)


def pydoc(doc):
    if not doc:
        return ''

    # Escape the python docstrings delimiters,
    # and strip empty characters.
    s = doc.replace('"""', '\"\"\"').strip()

    if '\n' not in s:
        # It's a one-line comment.
        return s

    # It's a multi-line comment.
    return '\n' + s + '\n\n'


class DirectoryOrFile(object):
    @classmethod
    def from_modules(cls, out, pymodules):
        '''Create a file/directory from pymodules.'''
        root = DirectoryOrFile(out, is_root=True)

        for pm in pymodules:
            node = root
            for name in pm.name.split('.'):
                node = node.child(name)
            node.module = pm

        return root

    def __init__(self, name, parent=None, is_root=False):
        self.name = name
        self.parent = parent
        self.children = {}

        self.is_directory = False
        self.is_root = is_root

        self.module = None

    def child(self, name):
        '''Return or create a child directory or file.'''
        if name not in self.children:
            node = DirectoryOrFile(name, parent=self)
            self.children[name] = node
            self.is_directory = True

        return self.children[name]

    @property
    def dirpath(self):
        '''Return this directory path if a directory, else the parent directory path.'''
        if self.is_directory:
            return os.path.join(self.parent.dirpath, self.name) if self.parent else self.name
        return self.parent.dirpath if self.parent else ''

    @property
    def modulepath(self):
        '''Return the dirpath/__init__.py if a directory or parentpath/modulename.py if a module.'''
        if self.is_directory:
            return os.path.join(self.dirpath, '__init__.py')
        return os.path.join(self.dirpath, self.name + '.py')

    def code(self, templates):
        '''Return an empty python code if a directory or rendered module module code if a module.'''
        if self.module:
            return self.module.render(templates)
        elif self.is_directory:
            return '# encoding: utf-8\n'

        raise AssertionError('Not a directory or a module')

    def write(self, templates):
        '''Write this directory or module and its children to the filesystem.'''
        self._write_module(templates)
        self._write_children(templates)

    def _write_module(self, templates):
        if self.is_root:
            return

        code = self.code(templates)
        generator.mkdir_p(self.dirpath)

        modulepath = self.modulepath
        with open(modulepath, 'wt') as f:
            f.write(code)
            logging.info('Created %s', modulepath)

    def _write_children(self, templates):
        for child in self.children.values():
            child.write(templates)
