# encoding: utf-8
import logging
import os.path
from pdef import Type
from pdef.compiler.translator import AbstractTranslator, mkdir_p


def translate(out, package):
    '''Translates a package into python code.'''
    return PythonTranslator(out).translate(package)


class PythonTranslator(AbstractTranslator):
    def __init__(self, out, pymodule_suffix='_pd'):
        super(PythonTranslator, self).__init__(out)
        self.pymodule_suffix = pymodule_suffix

        self.module_template = self.read_template('module.template')
        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def translate(self, package):
        pymodules = [PythonModule(module, self.pymodule_suffix)
                     for module in package.modules]

        for pm in pymodules:
            self._write(pm)

    def _write(self, pymodule):
        name = pymodule.name + self.pymodule_suffix
        relpath = name.replace('.', os.path.sep) + '.py'
        filepath = os.path.join(self.out, relpath)
        dirpath = os.path.dirname(filepath)

        mkdir_p(dirpath)
        with open(filepath, 'wt') as f:
            f.write(pymodule.render(self))
            logging.info('Created %s', relpath)


class PythonModule(object):
    def __init__(self, module, pymodule_suffix):
        ref = lambda def0: pyref(def0, module, pymodule_suffix)

        self.name = module.name
        self.imports = [pyimport(import0, pymodule_suffix) for import0 in module.imports]
        self.definitions = [pydef(def0, ref) for def0 in module.definitions]

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
        self.discriminator_value = ref(msg.discriminator_value) if msg.discriminator_value else None
        self.subtypes = [(ref(subtype), ref(submessage))
                         for subtype, submessage in msg.subtypes.items()]
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


def pyimport(import0, pymodule_suffix=''):
    '''Create a python import string.'''
    return '%s%s' % (import0.module.name, pymodule_suffix)


def pyref(def0, module=None, pymodule_suffix=''):
    '''Create a python reference.

    @param def0:    pdef definition.
    @param module:  pdef module in which the definition is referenced.
    @param pymodule_suffix: suffix which is used for all python modules.
    '''
    type0 = def0.type
    if type0 in NATIVE:
        return NATIVE[type0]

    if def0.is_list:
        element = pyref(def0.element, module, pymodule_suffix)
        descriptor = 'descriptors.list0(%s)' % element.descriptor
        return PythonRef('list', descriptor)

    elif def0.is_set:
        element = pyref(def0.element, module, pymodule_suffix)
        descriptor = 'descriptors.set0(%s)' % element.descriptor
        return PythonRef('set', descriptor)

    elif def0.is_map:
        key = pyref(def0.key, module, pymodule_suffix)
        value = pyref(def0.value, module, pymodule_suffix)
        descriptor = 'descriptors.map0(%s, %s)' % (key.descriptor, value.descriptor)
        return PythonRef('dict', descriptor)

    elif def0.is_enum_value:
        enum = pyref(def0.enum, module, pymodule_suffix)
        name = '%s.%s' % (enum.name, def0.name)
        return PythonRef(name, None)

    if def0.module == module:
        name = def0.name
    else:
        name = '%s%s.%s' % (def0.module.name, pymodule_suffix, def0.name)
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
