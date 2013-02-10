# encoding: utf-8
import logging
from pdef.preconditions import *


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

    def error(self, msg, *args):
        msg = '%s: %s' % (self.fullname, msg % args)
        logging.error(msg)

        if self.parent:
            self.parent.errors.append(msg)
        else:
            self.errors.append(msg)

    def symbol(self, name):
        if self.parent:
            return self.parent.symbol(name)

    def link(self, parent):
        self.parent = parent


class Proxy(Node):
    def __init__(self, name):
        super(Proxy, self).__init__(name)
        self.delegate = None

    def __getattr__(self, item):
        self._check_delegate()
        return getattr(self.delegate, item)

    def __hash__(self):
        self._check_delegate()
        return hash(self.delegate)

    def __eq__(self, other):
        self._check_delegate()
        return self.delegate == other

    def _check_delegate(self):
        check_state(self.delegate is not None, 'The proxy delegate is not set in %s', self)


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


class Package(Node):
    def __init__(self, name, builtin=None):
        super(Package, self).__init__(name)

        self.modules = SymbolTable()
        self.builtin = builtin
        self.specializations = {}

    def add_modules(self, *modules):
        for module in modules:
            self.modules.add(module)

    def link(self):
        for module in self.modules:
            module.link_imports(self)

        for module in self.modules:
            module.link(self)

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
            self.add_imports(*imports)

        if definitions:
            self.add_definitions(*definitions)

    def add_imports(self, *imports):
        for imp in imports:
            self.imports.add(imp)

    def add_definitions(self, *definitions):
        for d in definitions:
            self.definitions.add(d)

    def link_imports(self, package):
        super(Module, self).link(package)

        for imp in self.imports:
            imp.link(self)

    def link(self, parent):
        for definition in self.definitions:
            definition.link(self)

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


class ModuleReference(Proxy):
    def __init__(self, module_name, name=None):
        super(ModuleReference, self).__init__(name if name else module_name)
        self.module_name = module_name

    def link(self, module):
        super(ModuleReference, self).link(module)

        package = module.parent
        if not self.module_name in package.modules:
            self.error('import not found "%s"', self.module_name)
            return

        self.delegate = package.modules[self.module_name]


class Reference(Proxy):
    def __init__(self, name, *generic_args):
        super(Reference, self).__init__(name)
        self.args = []
        self.add_args(*generic_args)

    def add_args(self, *args):
        for arg in args:
            self.args.append(arg)

    def link(self, parent):
        super(Reference, self).link(parent)
        for arg in self.args:
            arg.link(self)

        self.delegate = self._lookup()

    def _lookup(self):
        # Find the rawtype by its name, for example: MyType, package.AnotherType, T (variable).
        rawtype = self.symbol(self.name)
        if not rawtype:
            self.parent.error('type not found "%s"', self.name)
            return

        # Return if the rawtype is not generic,
        # Else create a specialization.
        if not rawtype.variables:
            return rawtype

        # Error if the number of args does not match the number of variables
        if len(self.args) != len(rawtype.variables):
            self.error('wrong number of generic arguments')
            return

        # Map the rawtype variables to the arguments.
        arg_map = {}
        for var, arg in zip(rawtype.variables, self.args):
            arg_map[var] = arg

        return rawtype.specialize(arg_map)


class Specialization(Proxy):
    def __init__(self, rawtype, arg_map):
        super(Specialization, self).__init__(rawtype.name)
        self.rawtype = rawtype
        self.arg_map = arg_map

    def __eq__(self, other):
        return super(object, self).__eq__(other)

    def __hash__(self):
        return super(object, self).__hash__()

    def link(self, parent):
        super(Specialization, self).link(parent)

        self.delegate = self.rawtype._create_special(self.arg_map)
        self.delegate.rawtype = self.rawtype


class Type(Node):
    def __init__(self, name, variables=None):
        super(Type, self).__init__(name)

        self.variables = SymbolTable()
        self.specials = {}
        self.rawtype = None

        if variables:
            self.add_variables(*variables)

    def add_variables(self, *vars):
        for var in vars:
            self.variables.add(var)

    def symbol(self, name):
        if name in self.variables:
            return self.variables[name]

        return super(Type, self).symbol(name)

    def link(self, parent):
        super(Type, self).link(parent)
        for var in self.variables:
            var.link(self)

    def specialize(self, arg_map):
        '''Returns a specialization proxy which is added to the package specializations.
        The specializations must be linked after all normal types are linked in all packages.
        '''
        if not self.variables:
            # This type is not generic.
            return self

        # Construct a tuple specialization key.
        # It is possible, there is already a specialization with such arguments.
        svars = tuple(var.specialize(arg_map) for var in self.variables)
        key = (self, svars)
        if key in self.specials:
            return self.specials[key]

        # This type can circularly reference itself during linking, i.e.:
        # Node<V>:
        #   Node<V> next

        # Create a proxy to allow circular specialization.
        proxy = Specialization(self, arg_map)
        self.specials[key] = proxy
        return proxy

    def _create_special(self, arg_map):
        '''Create a new specialization using a provided variable to argument map.'''
        raise NotImplementedError


class Variable(Type):
    def __init__(self, name):
        super(Variable, self).__init__(name)

    def specialize(self, arg_map):
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

    def _create_special(self, arg_map):
        svars = tuple(var.specialize(arg_map) for var in self.variables)
        return Native(self.name, variables=svars, options=self.options)


class Enum(Type):
    def __init__(self, name, values):
        super(Enum, self).__init__(name)
        self.values = set(values) if values else set()


class Message(Type):
    def __init__(self, name, variables=None, base=None, declared_fields=None):
        super(Message, self).__init__(name, variables)

        self.base = None
        self.declared_fields = SymbolTable()
        self.fields = SymbolTable()

        if base:
            self.set_base(base)

        if declared_fields:
            self.add_fields(*declared_fields)

    def set_base(self, base):
        self.base = base

    def add_fields(self, *fields):
        for field in fields:
            self.declared_fields.add(field)

    def link(self, parent):
        super(Message, self).link(parent)

        if self.base:
            self.base.link(self)

        for field in self.declared_fields:
            field.link(self)

    def _create_special(self, arg_map):
        svars = tuple(var.specialize(arg_map) for var in self.variables)
        sbase = self.base.special(arg_map) if self.base else None
        sfields = [field.specialize(arg_map) for field in self.declared_fields]
        return Message(self.name, variables=svars, base=sbase, declared_fields=sfields)


class Field(Node):
    def __init__(self, name, type):
        super(Field, self).__init__(name)
        self.type = type

    def link(self, message):
        super(Field, self).link(message)
        self.type.link(self)

    def specialize(self, arg_map):
        stype = self.type.specialize(arg_map)
        if stype == self.type:
            return self

        return Field(self.name, stype)


class EnumValue(Node):
    def __init__(self, type, value):
        super(EnumValue, self).__init__(value)
        self.type = type
        self.value = value

    def link(self, parent):
        super(EnumValue, self).link(parent)
        self.type.link(self)

        if not isinstance(self.type, Enum):
            self.error('wrong type %s, must be an enum', self.type)
            return

        if self.value not in self.type.value_set:
            self.error('enum value "%s" is not found', self.value)
            return
