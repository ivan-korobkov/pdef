# encoding: utf-8
import os
import os.path
import logging
from jinja2 import Environment

from pdef import lang
from pdef.preconditions import *


PACKAGE_FILE = os.path.join(os.path.dirname(__file__), "java_package.jinja2")
MESSAGE_FILE = os.path.join(os.path.dirname(__file__), "java_message.jinja2")
ENUM_FILE = os.path.join(os.path.dirname(__file__), "java_enum.jinja2")

with open(PACKAGE_FILE, "r") as f:
    PACKAGE_TEMPLATE = f.read()
with open(MESSAGE_FILE, "r") as f:
    MESSAGE_TEMPLATE = f.read()
with open(ENUM_FILE, "r") as f:
    ENUM_TEMPLATE = f.read()


class JavaType(object):
    @classmethod
    def from_type(cls, t, can_be_primitive=True):
        name = t.name
        package_name = ''
        is_variable = False

        if isinstance(t, lang.Variable):
            is_variable = True
        elif isinstance(t, lang.Native):
            name = t.options.java_type
            check_not_none(name)
        else:
            package_name = t.package.name

        variables = [JavaType.from_type(var, False) for var in t.variables]
        arguments = [JavaType.from_type(arg, False) for arg in t.arguments]

        jt = JavaType(name, package_name, variables, arguments, is_variable)
        if can_be_primitive:
            jt.primitive = t.options.java_primitive

        return jt

    def __init__(self, name, package_name='', variables=None, arguments=None, is_variable=False):
        self.name = name
        self.primitive = None
        self.package_name = package_name

        self.variables = tuple(variables) if variables else ()
        self.arguments = tuple(arguments) if arguments else ()
        self.is_variable = is_variable

        self.vars = '<%s>' % join_str(', ', self.variables) if self.variables else ''
        self.args = '<%s>' % join_str(', ', self.arguments) if self.arguments else ''

        self.local = '%s%s' % (self.name, self.args)
        self.full = '%s.%s' % (self.package_name, self.local) if self.package_name else self.local
        self.full_wo_args = '%s.%s' % (self.package_name, self.name) \
                if self.package_name else self.name

        self._all_variables = None

    def __str__(self):
        if self.primitive:
            return self.primitive
        return self.full

    @property
    def generic(self):
        return bool(self.arguments) or self.is_variable

    @property
    def all_variables(self):
        '''For Map<List<T>, Map<K, List<T>> returns (T, K).'''
        if self._all_variables is not None:
            return self._all_variables

        vars = []
        for arg in self.arguments:
            if isinstance(arg, lang.Variable) and arg not in vars:
                vars.append(arg)
            else:
                subvars = arg.all_variables
                for subvar in subvars:
                    if subvar not in vars:
                        vars.append(subvar)

        self._all_variables = vars


class JavaDefinition(object):
    def __init__(self, definition):
        self.type = JavaType.from_type(definition)
        self.name = definition.name
        self.options = definition.options

        self.variables = [JavaType.from_type(var, False) for var in definition.variables]
        self.arguments = [JavaType.from_type(arg, False) for arg in definition.arguments]

        self.line_format = definition.line_format


class JavaEnum(JavaDefinition):
    def __init__(self, enum):
        super(JavaEnum, self).__init__(enum)

        check_isinstance(enum, lang.Enum)
        self.type = JavaType.from_type(enum)
        # TODO: Switch from strings to JavaEnumValue
        self.values = enum.values

        self.descriptor = JavaDescriptor(self)


class JavaEnumValue(object):
    def __init__(self, enum_value):
        self.type = JavaType.from_type(enum_value.type)
        self.value = enum_value.value

    def __str__(self):
        return '%s.%s' % (self.type, self.value)


class JavaMessage(JavaDefinition):
    def __init__(self, message):
        super(JavaMessage, self).__init__(message)
        check_isinstance(message, lang.Message)

        self.base = JavaType.from_type(message.base) if message.base else None
        self.is_type_base = message.is_type_base
        self.type_base_field = None

        declared_set = set(message.declared_fields)
        self.inherited_fields = []
        for field in message.fields:
            if field in declared_set:
                continue
            jfield = JavaField(field)
            self.inherited_fields.append(jfield)

        inherited_count = len(self.inherited_fields)
        self.declared_fields = []
        for field in message.declared_fields:
            jfield = JavaField(field)
            jfield.index = inherited_count
            inherited_count += 1
            self.declared_fields.append(jfield)

            if not message.is_type_base:
                continue

            if field == message.type_field:
                self.type_base_field = jfield

        self.mutable = JavaType('Mutable', package_name=self.type.package_name,
                                variables=self.variables, arguments=self.arguments)
        self.descriptor = JavaMessageDescriptor(self)

        self.subtype_map = {}
        for enum_value, subtype in message.subtype_map.items():
            jenum = JavaEnumValue(enum_value)
            jsubtype = JavaType.from_type(subtype)
            self.subtype_map[jenum] = jsubtype


class JavaField(object):
    def __init__(self, field):
        self.name = field.name
        self.type = JavaType.from_type(field.type)
        self.value = JavaEnumValue(field.value) if field.value else None
        self.is_type_field = field.is_type_field
        self.is_type_base_field = field.is_type_base_field
        self.is_subtype_field = self.is_type_field and not self.is_type_base_field
        self.read_only = field.read_only

        self.index = -1

        title_name = upper_first(self.name)
        self.get = 'get%s' % title_name
        self.set = 'set%s' % title_name
        self.has = 'has%s' % title_name
        self.clear = 'clear%s' % title_name


class JavaDescriptor(object):
    def __init__(self, jdefinition):
        dtype = jdefinition.type
        self.definition = jdefinition
        self.type = JavaType(dtype.name + '.Descriptor',
                             package_name=dtype.package_name,
                             variables=dtype.variables,
                             arguments=dtype.arguments)


class JavaMessageDescriptor(JavaDescriptor):
    def __init__(self, jmessage):
        super(JavaMessageDescriptor, self).__init__(jmessage)
        self.field_descriptors = []
        self.type_base_field_descriptor = None

        for field in jmessage.declared_fields:
            if field.is_subtype_field:
                continue

            fd = JavaDescriptorFieldClass(field)
            self.field_descriptors.append(fd)

            if field == jmessage.type_base_field:
                self.type_base_field_descriptor = fd


class JavaDescriptorFieldClass(object):
    def __init__(self, java_field):
        self.field = java_field

        self.name = '%sField' % java_field.name
        self.get = 'get%sField' % upper_first(self.field.name)


class JavaPackage(object):

    @classmethod
    def from_package(cls, package):
        definitions = []
        for definition in package.definitions:
            if isinstance(definition, lang.Message):
                jdef = JavaMessage(definition)
            elif isinstance(definition, lang.Enum):
                jdef = JavaEnum(definition)
            else:
                continue
            definitions.append(jdef)

        imports = [JavaPackage.create_type(imp.name) for imp in package.imports]
        return JavaPackage(package.name, definitions, imports)

    @classmethod
    def create_type(cls, name):
        class_name = name.rpartition('.')[2] + 'Package'
        return JavaType(upper_first(class_name), name)

    def __init__(self, name, jdefinitions, jimport_types=None):
        self.name = name
        self.type = JavaPackage.create_type(name)
        self.imports = tuple(jimport_types) if jimport_types else ()
        self.definitions = tuple(jdefinitions) if jdefinitions else ()


class JavaTranslator(object):
    def __init__(self, outdir, pool):
        self.pool = check_not_none(pool)
        self.errors = []

        self.env = Environment(trim_blocks=True)
        self.env.filters['upper_first'] = upper_first

        self.message_template = self.env.from_string(MESSAGE_TEMPLATE)
        self.package_template = self.env.from_string(PACKAGE_TEMPLATE)
        self.enum_template = self.env.from_string(ENUM_TEMPLATE)

        self.outdir = check_not_none(outdir)
        mkdir_p(self.outdir)

    def translate(self):
        jpackages = [JavaPackage.from_package(package) for package in self.pool.packages]
        for jpackage in jpackages:
            src = self.package_template.render(package=jpackage)
            self._write_file(jpackage.name, jpackage.type.name, src)

            for jdef in jpackage.definitions:
                if isinstance(jdef, JavaMessage):
                    src = self._translate_message(jdef, jpackage)
                elif isinstance(jdef, JavaEnum):
                    src = self._translate_enum(jdef, jpackage)
                else:
                    continue
                self._write_file(jpackage.name, jdef.type.name, src)

    def _translate_message(self, jdefinition, jpackage):
        return self.message_template.render(
            message=jdefinition,
            descriptor=jdefinition.descriptor,
            mutable_type=jdefinition.mutable,
            package=jpackage)

    def _translate_enum(self, jenum, jpackage):
        return self.enum_template.render(enum=jenum,package=jpackage)

    def _write_file(self, package_name, class_name, src):
        dirs = package_name.split('.')
        dirpath = os.path.join(self.outdir, *dirs)
        mkdir_p(dirpath)

        filename = '%s.java' % class_name
        filepath = os.path.join(dirpath, filename)
        with open(filepath, 'wr') as f:
            f.write(src)
        logging.info('Created %s' % filepath)


def upper_first(s):
    if not s:
        return s
    return s[0].upper() + s[1:]


def join_str(delimiter, iterable):
    return delimiter.join(str(e) for e in iterable)


def mkdir_p(dirname):
    if os.path.exists(dirname):
        return
    os.makedirs(dirname)
