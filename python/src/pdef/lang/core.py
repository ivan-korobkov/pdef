# encoding: utf-8
import logging

from pdef import ast
from pdef.preconditions import *


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
        from pdef.lang.datatypes import Native, Enum, Message

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


class Format(object):
    LINE = "line"
