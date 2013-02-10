# encoding: utf-8
import logging
from pdef.preconditions import *


class SymbolTable(object):
    @classmethod
    def from_list(cls, items=None):
        table = SymbolTable()
        if not items:
            return table
        for item in items:
            table.add(item)
        return table

    @classmethod
    def from_tuples(cls, kv_tuples=None):
        table = SymbolTable()
        if not kv_tuples:
            return table
        for k, v in kv_tuples:
            table.add_with_name(k, v)
        return table

    def __init__(self):
        self.items = []
        self.map = {}

    def __iter__(self):
        return iter(self.items)

    def __len__(self):
        return len(self.items)

    def __contains__(self, key):
        return key in self.map

    def __getitem__(self, key):
        return self.map[key]

    def add(self, item):
        self.add_with_name(item.name, item)

    def add_with_name(self, name, item):
        if name in self.map:
            raise ValueError('Duplicate "%s"' % name)

        self.map[name] = item
        self.items.append(item)


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

    def link(self):
        return self

    def error(self, msg, *args):
        msg = '%s: %s' % (self.fullname, msg % args)
        logging.error(msg)

        if self.parent:
            self.parent.errors.append(msg)
        else:
            self.errors.append(msg)


class Package(Node):
    def __init__(self, name, builtin=None):
        super(Package, self).__init__(name)

        self.modules = SymbolTable()
        self.builtin = builtin

    def add_modules(self, *modules):
        for module in modules:
            self.modules.add(module)
            module.parent = self

    def link(self):
        for module in self.modules:
            module.link()

    def symbol(self, name):
        if not self.builtin:
            return

        if '.' in name:
            return

        for module in self.builtin.modules:
            if name in module.definitions:
                return module.definitions[name]


class Module(Node):
    def __init__(self, name, imports=None, definitions=None):
        super(Module, self).__init__(name)

        self.imports = SymbolTable()
        self.definitions = SymbolTable()

        if imports:
            map(self.add_imports, imports)

        if definitions:
            map(self.add_definitions, definitions)

    def add_imports(self, *imports):
        for imp in imports:
            self.imports.add(imp)
            if isinstance(imp, ModuleReference):
                imp.parent = self

    def add_definitions(self, *definitions):
        for d in definitions:
            self.definitions.add(d)
            d.parent = self

    def link(self):
        self.imports = SymbolTable.from_tuples((imp.name, imp.link()) for imp in self.imports)
        map(lambda x: x.link(), self.definitions)
        return self

    def symbol(self, name):
        if name in self.definitions:
            # It's a package local type.
            return self.definitions[name]

        elif '.' in name:
            # It's an imported type.
            # The first part of the name is the module name, the latter is the type name.
            # a.b.c.D = > a.b.c is the package, D is the type.
            module_name, type_name = name.rsplit(".", 1)

            if module_name not in self.imports:
                return

            module = self.imports[module_name]
            if type_name not in module.definitions:
                return

            return module.definitions[type_name]

        # It can be a builtin type.
        if not self.parent:
            return

        return self.parent.symbol(name)


class ModuleReference(Node):
    def __init__(self, module_name, name=None):
        super(ModuleReference, self).__init__(name if name else module_name)
        self.module_name = module_name

    def link(self):
        module = self.parent
        package = module.parent

        if self.module_name in package.modules:
            return package.modules[self.module_name]

        self.error('import not found "%s"', self.module_name)


class Reference(Node):
    def __init__(self, name, *generic_args):
        super(Reference, self).__init__(name)
        self.args = []

        self.add_args(*generic_args)

    def add_args(self, *args):
        for arg in args:
            self.args.append(arg)
            if isinstance(arg, Reference):
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
            return rawtype

        # Link the argument references and create a specialization of the rawtype.
        if len(self.args) != len(rawtype.variables):
            self.error('wrong number of generic arguments')
            return

        linked_args = []
        for (var, args) in zip(rawtype.variables, self.args):
            linked_arg = args.link()
            if not linked_arg:
                # An error occurred.
                return

            linked_args.append(linked_arg)

        return Specialization(rawtype, *linked_args)


class Specialization(Node):
    '''Intermediate representation of a specialized type
    with both the raw type and the arguments linked.

    Should be compiled into a specialized type via specialize().
    '''
    def __init__(self, rawtype, *linked_args):
        super(Specialization, self).__init__(rawtype.name)

        self.rawtype = check_not_none(rawtype)
        self.args = list(linked_args)

        check_argument(len(rawtype.variables) != 0, 'Not a generic type %s', rawtype)
        for arg in self.args:
            check_argument(not isinstance(arg, Reference), 'Arguments must be linked, %s', arg)


class Type(Node):
    def __init__(self, name, variables=None):
        super(Type, self).__init__(name)

        self.declaration = self
        self.variables = SymbolTable()
        self.specials = {}

        if variables:
            self.add_variables(*variables)

    def add_variables(self, *vars):
        for var in vars:
            self.variables.add(var)
            var.parent = self

    def symbol(self, name):
        if name in self.variables:
            return self.variables[name]

        return super(Type, self).symbol(name)

    def link(self):
        self.variables = SymbolTable.from_tuples((var, var.link()) for var in self.variables)
        return self

    def special(self, arg_map):
        if not self.variables:
            # The type is a generic class, but it has not arguments.
            return self

        # Mind that some args can be partially specialized.
        # Recursively specialize all arguments.
        svars = tuple(var.special(arg_map) for var in self.variables)

        # Construct a tuple specialization key.
        # It is possible, there is already a specialization with such arguments.
        key = (self, svars)
        if key in self.specials:
            return self.specials[key]

        special = self.create_special(arg_map)
        special.declaration = self
        self.specials[key] = special

        special.link()
        return special

    def create_special(self, arg_map):
        raise NotImplementedError


class Variable(Type):
    def __init__(self, name):
        super(Variable, self).__init__(name)

    def link(self):
        return self

    def special(self, arg_map):
        # The variable must be in the map itself.
        svar = arg_map.get(self)
        if svar:
            return svar

        self.error('variable is not found in the args map')


class Native(Type):
    def __init__(self, name, variables=None, options=None):
        super(Native, self).__init__(name, variables)
        # TODO: java_type
        #self.java_type = options.java_type
        self.options = options

    def create_special(self, arg_map):
        svars = tuple(var.special(arg_map) for var in self.variables)
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

        self.base = None
        self.declared_fields = SymbolTable()
        self.fields = SymbolTable()

        if base:
            self.set_base(base)

        if declared_fields:
            map(self.add_fields, declared_fields)

    def set_base(self, base):
        self.base = base
        if isinstance(base, Reference):
            self.base.parent = self

    def add_fields(self, *fields):
        for field in fields:
            self.declared_fields.add(field)
            field.parent = self

    def link(self):
        self.base = self.base.link() if self.base else None
        self.declared_fields = SymbolTable.from_list(
                field.link() for field in self.declared_fields)
        return self

    def create_special(self, arg_map):
        svars = tuple(var.special(arg_map) for var in self.variables)
        sbase = self.base.special(arg_map) if self.base else None
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
