# encoding: utf-8
from collections import deque
import logging
from struct import pack

from pdef import ast
from pdef.preconditions import *


class Format(object):
    LINE = "line"


class Pool(object):
    def __init__(self):
        self.packages = []
        self.package_map = {}
        self.builtins = [] # List of packages, which types can be referenced without importing.
        self.errors = []
        self.special_to_build = []
        self.specials = set()

    def error(self, msg, *args):
        self.errors.append(msg % args)
        logging.error(msg, *args)

    def add_packages(self, nodes):
        self._create_packages(nodes)
        if self.errors:
            return

        self._link()
        if self.errors:
            return

        self._build_special()
        if self.errors:
            return

    def _create_packages(self, nodes):
        for node in nodes:
            package  = Package.from_node(node, self)
            if package.name in self.package_map:
                package.error("Duplicate package")
                continue

            self.packages.append(package)
            self.package_map[package.name] = package

    def _link(self):
        for package in self.packages:
            package.link()

    def _build_special(self):
        while True:
            if not len(self.special_to_build):
                return

            special = self.special_to_build[-1]
            del self.special_to_build[-1]

            special.build_special()

    def enqueue_special(self, special):
        self.special_to_build.append(special)
        self.specials.add(special)


class Options(object):
    def __init__(self, map=None):
        map = map if map else {}

        self.map = map
        self.java_type = map.get('java_type')
        self.java_primitive = map.get('java_primitive')

        self.type_field = map.get('type_field')
        self.value = map.get('value')
        self.format = map.get('format')

    def __str__(self):
        return str(self.map)


class Node(object):
    def __init__(self, name, parent):
        self.name = name
        self.parent = parent

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.simple_repr)

    @property
    def pool(self):
        return self.parent.pool

    @property
    def fullpath(self):
        if not self.parent:
            return self.simple_repr
        return '%s.%s' % (self.parent.fullpath, self.name)

    @property
    def simple_repr(self):
        return self.name

    def symbol(self, name):
        if self.parent:
            return self.parent.symbol(name)

    def error(self, msg, *args):
        if self.parent:
            self.parent.error(msg, *args)
        else:
            logging.error(msg, *args)


class Package(Node):
    @classmethod
    def from_node(cls, node, pool):
        check_isinstance(node, ast.Package)
        package = Package(node.name, pool)

        import_aliases = set()
        for inode in node.imports:
            ref = ImportRef.from_node(inode, package)
            if ref.alias in import_aliases:
                ref.error('Duplicate import')
                continue

            package.importrefs.append(ref)
            import_aliases.add(ref.alias)

        for dnode in node.definitions:
            definition = TypeDefinition.from_node(dnode, package)
            if definition.name in package.definition_map:
                definition.error('Duplicate definition')
                continue

            package.definitions.append(definition)
            package.definition_map[definition.name] = definition

        return package

    def __init__(self, name, pool):
        super(Package, self).__init__(name, None)
        self._pool = pool
        self.importrefs = []
        self.definitions = []

        # Set when linked
        self.imports = []
        self.import_map = {}
        self.definition_map = {}

    def __repr__(self):
        return "<Package %s>" % self.name

    @property
    def pool(self):
        return self._pool

    def symbol(self, name):
        if '.' in name:
            # It's a package local type.

            # It's an imported type.
            # The first part of the name is the package name, the latter is the type name.
            # a.b.c.D = > a.b.c is the package, D is the type.
            package_name, type_name = name.rsplit(".", 1)

            if package_name not in self.import_map:
                self.error('%s: type not found %s', self, name)
                return

            imported = self.import_map[package_name]
            if type_name not in imported.definition_map:
                self.error('%s: type not found %s', self, name)
                return

            return imported.definition_map[type_name]

        if name in self.definition_map:
            return self.definition_map[name]

        # It can be a builtin type.
        for builtin in self.pool.builtins:
            if name in builtin.definition_map:
                return builtin.definition_map[name]

    def link(self):
        for ref in self.importrefs:
            alias = ref.alias if ref.alias else ref.name
            if alias in self.import_map:
                self.error('%s: duplicate import %s', self, alias)
                continue

            imported = self.pool.package_map.get(ref.name)
            if imported:
                self.imports.append(imported)
                self.import_map[alias] = imported

            else:
                self.error('%s: import not found %s', self, ref.name)

        for definition in self.definitions:
            definition.link()

        return self


class ImportRef(Node):
    @classmethod
    def from_node(cls, node, package):
        alias = node.alias if node.alias else node.name
        return ImportRef(node.name, alias, package)

    def __init__(self, name, alias, package):
        super(ImportRef, self).__init__(name, package)
        self.alias = check_not_none(alias)


class TypeDefinition(Node):
    @classmethod
    def from_node(cls, node, package):
        if isinstance(node, ast.Native):
            return Native.from_node(node, package)
        elif isinstance(node, ast.Enum):
            return Enum.from_node(node, package)
        elif isinstance(node, ast.Message):
            return Message.from_node(node, package)
        else:
            package.error('Unsupported definition node %s', node)

    def __init__(self, name, package):
        super(TypeDefinition, self).__init__(name, package)
        self.package = package

        self.arguments = []
        self.variables = []
        self.variable_map = {} # {var_name, var}, present only in a definition.
        self.options = Options()

        self.rawtype = self
        self.specials = {}

    @property
    def simple_repr(self):
        if self.rawtype != self:
            # It's a specialization
            return "@%s<%s>" % (self.name, ', '.join(var.simple_repr for var in self.arguments))
        elif self.variables:
            # It's a declartion
            return "%s<%s>" % (self.name, ', '.join(var.simple_repr for var in self.arguments))
        return self.name

    @property
    def line_format(self):
        return self.options.format == Format.LINE

    def link(self):
        return self

    def special(self, arg_map, pool):
        # Mind that some args can be partially specialized.

        if not self.arguments:
            # The type is a generic class, but it has not arguments.
            return self

        # Recursively specialize all arguments.
        sargs = []
        for arg in self.arguments:
            sarg = arg.special(arg_map, pool)
            sargs.append(sarg)
        sargs = tuple(sargs)

        # Construct a tuple specialization key.
        # It is possible, there is already a specialization of the rawtype with such arguments.
        key = (self.rawtype, sargs)
        if key in self.specials:
            return self.specials[key]

        # No specialization for the key, construct a new one.
        rawtype = self.rawtype
        special = self.__class__(self.name, self.parent)
        special.rawtype = rawtype
        special.arguments = sargs

        self.specials[key] = special
        pool.enqueue_special(special)

        return special

    def build_special(self):
        raise NotImplementedError


class Native(TypeDefinition):
    @classmethod
    def from_node(cls, node, package):
        native = Native(node.name, package)

        for vname in node.variables:
            var = Variable(vname, native)
            if var.name in native.variable_map:
                var.error('Duplicate vairable')
                continue

            native.arguments.append(var)
            native.variables.append(var)
            native.variable_map[vname] = var

        native.options = Options(node.options)
        native.java_type = native.options.java_type

        return native

    def __init__(self, name, package):
        super(Native, self).__init__(name, package)

        self.java_type = None
        self.python_type = None
        self.rawtype = self

    def build_special(self):
        rawtype = self.rawtype

        self.options = rawtype.options
        self.java_type = rawtype.java_type
        self.python_type = rawtype.python_type


class Enum(TypeDefinition):
    @classmethod
    def from_node(cls, node, package):
        enum = Enum(node.name, package)

        for val in node.values:
            if val in enum.value_set:
                enum.error('Duplicate enum value %s', val)
                continue

            enum.values.append(val)
            enum.value_set.add(val)

        return enum

    def __init__(self, name, package):
        super(Enum, self).__init__(name, package)

        self.values = []
        self.value_set = set()


class Message(TypeDefinition):
    @classmethod
    def from_node(cls, node, package):
        message = Message(node.name, package)
        message.options = Options(node.options)

        for vname in node.variables:
            var = Variable(vname, message)
            if var.name in message.variable_map:
                var.error('Duplicate vairable')
                continue

            message.arguments.append(var)
            message.variables.append(var)
            message.variable_map[vname] = var

        basenode = node.base
        if basenode:
            message.baseref = TypeRef.from_node(basenode, message)

        for fnode in node.fields:
            field = Field.from_node(fnode, message)
            if field.name in message.declared_field_map:
                field.error('Duplicate field')
                continue

            message.declared_fields.append(field)
            message.declared_field_map[fnode.name] = field

        return message

    def __init__(self, name, package):
        super(Message, self).__init__(name, package)

        self.baseref = None
        self.declared_fields = []
        self.declared_field_map = {}

        self.base = None
        self.all_bases = []

        self.fields = []
        self.field_map = {}

        self.type_field = None
        self.is_type_base = None
        self.subtype_map = {} # Map {type_value: subtype}
        self.subtypes = []

        self.rawtype = self

    def symbol(self, name):
        if name in self.variable_map:
            return self.variable_map[name]

        return super(Message, self).symbol(name)

    def link(self):
        super(Message, self).link()

        baseref = self.baseref
        if baseref:
            self.base = baseref.link()

        for field in self.declared_fields:
            field.link()

        return self

    def build_special(self):
        if not self.arguments:
            raise ValueError("Not a special %s" % self)

        rawtype = self.rawtype
        arg_map = dict((var, arg) for (var, arg) in zip(rawtype.variables, self.arguments))

        base = rawtype.base
        if base:
            sbase = base.special(arg_map, self.pool)
            self.base = sbase

        declared_fields = []
        for field in rawtype.declared_fields:
            type = field.type
            stype = type.special(arg_map, self.pool)

            sfield = Field(field.name, self)
            sfield.type = stype
            sfield.options = field.options

            declared_fields.append(sfield)
        self.declared_fields = declared_fields


class Field(Node):
    @classmethod
    def from_node(cls, node, message):
        field = Field(node.name, message)
        field.typeref = TypeRef.from_node(node.type, field)
        field.options = Options(node.options)

        value = field.options.value
        if value:
            field.value_ref = EnumValueRef.from_node(value, field)

        return field

    def __init__(self, name, message):
        super(Field, self).__init__(name, message)
        self.message = message
        self.typeref = None

        self.type = None
        self.read_only = False
        self.is_type_field = False
        self.is_type_base_field = False
        self.value_ref = None
        self.value = None
        self.options = Options()

    def __repr__(self):
        if self.type:
            return "<Field %s, %s>" % (self.name, self.type.simple_repr)
        return "<Field %s, ref=%s>" % (self.name, self.typeref.simple_repr)

    def link(self):
        typeref = self.typeref
        self.type = typeref.link()

        value_ref = self.value_ref
        if not value_ref:
            return

        self.value = value_ref.link()
        return self


class TypeRef(Node):
    @classmethod
    def from_node(cls, node, parent):
        ref = TypeRef(node.name, parent)
        ref.argrefs = [TypeRef.from_node(anode, ref) for anode in node.args]
        return ref

    def __init__(self, name, parent):
        super(TypeRef, self).__init__(name, parent)
        self.argrefs = []

        self.type = None

    @property
    def simple_repr(self):
        if not self.argrefs:
            return self.name

        return '%s<%s>' % (self.name, ', '.join(arg.simple_repr for arg in self.argrefs))

    def link(self):
        # Find the rawtype by its name, for example: MyType, package.AnotherType, T (variable).
        rawtype = self.symbol(self.name)
        if not rawtype:
            self.error('%s: type not found', self)
            return

        # Return if the rawtype is not generic.
        if not rawtype.variables:
            self.type = rawtype
            return self.type

        # Link the argument references and create a specialization of the rawtype.
        if len(self.argrefs) != len(rawtype.variables):
            self.error('%s: wrong number of generic arguments', self)
            return

        arg_map = {}
        for (var, argref) in zip(rawtype.variables, self.argrefs):
            arg = argref.link()
            if not arg:
                # An error occurred.
                return

            arg = argref.type
            arg_map[var] = arg

        self.type = rawtype.special(arg_map, self.pool)
        return self.type


class Variable(TypeDefinition):
    def __eq__(self, other):
        if not isinstance(other, Variable):
            return False

        return self.name == other.name and self.parent == other.parent

    def __hash__( self ):
        r = 1
        r = r * 31 + hash(self.name)
        r = r * 31 + hash(self.parent)
        return int(r)

    def special(self, arg_map, pool):
        # The variable must be in the map itself.
        return arg_map.get(self)


class EnumValue(Node):
    def __init__(self, value, type, parent):
        super(EnumValue, self).__init__(value, parent)
        self.value = value
        self.type = type


class EnumValueRef(Node):
    @classmethod
    def from_node(cls, text, parent):
        '''Translates "package.EnumType.VALUE" into EnumValueRef("package.EnumType", "VALUE").'''
        typename, _, valuename = text.rpartition('.')
        if not typename or not valuename:
            parent.error("Wrong enum value %s ", text)
            return

        typeref = TypeRef(typename, parent)
        return EnumValueRef(valuename, typeref, parent)

    def __init__(self, valuename, typeref, parent):
        super(EnumValueRef, self).__init__(valuename, parent)

        self.typeref = typeref
        self.valuename = valuename
        self.value = None

    def link(self):
        typeref = self.typeref
        self.type = typeref.link()

        if not self.type:
            # An error occurred.
            return

        if not isinstance(self.type, Enum):
            self.error('%s: wrong type %s, must be an enum',self, self.type)
            return

        enum = self.type
        valuename = self.valuename
        if valuename not in enum.value_set:
            self.error('%s: enum value %s is not found', self, valuename)
            return

        self.value = EnumValue(valuename, enum, self.parent)
        return self.value
