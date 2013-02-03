# encoding: utf-8
from collections import OrderedDict
import logging


def list_to_map(items, attr='name', on_duplicate=None):
    d = OrderedDict()
    for item in items:
        val = getattr(item, attr)
        if val not in d:
            d[val] = item
            continue

        if on_duplicate:
            on_duplicate(val)
        else:
            raise ValueError('Duplicate item "%s"' % val)

    return d


class Node(object):
    def __init__(self, name):
        self.name = name
        self.parent = None
        self.errors = []

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
        msg = '%s: %s' % (self.fullname, msg % args)
        logging.error(msg)

        if self.parent:
            self.errors.append(msg)


class Package(Node):
    def __init__(self, name, builtin=None):
        super(Package, self).__init__(name)

        self.dependencies = []
        self.modules = {}

        self.errors = []
        self.builtin = builtin

    def error(self, msg, *args):
        self.errors.append(msg % args)
        logging.error(msg, *args)

    def add_module(self, module):
        if module.name in self.modules:
            module.error("Duplicate module")
            return

        self.modules[module.name] = module
        module.package = self

    def link(self):
        for module in self.modules.values():
            module.link()

    def symbol(self, name):
        if not self.builtin:
            return

        for module in self.builtin.modules.values():
            if name in module.definitions:
                return module.definitions[name]


class Module(Node):
    def __init__(self, name, imports=None, definitions=None, package=None):
        super(Module, self).__init__(name)

        self.imports = list_to_map(imports) if imports else {}
        self.definitions = list_to_map(definitions) if definitions else {}
        self.package = package

        for imp in self.imports.values():
            imp.parent = self

        for definition in self.definitions.values():
            definition.parent = self

    def link(self):
        self.imports = dict((imp.name, imp.link()) for imp in self.imports.values())
        self.definitions = list_to_map([definition.link()
                                        for definition in self.definitions.values()])
        return self

    def symbol(self, name):
        if name in self.definitions:
            # It's a package local type.
            return self.definitions[name]

        elif '.' in name:
            # It's an imported type.
            # The first part of the name is the package name, the latter is the type name.
            # a.b.c.D = > a.b.c is the package, D is the type.
            package_name, type_name = name.rsplit(".", 1)

            if package_name not in self.imports:
                return

            imported = self.imports[package_name]
            if type_name not in imported.definitions:
                return

            return imported.definitions[type_name]

        # It can be a builtin type.
        if not self.package:
            return
        gi
        return self.package.symbol(name)


class Import(Node):
    def __init__(self, package_name, name=None):
        super(Import, self).__init__(name if name else package_name)
        self.package_name = package_name

    def link(self):
        module = self.parent
        package = module.package

        if self.package_name in package.modules:
            return package.modules[self.package_name]

        self.error('import not found "%s"', self.package_name)


class Reference(Node):
    def __init__(self, name, args=None):
        super(Reference, self).__init__(name)
        self.args = list(args) if args else []

        for arg in self.args:
            arg.parent = self

    def link(self):
        # Find the rawtype by its name, for example: MyType, package.AnotherType, T (variable).
        rawtype = self.symbol(self.name)
        if not rawtype:
            self.parent.error('type not found "%s"', self.name)
            return

        # Return if the rawtype is not generic.
        if not rawtype.variables:
            self.type = rawtype
            return self.type

        # Link the argument references and create a specialization of the rawtype.
        if len(self.args) != len(rawtype.variables):
            self.error('wrong number of generic arguments')
            return

        arg_map = {}
        for (var, argref) in zip(rawtype.variables.values(), self.args):
            arg = argref.link()
            if not arg:
                # An error occurred.
                return

            arg_map[var] = arg

        return rawtype.special(arg_map)

    def special(self, arg_map):
        if not self.args:
            return self

        sargs = [arg.special(arg_map) for arg in self.args]
        return Reference(self.name, args=sargs)


class Variable(Node):
    def special(self, arg_map):
        # The variable must be in the map itself.
        svar = arg_map.get(self)
        if svar:
            return svar

        self.error('variable is not found in the args map')


class Type(Node):
    def __init__(self, name, variables=None):
        super(Type, self).__init__(name)

        self.declaration = self
        self.variables = list_to_map(variables) if variables else {}
        self.specials = {}

        for var in self.variables.values():
            var.parent = self

    def symbol(self, name):
        if name in self.variables:
            return self.variables[name]

        return super(Type, self).symbol(name)

    def link(self):
        self.variables = list_to_map(var.link() for var in self.variables)
        return self

    def special(self, arg_map):
        if not self.variables:
            # The type is a generic class, but it has not arguments.
            return self

        # Mind that some args can be partially specialized.
        # Recursively specialize all arguments.
        svars = tuple(var.special(arg_map) for var in self.variables.values())

        # Construct a tuple specialization key.
        # It is possible, there is already a specialization with such arguments.
        key = (self, svars)
        if key in self.specials:
            return self.specials[key]

        special = self.create_special(arg_map)
        special.declaration = self
        self.specials[key] = special
        return special

    def create_special(self, arg_map):
        raise NotImplementedError


class Native(Type):
    def __init__(self, name, variables=None, options=None):
        super(Native, self).__init__(name, variables)
        # TODO: java_type
        #self.java_type = options.java_type
        self.options = options

    def create_special(self, arg_map):
        svars = tuple(var.special(arg_map) for var in self.variables.values())
        return Native(self.name, variables=svars, options=self.options)


class Enum(Type):
    def __init__(self, name, values):
        super(Enum, self).__init__(name)

        self.values = set(values) if values else set()

    def create_special(self, arg_map):
        return self


class Message(Type):
    def __init__(self, name, variables=None, base=None, declared_fields=None):
        super(Message, self).__init__(name, variables)

        self.base = base
        self.declared_fields = list_to_map(declared_fields) if declared_fields else {}
        self.fields = {}

        if self.base:
            self.base.parent = self

        for field in self.declared_fields.values():
            field.parent = self

    def link(self):
        self.base = self.base.link() if self.base else None
        self.declared_fields = list_to_map([field.link()
                                            for field in self.declared_fields.values()])
        return self

    def create_special(self, arg_map):
        svars = tuple(var.special(arg_map) for var in self.variables)
        sbase = self.base.special(arg_map)
        sfields = [field.special(arg_map) for field in self.declared_fields]
        return Message(self.name, variables=svars, base=sbase, declared_fields=sfields)


class Field(Node):
    def __init__(self, name, type):
        super(Field, self).__init__(name)
        self.type = type
        self.type.parent = self

    def link(self):
        self.type = self.type.link()
        return self

    def create_special(self, arg_map):
        stype = self.type.special(arg_map)
        if stype == self.type:
            return self

        field = Field(self.name, stype)
        field.parent = self.parent
        return field


class EnumValue(Node):
    def __init__(self, type, value):
        super(EnumValue, self).__init__(value)
        self.type = type
        self.value = value

    def link(self):
        self.type = self.type.link()
        if not isinstance(self.type, Enum):
            self.error('wrong type %s, must be an enum', self.type)
            return

        if self.value not in self.type.value_set:
            self.error('enum value "%s" is not found', self.value)
            return

        return self
