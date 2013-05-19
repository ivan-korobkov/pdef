# encoding: utf-8
from itertools import groupby
import logging
import os.path
from jinja2 import Environment
from pdef import lang
from pdef.common import Type, upper_first, mkdir_p


class CsharpTranslator(object):
    def __init__(self, out):
        self.out = out

        self.env = Environment(trim_blocks=True)
        self.file_template = self.read_template('file.template')
        self.enum_template = self.read_template('enum.template')
        self.message_template = self.read_template('message.template')
        self.interface_template = self.read_template('interface.template')

    def __str__(self):
        return 'CsharpTranslator'

    def write_definitions(self, defs):
        '''Writes definitions to the out directory.'''
        bymodule = groupby(defs, key=lambda x: x.module)
        for module, defs0 in bymodule:
            self._write_module(module, defs0)

    def _write_module(self, module, defs):
        file0 = CsharpFile(module, self.file_template)
        for def0 in defs:
            csdef = self.definition(def0)
            file0.add_definition(csdef)

        self._write_file(file0.filename, file0.code)

    def _write_file(self, filename, code):
        mkdir_p(self.out)

        fullpath = os.path.join(self.out, filename)
        with open(fullpath, 'wt') as f:
            f.write(code)

        logging.info('%s: created %s', self, fullpath)

    def definition(self, def0):
        '''Creates and returns a c# definition.'''
        t = def0.type
        if t == Type.ENUM: return CsharpEnum(def0, self.enum_template)
        elif t == Type.MESSAGE: return CsharpMessage(def0, self.message_template)
        elif t == Type.INTERFACE: return CsharpInterface(def0, self.interface_template)
        raise ValueError('Unsupported definition %s' % def0)

    def read_template(self, name):
        path = os.path.join(os.path.dirname(__file__), name)
        with open(path, 'r') as f:
            text = f.read()
        return self.env.from_string(text)


class CsharpFile(object):
    def __init__(self, module, template):
        self.namespace = module.name.capitalize()
        # package io.pdef
        # module io.pdef.test => Test.cs
        # module io.pdef.fixtures => TestFixtures.cs
        prefix = module.name.rpartition('.')[0]
        self.filename = '%s.cs' % module.name[len(prefix):].title().replace('.', '')
        self.template = template

        self.definitions = []

    def add_definition(self, d):
        if not isinstance(d, CsharpDefinition):
            raise ValueError('Not a C# definition ' + d)

        self.definitions.append(d)

    @property
    def code(self):
        data = {
            'namespace': self.namespace,
            'definitions': [d.code for d in self.definitions]
        }
        return self.template.render(**data)


class CsharpDefinition(object):
    def __init__(self, name, template):
        self.name = name
        self.template = template

    @property
    def code(self):
        return self.template.render(**self.__dict__)


class CsharpEnum(CsharpDefinition):
    def __init__(self, enum, template):
        super(CsharpEnum, self).__init__(enum.name, template)
        self.values = [v.name.upper() for v in enum.values.values()]


class CsharpMessage(CsharpDefinition):
    def __init__(self, msg, template):
        super(CsharpMessage, self).__init__(msg.name, template)
        self.base = ref(msg.base) if msg.base else None

        self.discriminator_field = CsharpField(msg.discriminator_field) \
            if msg.discriminator_field else None
        self.subtypes = tuple((k.name.lower(), ref(v)) for k, v in msg.subtypes.items())

        self.declared_fields = [CsharpField(f) for f in msg.declared_fields.values()]
        self.is_exception = msg.is_exception


class CsharpField(object):
    def __init__(self, field):
        self.name = upper_first(field.name)
        self.type = ref(field.type)


class CsharpInterface(CsharpDefinition):
    def __init__(self, iface, template):
        super(CsharpInterface, self).__init__('I' + iface.name, template)
        self.bases = [ref(base) for base in iface.bases]
        self.declared_methods = [CsharpMethod(m) for m in iface.declared_methods.values()]


class CsharpMethod(object):
    def __init__(self, method):
        self.name = upper_first(method.name)
        self.args = tuple((a.name, ref(a.type)) for a in method.args.values())

        if isinstance(method.result, lang.Interface):
            self.result = ref(method.result)
        else:
            s = ref(method.result)
            self.result = s if s == 'void' else 'IObservable<%s>' % ref(method.result)


def ref(obj):
    '''Returns a string c# reference for a type.'''
    t = obj.type
    if t in NATIVE_MAP: return NATIVE_MAP[t]
    elif t == Type.LIST: return 'IList<%s>' % ref(obj.element)
    elif t == Type.SET: return 'ISet<%s>' % ref(obj.element)
    elif t == Type.MAP: return 'IDictionary<%s, %s>' % (ref(obj.key), ref(obj.value))
    elif t == Type.INTERFACE: return 'I%s' % upper_first(obj.name)
    elif t == Type.ENUM_VALUE: return '%s.%s' % (ref(obj.enum), obj.name)
    return obj.name


# Map from pdef types to c# types
NATIVE_MAP = {
    Type.BOOL : 'bool',
    Type.INT16: 'short',
    Type.INT32: 'int',
    Type.INT64: 'long',
    Type.FLOAT: 'float',
    Type.DOUBLE: 'double',
    Type.VOID: 'void',
    Type.STRING: 'string',
    Type.OBJECT: 'object'
}
