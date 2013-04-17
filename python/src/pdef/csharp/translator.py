# encoding: utf-8
import logging
import os.path
from jinja2 import Environment
from pdef import lang
from pdef.utils import upper_first

# Map from pdef types to c# types
NATIVE_MAP = {
    'bool': 'bool',
    'int16': 'short',
    'int32': 'int',
    'int64': 'long',
    'float': 'float',
    'double': 'double',
    'void': 'void',
    'string': 'string',
    'object': 'object',
    'list': 'IList',
    'set': 'ISet',
    'map': 'IDictionary'
}


FILE = os.path.join(os.path.dirname(__file__), 'file.template')
ENUM_FILE = os.path.join(os.path.dirname(__file__), 'enum.template')
MESSAGE_FILE = os.path.join(os.path.dirname(__file__), 'message.template')
IFACE_FILE = os.path.join(os.path.dirname(__file__), 'interface.template')
with open(FILE, 'r') as f:
    FILE_TEMPLATE = f.read()
with open(ENUM_FILE, 'r') as f:
    ENUM_TEMPLATE = f.read()
with open(MESSAGE_FILE, 'r') as f:
    MESSAGE_TEMPLATE = f.read()
with open(IFACE_FILE, 'r') as f:
    IFACE_TEMPLATE = f.read()

ENV = Environment(trim_blocks=True)
FILE = ENV.from_string(FILE_TEMPLATE)
ENUM = ENV.from_string(ENUM_TEMPLATE)
MESSAGE = ENV.from_string(MESSAGE_TEMPLATE)
IFACE = ENV.from_string(IFACE_TEMPLATE)


class CsharpPackage(object):
    def __init__(self, package):
        self.package = package
        self.files = []

        for module in package.modules:
            file = CsharpFile(module)
            self.files.append(file)

    def write_to(self, outdir):
        for file in self.files:
            file.write_to(outdir)


class CsharpFile(object):
    def __init__(self, module):
        self.namespace = module.package.name.capitalize()
        # package io.pdef
        # module io.pdef.test => Test.cs
        # module io.pdef.fixtures => TestFixtures.cs
        prefix = module.package.name.rpartition('.')[0]
        self.filename = '%s.cs' % module.name[len(prefix):].title().replace('.', '')

        self.definitions = []
        for definition in module.definitions:
            self._add_definition(definition)

    def _add_definition(self, d):
        if isinstance(d, lang.Message):
            cs = CsharpMessage(d)
        elif isinstance(d, lang.Enum):
            cs = CsharpEnum(d)
        elif isinstance(d, lang.Interface):
            cs = CsharpInterface(d)
        else:
            return

        self.definitions.append(cs)

    @property
    def code(self):
        data = {
            'namespace': self.namespace,
            'definitions': [d.code for d in self.definitions]
        }
        return FILE.render(**data)

    def write_to(self, outdir):
        fullpath = os.path.join(outdir, self.filename)
        code = self.code
        with open(fullpath, 'wt') as f:
            f.write(code)

        logging.info('Created %s', fullpath)


class CsharpEnum(object):
    def __init__(self, enum):
        self.name = upper_first(enum.name)
        self.values = [v.name.upper() for v in enum.values]

    @property
    def code(self):
        return ENUM.render(**self.__dict__)


class CsharpMessage(object):
    def __init__(self, msg):
        self.name = upper_first(msg.name)
        self.base = ref(msg.base) if msg.base else None
        self.declared_fields = [CsharpField(f) for f in msg.declared_fields]

        self.type_field = upper_first(msg.subtypes.field.name) if msg.subtypes else None
        self.subtypes = tuple((upper_first(k.name), ref(v))
            for k, v in msg.subtypes.as_map().items()) if msg.subtypes else ()

    @property
    def code(self):
        return MESSAGE.render(**self.__dict__)


class CsharpField(object):
    def __init__(self, field):
        self.name = upper_first(field.name)
        self.type = ref(field.type)


class CsharpInterface(object):
    def __init__(self, iface):
        self.name = 'I' + upper_first(iface.name)
        self.bases = [ref(base) for base in iface.bases]
        self.declared_methods = [CsharpMethod(m) for m in iface.declared_methods]

    @property
    def code(self):
        return IFACE.render(**self.__dict__)


class CsharpMethod(object):
    def __init__(self, method):
        self.name = upper_first(method.name)
        self.args = tuple((upper_first(a.name), ref(a.type)) for a in method.args)

        if isinstance(method.result, lang.Interface):
            self.result = ref(method.result)
        else:
            self.result = 'Task<%s>' % ref(method.result)


def ref(t):
    '''Returns a string c# reference for a type.'''
    if isinstance(t, lang.Native):
        return NATIVE_MAP[t.name]

    if isinstance(t, lang.Interface):
        return 'I' + upper_first(t.name)

    if not isinstance(t, lang.ParameterizedNative):
        return upper_first(t.name)

    s = ref(t.rawtype)
    if not t.variables:
        return s

    return '%s<%s>' % (s, ', '.join(ref(var) for var in t.variables))
