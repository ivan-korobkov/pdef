# encoding: utf-8
from collections import deque
import logging
from pdef.preconditions import *
from pdef.walker import Walker


class Node(object):
    def __init__(self, name):
        self.name = name
        self.parent = None
        self.children = []
        self.errors = []

    def __repr__(self):
        return '<%s %s %s>' % (self.__class__.__name__, self.fullname, hex(id(self)))

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


class Builder(object):
    def __init__(self, root):
        self.root = root
        self.walker = Walker(root)

    def build(self):
        self.link_module_refs()
        self.link_refs()
        self.build_ptypes()

    def link_module_refs(self):
        for module_ref in self.walker.module_refs():
            module_ref.link()

    def link_refs(self):
        for ref in self.walker.refs():
            ref.link()

    def build_ptypes(self):
        # Parameterized types are created only in the package their are defined in.
        # So, after building all ptypes from the package, no other ptypes will be created in it.
        for pkg in self.walker.packages():
            pkg.build_parameterized()


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
            self.children.append(module)
            module.parent = self

    def build(self):
        builder = Builder(self)
        builder.build()

    def build_parameterized(self):
        while len(self.pqueue) > 0:
            ptype = self.pqueue.pop()
            ptype.build()

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

    def parameterized_symbol(self, rawtype, *variables):
        variables = tuple(variables)
        key = (rawtype, variables)
        if key in self.parameterized:
            return self.parameterized[key]

        if tuple(rawtype.variables) == variables:
            self.parameterized[key] = rawtype
            return rawtype

        # Create a proxy to allow parameterized circular references, i.e.:
        # Node<V>:
        #   Node<V> next
        ptype = ParameterizedType(rawtype, *variables)
        ptype.parent = self

        self.parameterized[key] = ptype
        self.pqueue.append(ptype)
        self.children.append(ptype)

        return ptype


class Proxy(Node):
    def __init__(self, name):
        super(Proxy, self).__init__(name)
        self.delegate = None

    @property
    def fullname(self):
        if self.delegate:
            return self.delegate.fullname
        return super(Proxy, self).fullname

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
    def __init__(self):
        self.items = []
        self.map = {}

    def __eq__(self, other):
        if not isinstance(other, SymbolTable):
            return False
        return self.items == other.items

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

    def as_map(self):
        return dict(self.map)


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
            self.children.append(imp)
            imp.parent = self

    def add_definitions(self, *definitions):
        for d in definitions:
            self.definitions.add(d)
            self.children.append(d)
            d.parent = self

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
        self.children.append(self.delegate)


class Reference(Proxy):
    def __init__(self, name, *generic_variables):
        super(Reference, self).__init__(name)
        self.variables = []
        self.add_variables(*generic_variables)

    def add_variables(self, *variables):
        for arg in variables:
            self.variables.append(arg)
            self.children.append(arg)
            arg.parent = self

    def link(self):
        for arg in self.variables:
            arg.link()

        self.delegate = self._lookup()
        self.children.append(self.delegate)

    def _lookup(self):
        # Find the rawtype by its name, for example: MyType, package.AnotherType, T (variable).
        rawtype = self.symbol(self.name)
        if not rawtype:
            self.error('type not found "%s"', self.name)
            return

        if not rawtype.variables:
            # Rawtype is not generic.
            return rawtype

        return self.package.parameterized_symbol(rawtype, *self.variables)


class ParameterizedType(Proxy):
    def __init__(self, rawtype, *variables):
        super(ParameterizedType, self).__init__(rawtype.name)
        check_argument(len(rawtype.variables) == len(variables),
                       "wrong number of variables %s", variables)

        self.rawtype = rawtype
        self.variables = SymbolTable()
        for var, arg in zip(self.rawtype.variables, variables):
            self.variables.add_with_name(var, arg)

    def __eq__(self, other):
        return self is other

    def __hash__(self):
        return object.__hash__(self)

    def build(self):
        self.delegate = self.rawtype.parameterize(*self.variables)
        self.children.append(self.delegate)

    def bind(self, arg_map):
        bvariables = []
        for arg in self.variables:
            barg = arg.bind(arg_map)
            bvariables.append(barg)

        return self.package.parameterized_symbol(self.rawtype, *bvariables)


class Type(Node):
    def __init__(self, name, variables=None):
        super(Type, self).__init__(name)

        self.rawtype = self
        self.variables = SymbolTable()

        if variables:
            self.add_variables(*variables)

    @property
    def fullname(self):
        s = super(Type, self).fullname
        if self.variables:
            s += '<' + ', '.join(var.name for var in self.variables) + '>'
        return s

    def add_variables(self, *vars):
        for var in vars:
            self.variables.add(var)
            self.children.append(var)
            var.parent = self

    def symbol(self, name):
        if name in self.variables:
            return self.variables[name]

        return super(Type, self).symbol(name)

    def parameterize(self, *variables):
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

        self.error('variable is not found in the variables map')


class Native(Type):
    def __init__(self, name, variables=None, options=None):
        super(Native, self).__init__(name, variables)
        # TODO: java_type
        #self.java_type = options.java_type
        self.options = options

    def parameterize(self, *variables):
        if len(self.variables) != len(variables):
            self.error('wrong number of arguments %s', variables)
            return

        special = Native(self.name, variables=variables, options=self.options)
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
        self.children.append(base)

        if isinstance(base, Reference):
            base.parent = self

    def add_fields(self, *fields):
        for field in fields:
            self.declared_fields.add(field)
            self.children.append(field)
            field.parent = self

    def parameterize(self, *variables):
        if len(self.variables) != len(variables):
            self.error('wrong number of variables %s', variables)
            return

        arg_map = dict((var, arg) for var, arg in zip(self.variables, variables))
        bbase = self.base.bind(arg_map) if self.base else None
        bfields = [field.bind(arg_map) for field in self.declared_fields]

        special = Message(self.name, variables=variables, base=bbase, declared_fields=bfields)
        special.rawtype = self
        return special


class Field(Node):
    def __init__(self, name, type):
        super(Field, self).__init__(name)
        self.type = type
        self.children.append(type)

        if isinstance(type, Reference):
            type.parent = self

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
