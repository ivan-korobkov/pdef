# encoding: utf-8
import logging

from pdef.preconditions import *


class ListMap(list):

    def __init__(self, items=None, attr='name', duplicates=None):
        items = items if items else []
        super(ListMap, self).__init__(items)

        self.map = {}
        for item in items:
            val = getattr(item, attr)
            if val not in self.map:
                self.map[val] = item
                continue

            if duplicates:
                duplicates.append(val)
            else:
                raise ValueError('Duplicate item "%s"' % val)


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
    def __init__(self, name):
        self.name = name
        self.parent = None

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    @property
    def fullname(self):
        if self.parent:
            return '%s.%s' % (self.parent.fullname, self.name)
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
    def __init__(self, name, imports=None, definitions=None):
        super(Package, self).__init__(name)

        self.imports = ListMap(imports) if imports else []
        self.definitions = ListMap(definitions) if definitions else []
        self.pool = None

    def link(self):
        linked = []
        for ref in self.imports:
            linked.append(ref.link())
        self.imports = ListMap(linked)

        linked = []
        for definition in self.definitions:
            linked.append(definition.link())
        self.definitions = ListMap(linked)

        return self

    def symbol(self, name):
        if '.' in name:
            # It's a package local type.

            # It's an imported type.
            # The first part of the name is the package name, the latter is the type name.
            # a.b.c.D = > a.b.c is the package, D is the type.
            package_name, type_name = name.rsplit(".", 1)

            if package_name not in self.imports.map:
                self.error('%s: type not found %s', self, name)
                return

            imported = self.imports.map[package_name]
            if type_name not in imported.definition_map:
                self.error('%s: type not found %s', self, name)
                return

            return imported.definition_map[type_name]

        if name in self.definitions.map:
            return self.definitions.map[name]

        # It can be a builtin type.
        for builtin in self.pool.builtins:
            if name in builtin.definition_map:
                return builtin.definition_map[name]


class ImportRef(Node):
    def __init__(self, name, alias):
        super(ImportRef, self).__init__(name)
        self.alias = check_not_none(alias)


class Definition(Node):
    def __init__(self, name, variables=None):
        super(Definition, self).__init__(name)

        self.variables = ListMap(variables) if variables else []
        self.rawtype = self
        self.specials = {}

    def link(self):
        return self

    def special(self, arg_map, pool):
        # Mind that some args can be partially specialized.

        if not self.variables:
            # The type is a generic class, but it has not arguments.
            return self

        # Recursively specialize all arguments.
        svars = []
        for var in self.variables:
            sarg = var.special(arg_map, pool)
            svars.append(sarg)
        svars = tuple(svars)

        # Construct a tuple specialization key.
        # It is possible, there is already a specialization of the rawtype with such arguments.
        key = (self.rawtype, svars)
        if key in self.specials:
            return self.specials[key]

        # No specialization for the key, construct a new one.
        rawtype = self.rawtype
        special = self.__class__(self.name, self.parent)
        special.rawtype = rawtype
        special.arguments = svars

        self.specials[key] = special
        pool.enqueue_special(special)

        return special

    def build_special(self):
        raise NotImplementedError


class TypeRef(Node):
    def __init__(self, name, args=None):
        super(TypeRef, self).__init__(name)
        self.args = list(args) if args else []

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
        if len(self.args) != len(rawtype.variables):
            self.error('%s: wrong number of generic arguments', self)
            return

        arg_map = {}
        for (var, argref) in zip(rawtype.variables, self.args):
            arg = argref.link()
            if not arg:
                # An error occurred.
                return

            arg = argref.type
            arg_map[var] = arg

        return rawtype.special(arg_map, self.pool)


class Variable(Node):
    def special(self, arg_map, pool):
        # The variable must be in the map itself.
        return arg_map.get(self)
