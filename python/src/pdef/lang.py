# encoding: utf-8
from collections import deque, OrderedDict
import logging
from pdef.preconditions import *
from pdef.walker import Walker


class Node(object):
    def __init__(self):
        self.parent = None
        self.children = []
        self.symbols = SymbolTable()
        self.errors = []

    def __repr__(self):
        return '<%s %s %s>' % (self.__class__.__name__, self.fullname, hex(id(self)))

    def _add_child(self, child, always_parent=True):
        if child is None:
            return

        self.children.append(child)
        if always_parent or isinstance(child, Ref):
            child.parent = self

    def _add_symbol(self, symbol):
        check_isinstance(symbol, Symbol, '%s is not a Symbol', symbol)
        self._add_child(symbol)
        self.symbols.add(symbol)

    @property
    def fullname(self):
        if self.parent:
            return '%s %s' % (self.parent.fullname, self.__class__.__name__)
        return self.__class__.__name__

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

    def lookup(self, name):
        symbol = self._lookup_child(name)
        if symbol is not None:
            return symbol

        if self.parent:
            return self.parent.lookup(name)

    def _lookup_child(self, name):
        if name in self.symbols:
            return self.symbols[name]

        if '.' not in name:
            return

        child_parts = deque(name.split('.'))
        parent_parts = [child_parts.popleft()]

        while child_parts:
            parent_name = '.'.join(parent_parts)
            if parent_name not in self.symbols:
                parent_parts.append(child_parts.popleft())
                continue

            child_name = '.'.join(child_parts)
            parent = self.symbols[parent_name]
            return parent._lookup_child(child_name)


class Symbol(Node):
    def __init__(self, name):
        super(Symbol, self).__init__()
        self.name = name

    @property
    def fullname(self):
        if self.parent:
            return '%s.%s' % (self.parent.fullname, self.name)

        return self.name


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


class Builder(object):
    def __init__(self, root):
        self.root = root
        self.walker = Walker(root)

    def build(self):
        self.link_module_refs()
        self.link_refs()
        self.built_pmessages()
        self.check_circular_inheritance()
        self.compfile_fields()

    def link_module_refs(self):
        for module_ref in self.walker.module_refs():
            module_ref.link()

    def link_refs(self):
        for ref in self.walker.refs():
            ref.link()

    def check_circular_inheritance(self):
        for message in self.walker.messages():
            message.check_circular_inheritance()

    def built_pmessages(self):
        # Parameterized types are created only in the package their are defined in.
        # So, after building all ptypes from the package, no other ptypes will be created in it.
        for pkg in self.walker.packages():
            pkg.build_parameterized()

    def compfile_fields(self):
        for message in self.walker.messages():
            message.compile_fields()


class Package(Symbol):
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
            if not module.name.startswith(self.name):
                module.error('module name must start with the package name "%s"', self.name)
                continue

            self.modules.add(module)
            self._add_symbol(module)

    def build(self):
        builder = Builder(self)
        builder.build()

    def build_parameterized(self):
        while self.pqueue:
            ptype = self.pqueue.pop()
            if isinstance(ptype, ParameterizedMessage):
                ptype.build()

    def lookup(self, name):
        '''Find a globally accessible builtin.'''
        if not self.builtin or '.' in name:
            return

        for module in self.builtin.modules:
            symbol = module.lookup(name)
            if symbol:
                return symbol

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
        ptype = rawtype.parameterize(*variables)

        self.parameterized[key] = ptype
        self.pqueue.append(ptype)
        return ptype


class Module(Symbol):
    def __init__(self, name, imports=None, definitions=None):
        super(Module, self).__init__(name)

        self.imports = SymbolTable()
        self.definitions = SymbolTable()

        if imports:
            self.add_imports(*imports)

        if definitions:
            self.add_definitions(*definitions)

    @property
    def fullname(self):
        return self.name

    def add_imports(self, *imports):
        for imp in imports:
            self.imports.add(imp)
            self._add_symbol(imp)

    def add_definitions(self, *definitions):
        for d in definitions:
            self.definitions.add(d)
            self._add_symbol(d)


class AbstractRef(Symbol):
    def __init__(self, name):
        super(AbstractRef, self).__init__(name)
        self.delegate = None

    @property
    def fullname(self):
        if self.delegate:
            return self.delegate.fullname
        return super(AbstractRef, self).fullname

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


class ImportRef(AbstractRef):
    def __init__(self, import_name, alias=None):
        super(ImportRef, self).__init__(alias if alias else import_name)
        self.import_name = import_name

    def link(self):
        package = self.package
        if not self.import_name in package.modules:
            self.error('import not found "%s"', self.import_name)
            return

        self.delegate = package._lookup_child(self.import_name)
        self._add_child(self.delegate, always_parent=False)

    def lookup(self, name):
        self._check_delegate()
        return self.delegate.lookup(name)

    def _lookup_child(self, name):
        self._check_delegate()
        return self.delegate._lookup_child(name)


class Ref(AbstractRef):
    def __init__(self, name, *generic_variables):
        super(Ref, self).__init__(name)
        self.variables = []
        self.add_variables(*generic_variables)

    @property
    def fullname(self):
        if self.parent:
            return '%s in %s' % (self.name, self.parent.fullname)
        return self.name

    def add_variables(self, *variables):
        for arg in variables:
            self.variables.append(arg)
            self._add_child(arg)

    def link(self):
        for arg in self.variables:
            arg.link()

        self.delegate = self._lookup_delegate()
        self._add_child(self.delegate, always_parent=False)

    def _lookup_delegate(self):
        rawtype = self.lookup(self.name)
        if not rawtype:
            self.error('type not found "%s"', self.name)
            return

        if not rawtype.variables:
            # Rawtype is not generic.
            return rawtype

        ptype = self.package.parameterized_symbol(rawtype, *self.variables)
        ptype.parent = self.parent
        return ptype


class Type(Symbol):
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
            self._add_symbol(var)

    def parameterize(self, *variables):
        '''Create a parameterized type.'''
        raise NotImplementedError('Implement in a subclass')

    def bind(self, arg_map):
        '''Parameterized types and variables should redefine this method.'''
        return self


class ParameterizedType(Type):
    def __init__(self, rawtype, *variables):
        super(ParameterizedType, self).__init__(rawtype.name)
        check_argument(len(rawtype.variables) == len(variables),
                       "wrong number of variables %s", variables)

        self.rawtype = rawtype
        self.variables = SymbolTable()
        for var, arg in zip(self.rawtype.variables, variables):
            self.variables.add_with_name(var, arg)
            self._add_child(arg)

    def bind(self, arg_map):
        bvariables = []
        for arg in self.variables:
            barg = arg.bind(arg_map)
            bvariables.append(barg)

        return self.package.parameterized_symbol(self.rawtype, *bvariables)


class Variable(Type):
    def __init__(self, name):
        super(Variable, self).__init__(name)

    def bind(self, arg_map):
        '''Find this variable in the arg map and return the value.'''
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
        '''Parameterize this native with the given variables and return a new one.'''
        if len(self.variables) != len(variables):
            self.error('wrong number of arguments %s', variables)
            return
        return ParameterizedNative(self, *variables)


class ParameterizedNative(ParameterizedType):
    @property
    def options(self):
        return self.rawtype.options


class Enum(Type):
    def __init__(self, name, values):
        super(Enum, self).__init__(name)
        self.values = set(values) if values else set()


class Message(Type):
    def __init__(self, name, variables=None, base=None, base_type=None,
                 polymorphism=None, declared_fields=None):
        super(Message, self).__init__(name, variables)

        self.base = None
        self.base_type = None
        self._polymorphism = None

        self.declared_fields = SymbolTable()
        self.fields = SymbolTable()

        if base:
            self.set_base(base, base_type)

        if polymorphism:
            self.set_polymorphism(polymorphism)

        if declared_fields:
            self.add_fields(*declared_fields)

    def set_base(self, base, base_type):
        '''Set this message inheritance.'''
        check_state(not self.base, 'base is already set in %s', self)

        self.base = check_not_none(base)
        self.base_type = check_not_none(base_type)

        self._add_child(base, always_parent=False)
        self._add_child(base_type, always_parent=False)

    def set_polymorphism(self, polymorphism):
        '''Set this message polymorphism.'''
        check_state(not self.polymorphism, 'polymorphism is already set in %s', self)

        self._polymorphism = check_not_none(polymorphism)
        self._add_child(polymorphism)
        polymorphism.set_message(self)

    @property
    def bases(self):
        '''Return an iterator over the base tree of this message.

        The bases are ordered from this message direct base to the root one.
        '''
        base = self.base
        while base:
            yield base
            base = base.base

    @property
    def polymorphism(self):
        if self._polymorphism:
            return self._polymorphism
        return self.base.polymorphism if self.base else None

    def add_fields(self, *fields):
        for field in fields:
            self.declared_fields.add(field)
            self._add_symbol(field)

    def parameterize(self, *variables):
        '''Parameterize this message with the given arguments, return another message.'''
        if len(self.variables) != len(variables):
            self.error('wrong number of variables %s', variables)
            return
        return ParameterizedMessage(self, *variables)

    def check_circular_inheritance(self):
        '''Check circular inheritance, logs an error if it exists.'''
        seen = OrderedDict()
        seen[self] = True

        base = self.base
        while base:
            if base in seen:
                self.error('circular inheritance %s', seen.keys())
                return

            seen[base] = True
            base = base.base

    def compile_fields(self):
        '''Compile this message and its bases fields.

        The fields are stored in reverse order, from the root base to this message.
        '''
        reversed_bases = reversed(list(self.bases))
        for base in reversed_bases:
            for field in base.declared_fields:
                self.fields.add(field)

        for field in self.declared_fields:
            self.fields.add(field)

    def compile_base_type(self):
        if not self.base:
            return

        if not self.base.polymorphism:
            self.error('base message %s must be polymorphic', self.base)
            return

        self.base.polymorphism.add_subtype(self)


class ParameterizedMessage(ParameterizedType):
    def __init__(self, rawtype, *variables):
        super(ParameterizedMessage, self).__init__(rawtype, *variables)

        self._base = None
        self._declared_fields = None
        self._built = False

    def _check_built(self):
        check_state(self._built, "%s is not built", self)

    @property
    def base(self):
        self._check_built()
        return self._base

    @property
    def base_type(self):
        return self.rawtype.base_type

    @property
    def bases(self):
        # TODO: copy-paste
        base = self.base
        while base:
            yield base
            base = base.base

    @property
    def polymorphism(self):
        return self.rawtype.polymorphism

    @property
    def declared_fields(self):
        self._check_built()
        return self._declared_fields

    def build(self):
        var_map = self.variables.as_map()
        rawtype = self.rawtype

        self._base = rawtype.base.bind(var_map) if rawtype.base else None
        self._declared_fields = SymbolTable()

        for field in rawtype.declared_fields:
            bfield = field.bind(var_map)
            self._declared_fields.add(bfield)
            self._add_symbol(bfield)

        self._built = True


class MessagePolymorphism(Node):
    def __init__(self, field, default_type):
        super(MessagePolymorphism, self).__init__()
        self.field = check_not_none(field)
        self.default_type = check_not_none(default_type)
        self._add_child(field, always_parent=False)

        self.map = {}
        self.message = None

    def set_message(self, message):
        check_state(not self.message, 'message is already set in %s', self)

        self.parent = check_not_none(message)
        self.message = message
        self.map[self.default_type] = message

    def add_subtype(self, subtype):
        check_not_none(subtype)

        base_type = subtype.base_type
        if base_type in self.map:
            self.error('duplicate subtype %s', base_type)
            return

        if self.message not in subtype.bases:
            self.error('%s must inherit %s', subtype, self.message)
            return

        self.map[base_type] = subtype


class Field(Symbol):
    def __init__(self, name, type):
        super(Field, self).__init__(name)
        self.type = type
        self.children.append(type)

        if isinstance(type, Ref):
            type.parent = self

    def bind(self, arg_map):
        '''Bind this field type and return a new field.'''
        btype = self.type.bind(arg_map)
        if btype == self.type:
            return self

        return Field(self.name, btype)
