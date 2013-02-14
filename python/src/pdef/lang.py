# encoding: utf-8
from collections import deque
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

    @property
    def package(self):
        if self.parent is None:
            raise ValueError('Can\'t access the package, %s has no parent' % self)
        return self.parent.package

    def error(self, msg, *args):
        msg = '%s: %s' % (self.fullname, msg % args)
        logging.error(msg)

        # The parent can be absent if the node is not linked.
        if self.parent:
            self.parent.errors.append(msg)
        else:
            self.errors.append(msg)

    def symbol(self, name):
        if self.parent:
            return self.parent.symbol(name)

    def link(self):
        pass


class Package(Node):
    def __init__(self, name, builtin=None):
        super(Package, self).__init__(name)

        self.modules = SymbolTable()
        self.builtin = builtin
        self.parameterized = {}
        self.pqueue = deque()

    @property
    def package(self):
        return self

    def add_modules(self, *modules):
        for module in modules:
            self.modules.add(module)
            module.parent = self

    def link(self):
        for module in self.modules:
            module.link_imports()

        for module in self.modules:
            module.link()

    def link_specials(self):
        while 1:
            if not len(self.pqueue):
                break

            sp = self.pqueue.pop()
            sp.build()

    def symbol(self, name):
        '''Find a globally accessible builtin.'''
        if not self.builtin:
            return

        if '.' in name:
            return

        for module in self.builtin.modules:
            if name in module.definitions:
                return module.definitions[name]

        return None

    def parameterized_symbol(self, rawtype, args):
        check_argument(len(rawtype.variables) == len(args), "wrong number of args %s", args)

        args = tuple(args)
        key = (rawtype, args)
        if key in self.parameterized:
            return self.parameterized[key]

        # Create a proxy to allow parameterized circular references, i.e.:
        # Node<V>:
        #   Node<V> next
        ptype = ParameterizedType(rawtype, args, parent=self)
        self.parameterized[key] = ptype
        self.pqueue.append(ptype)

        return ptype


class Proxy(Node):
    def __init__(self, name):
        super(Proxy, self).__init__(name)
        self.delegate = None

    def __repr__(self):
        if self.delegate:
            return '@' + repr(self.delegate)
        return super(Proxy, self).__repr__()

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
        check_state(self.delegate is not None, 'Delegate is not set in %s', self)


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
            imp.parent = self

    def add_definitions(self, *definitions):
        for d in definitions:
            self.definitions.add(d)
            d.parent = self

    def link_imports(self):
        for imp in self.imports:
            imp.link()

    def link(self):
        for definition in self.definitions:
            definition.link()

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

    def link(self):
        package = self.package
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
            arg.parent = self

    def link(self):
        for arg in self.args:
            arg.link()

        self.delegate = self._lookup()

    def _lookup(self):
        # Find the rawtype by its name, for example: MyType, package.AnotherType, T (variable).
        rawtype = self.symbol(self.name)
        if not rawtype:
            self.error('type not found "%s"', self.name)
            return

        if not rawtype.variables:
            # Rawtype is not generic.
            return rawtype

        return self.package.parameterized_symbol(rawtype, self.args)


class ParameterizedType(Proxy):
    def __init__(self, rawtype, args, parent):
        super(ParameterizedType, self).__init__(rawtype.name)
        self.rawtype = rawtype
        self.args = args
        self.arg_map = dict((var, arg) for var, arg in zip(rawtype.variables, args))
        self.parent = parent

    def __eq__(self, other):
        return self is other

    def __hash__(self):
        return object.__hash__(self)

    def build(self):
        self.delegate = self.rawtype.parameterize(self.args)

    def parameterize(self, args):
        raise ValueError('Only rawtype can be parameterized')

    def bind(self, arg_map):
        bargs = []
        for arg in self.args:
            barg = arg.bind(arg_map)
            bargs.append(barg)

        return self.package.parameterized_symbol(self.rawtype, bargs)


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
            var.parent = self

    def symbol(self, name):
        if name in self.variables:
            return self.variables[name]

        return super(Type, self).symbol(name)

    def link(self):
        for var in self.variables:
            var.link()

    def parameterize(self, args):
        '''Create a parameterized type.'''
        raise NotImplementedError('Implement in a subclass')

    def bind(self, arg_map):
        '''Only parameterized types and variables can be bound.'''
        return self


class Variable(Type):
    def __init__(self, name):
        super(Variable, self).__init__(name)

    def bind(self, arg_map):
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

    def parameterize(self, args):
        check_argument(len(self.variables) == len(args), 'wrong number of args')

        special = Native(self.name, variables=args, options=self.options)
        special.rawtype = self
        return special


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
        if isinstance(base, Reference):
            base.parent = self

    def add_fields(self, *fields):
        for field in fields:
            self.declared_fields.add(field)
            field.parent = self

    def link(self):
        if self.base:
            self.base.link()

        for field in self.declared_fields:
            field.link()

    def parameterize(self, args):
        check_argument(len(self.variables) == len(args), 'wrong number of args')
        arg_map = dict((var, arg) for var, arg in zip(self.variables, args))

        bbase = self.base.bind(arg_map) if self.base else None
        bfields = [field.bind(arg_map) for field in self.declared_fields]

        special = Message(self.name, variables=args, base=bbase, declared_fields=bfields)
        special.rawtype = self
        return special


class Field(Node):
    def __init__(self, name, type):
        super(Field, self).__init__(name)
        self.type = type
        if isinstance(type, Reference):
            type.parent = self

    def link(self):
        self.type.link()

    def bind(self, arg_map):
        btype = self.type.bind(arg_map)
        if btype == self.type:
            return self

        return Field(self.name, btype)


class EnumValue(Node):
    def __init__(self, type, value):
        super(EnumValue, self).__init__(value)
        self.type = type
        self.value = value

    def link(self):
        self.type.link()

        if not isinstance(self.type, Enum):
            self.error('wrong type %s, must be an enum', self.type)
            return

        if self.value not in self.type.value_set:
            self.error('enum value "%s" is not found', self.value)
            return
