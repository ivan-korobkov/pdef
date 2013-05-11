# encoding: utf-8
from collections import OrderedDict
from pdef import ast
from pdef.common import Type, PdefException
from pdef.preconditions import *


class Pdef(object):
    def __init__(self):
        self.modules = SymbolTable()

    def add_module(self, module):
        '''Adds a new module to Pdef.'''
        check_argument(module.pdef is None, '%s is already added to another pdef instance', module)
        module.pdef = self
        self.modules.add(module)

    def get_module(self, name):
        '''Returns a module by its name, or raises and exception.'''
        module = self.modules.get(name)
        if not module: raise PdefException('%s: module %r is not found', self, name)
        return module


class Module(object):
    @classmethod
    def from_ast(cls, node):
        '''Creates a new module from an AST node.'''
        module = Module(node.name)
        module._node = node

        for def_node in node.definitions:
            def0 = Definition.from_ast_polymorphic(def_node)
            module.add_definition(def0)

        return module

    def __init__(self, name):
        self.name = name
        self.definitions = SymbolTable(self)
        self.imported_definitions = SymbolTable(self)
        self.pdef = None

        self._node = None
        self._imports_linked = False
        self._defs_linked = False

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.name)

    def __str__(self):
        return self.name

    def link_imports(self):
        '''Links this method imports, must be called before link_definitions().'''
        if self._imports_linked: return
        self._imports_linked = True
        if not self._node: return

        pdef = self.pdef
        check_state(pdef is not None, '%s: cannot link, pdef is required', self)

        for node in self._node.imports:
            module = pdef.get_module(node.module_name)
            for name in node.names:
                try:
                    def0 = module.get_definition(name)
                except PdefException:
                    raise PdefException('%s: import %r is not found in %s' % (self, name, module))
                self.add_import(def0)

    def link_definitions(self):
        '''Links this module definitions, must be called after link_imports().'''
        if self._defs_linked: return
        self._defs_linked = True

        for definition in self.definitions.values():
            definition.link()

    def add_import(self, definition):
        '''Adds an imported definition to this module.'''
        check_isinstance(definition, Definition)
        self.imported_definitions.add(definition)

    def add_definition(self, definition):
        '''Adds a new definition to this module.'''
        check_isinstance(definition, Definition)
        check_argument(definition.module is None, '%s is already added to another module', definition)
        check_argument(definition.name not in self.imported_definitions,
                       '%s: definition %r clashes with an import' % (self, definition.name))

        definition.module = self
        self.definitions.add(definition)

    def add_definitions(self, *definitions):
        '''Adds all definitions to this module.'''
        map(self.add_definition, definitions)

    def get_definition(self, name):
        '''Returns a definition by its name, or raises an exception.'''
        def0 = self.definitions.get(name)
        if not def0: raise PdefException('%s: definitions %r is not found' % (self, name))
        return def0

    def lookup(self, ref_or_def):
        '''Lookups a definition if a reference, then links the definition, and returns it.'''
        if isinstance(ref_or_def, Definition):
            def0 = ref_or_def
        elif isinstance(ref_or_def, ast.Ref):
            def0 = self._lookup_ref(ref_or_def)
        else:
            raise PdefException('%s: unsupported lookup reference or definition %s' %
                                (self, ref_or_def))

        def0.link()
        return def0

    def _lookup_ref(self, ref):
        def0 = Values.get_by_type(ref.type)
        if def0: return def0 # It's a simple value.

        t = ref.type
        if t == Type.LIST: return List(ref.element, module=self)
        elif t == Type.SET: return Set(ref.element, module=self)
        elif t == Type.MAP: return Map(ref.key, ref.value, module=self)
        elif t == Type.ENUM_VALUE:
            enum = self.lookup(ref.enum)
            value = enum.values.get(ref.value)
            if not value:
                raise PdefException('%s: enum value "%s" is not found' % (self, ref))
            return value

        # It must be an import or a user defined type.
        name = ref.name
        if name in self.imported_definitions: return self.imported_definitions[name]
        if name in self.definitions: return self.definitions[name]
        raise PdefException('%s: type "%s" is not found' % (self, ref))


class Definition(object):
    @classmethod
    def from_ast_polymorphic(cls, node):
        '''Creates a new definition from an AST node, supports enums, messages and interfaces.'''
        if node.type == Type.ENUM:
            return Enum.from_ast(node)
        elif node.type == Type.MESSAGE:
            return Message.from_ast(node)
        elif node.type == Type.INTERFACE:
            return Interface.from_ast(node)

        raise ValueError('Unsupported definition node %s' % node)

    def __init__(self, type, name):
        self.type = type
        self.name = name
        self.module = None
        self._linked = False

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    def __str__(self):
        return self.fullname

    @property
    def fullname(self):
        if self.module:
            return '%s.%s' % (self.module.name, self.name)
        return self.name

    def link(self):
        if self._linked:
            return

        self._linked = True
        self._link()

    def _link(self):
        pass


class Value(Definition):
    '''Value definition.'''
    def __init__(self, type):
        super(Value, self).__init__(type, type)
        self.type = type


class Values(object):
    '''Value definition singletons.'''
    BOOL = Value(Type.BOOL)
    INT16 = Value(Type.INT16)
    INT32 = Value(Type.INT32)
    INT64 = Value(Type.INT64)
    FLOAT = Value(Type.FLOAT)
    DOUBLE = Value(Type.DOUBLE)
    DECIMAL = Value(Type.DECIMAL)
    DATE = Value(Type.DATE)
    DATETIME = Value(Type.DATETIME)
    STRING = Value(Type.STRING)
    UUID = Value(Type.UUID)

    OBJECT = Value(Type.OBJECT)
    VOID = Value(Type.VOID)

    _BY_TYPE = None

    @classmethod
    def get_by_type(cls, t):
        '''Returns a value by its type or none.'''
        if cls._BY_TYPE is None:
            cls._BY_TYPE = {}
            for k, v in cls.__dict__.items():
                if not isinstance(v, Value): continue
                cls._BY_TYPE[v.type] = v

        return cls._BY_TYPE.get(t)


class List(Definition):
    def __init__(self, element, module=None):
        super(List, self).__init__(Type.LIST, 'List')
        self.element = element
        self.module = module

    def _link(self):
        self.element = self.module.lookup(self.element)


class Set(Definition):
    def __init__(self, element, module=None):
        super(Set, self).__init__(Type.SET, 'Set')
        self.element = element
        self.module = module

    def _link(self):
        self.element = self.module.lookup(self.element)


class Map(Definition):
    def __init__(self, key, value, module=None):
        super(Map, self).__init__(Type.MAP, 'Map')
        self.key = key
        self.value = value
        self.module = module

    def _link(self):
        self.key = self.module.lookup(self.key)
        self.value = self.module.lookup(self.value)


class Enum(Definition):
    @classmethod
    def from_ast(cls, node):
        check_isinstance(node, ast.Enum)
        return Enum(node.name, *node.values)

    def __init__(self, name, *values):
        super(Enum, self).__init__(Type.ENUM, name)
        self.values = SymbolTable(self)
        if values:
            self.add_values(*values)

    def add_value(self, value_name):
        '''Creates a new enum value by its name, adds it to this enum, and returns it.'''
        value = EnumValue(self, value_name)
        self.values.add(value)
        return value

    def add_values(self, *value_names):
        map(self.add_value, value_names)

    def __contains__(self, item):
        return item in self.values.values()


class EnumValue(Definition):
    '''Single enum value which has a name and a pointer to the declaring enum.'''
    def __init__(self, enum, name):
        super(EnumValue, self).__init__(Type.ENUM_VALUE, name)
        self.enum = enum
        self.name = name


class Message(Definition):
    '''User-defined message.'''
    @classmethod
    def from_ast(cls, node):
        '''Creates a new unlinked message from an AST node.'''
        check_isinstance(node, ast.Message)
        msg = Message(node.name)
        msg._node = node
        return msg

    def __init__(self, name, is_exception=False):
        super(Message, self).__init__(Type.MESSAGE, name)
        self.is_exception = is_exception

        self.base = None
        self.base_type = None
        self.subtypes = OrderedDict()
        self._discriminator_field = None

        self.fields = SymbolTable(self)
        self.declared_fields = SymbolTable(self)
        self.inherited_fields = SymbolTable(self)

        self._node = None

    @property
    def discriminator_field(self):
        return self._discriminator_field if self._discriminator_field \
            else self.base.discriminator_field if self.base else None

    def set_base(self, base, base_type):
        '''Sets this message base and inherits its fields.'''
        check_isinstance(base, Message)
        check_isinstance(base_type, EnumValue)
        check_argument(self != base, '%s: cannot inherit itself', self)
        check_argument(self not in base._bases, '%s: circular inheritance with %s', self, base)
        check_argument(self.is_exception == base.is_exception, '%s: cannot inherit %s', self,
                       base.fullname)

        self.base = base
        self.base_type = base_type
        base._add_subtype(self)

        for field in base.fields.values():
            self.inherited_fields.add(field)
            self.fields.add(field)

    def _add_subtype(self, subtype):
        '''Adds a new subtype to this message, checks its base_type.'''
        check_isinstance(subtype, Message)
        check_state(self.discriminator_field, '%s: is not polymorphic, no discriminator field', self)
        check_argument(subtype.base_type in self.discriminator_field.type)
        check_argument(subtype.base_type not in self.subtypes, '%s: duplicate subtype %s',
                       self, subtype.base_type)

        self.subtypes[subtype.base_type] = subtype
        if self.base and self.base.discriminator_field == self.discriminator_field:
            self.base._add_subtype(subtype)

    def add_field(self, name, definition, is_discriminator=False):
        '''Adds a new field to this message and returns the field.'''
        field = Field(self, name, definition, is_discriminator)
        self.declared_fields.add(field)
        self.fields.add(field)

        if is_discriminator:
            check_state(not self.discriminator_field, '%s: duplicate discriminator field', self)
            check_argument(isinstance(field.type, Enum), '%s: discriminator field %s must be an enum',
                           self, field)
            check_state(not self.subtypes,
                        '%s: discriminator field must be set before adding subtypes', self)
            self._discriminator_field = field
        return field

    def _link(self):
        '''Initializes this message from its AST node if present.'''
        node = self._node
        if not node: return

        module = self.module
        check_state(module, '%: cannot link, module is required', self)

        if node.base:
            base = module.lookup(node.base)
            base_type = module.lookup(node.base_type) if node.base_type else None
            self.set_base(base, base_type)

        for field_node in node.fields:
            fname = field_node.name
            ftype = module.lookup(field_node.type)
            self.add_field(fname, ftype, field_node.is_discriminator)

    @property
    def _bases(self):
        '''Internal, returns all this message bases.'''
        bases = []

        b = self
        while b.base:
            bases.append(b.base)
            b = b.base

        return bases


class Field(object):
    '''Single message field.'''
    def __init__(self, message, name, type, is_discriminator=False):
        self.message = message
        self.name = name
        self.type = type
        self.is_discriminator = is_discriminator
        check_isinstance(type, Definition)

    def __repr__(self):
        return '%s %s' % (self.name, self.type)

    @property
    def fullname(self):
        return '%s.%s=%s' % (self.message.fullname, self.name, self.type)


class Interface(Definition):
    @classmethod
    def from_ast(cls, node):
        '''Creates a new interface from an AST node.'''
        check_isinstance(node, ast.Interface)
        iface = Interface(node.name)
        iface._node = node
        return iface

    def __init__(self, name):
        super(Interface, self).__init__(Type.INTERFACE, name)

        self.bases = []
        self.methods = SymbolTable(self)
        self.declared_methods = SymbolTable(self)
        self.inherited_methods = SymbolTable(self)

        self._node = None

    def add_base(self, base):
        '''Adds a new base to this interface.'''
        check_isinstance(base, Interface)
        check_argument(base is not self, '%s: self inheritance', self)
        check_argument(base not in self.bases, '%s: duplicate base %s', self, base)
        check_argument(self not in base._all_bases, '%s: circular inheritance with %s', self, base)

        self.bases.append(base)
        for method in base.methods.values():
            self.inherited_methods.add(method)
            self.methods.add(method)

    def add_method(self, name, result=Values.VOID, *args_tuples):
        '''Adds a new method to this interface and returns the method.'''
        method = Method(self, name, result, args_tuples)
        self.declared_methods.add(method)
        self.methods.add(method)
        return method

    def _link(self):
        '''Initializes this interface from its AST node if present.'''
        node = self._node
        if not node: return

        module = self.module
        check_state(module, '%: cannot link, module is required', self)

        for base_node in node.bases:
            base = module.lookup(base_node)
            self.add_base(base)

        for method_node in node.methods:
            method_name = method_node.name
            result = module.lookup(method_node.result)
            args = []
            for arg_node in method_node.args:
                arg_name = arg_node.name
                arg_type = module.lookup(arg_node.type)
                args.append((arg_name, arg_type))

            self.add_method(method_name, result, *args)

    @property
    def _all_bases(self):
        '''Internal, returns all bases including the ones from the inherited interfaces.'''
        bases = []
        for b in self.bases:
            bases.append(b)
            bases.extend(b._all_bases)
        return bases


class Method(object):
    def __init__(self, interface, name, result, args_tuples=None):
        self.interface = interface
        self.name = name
        self.result = result
        self.args = SymbolTable(self)
        for arg_name, arg_def in args_tuples:
            self.args.add(MethodArg(arg_name, arg_def))

        check_isinstance(result, Definition)

    def __repr__(self):
        return '%s%s=>%s' % (self.name, self.args, self.result)

    @property
    def fullname(self):
        return '%s.%s(%s)=>%s' % (self.interface.fullname, self.name,
                                  ', '.join(str(a) for a in self.args.values()), self.result)


class MethodArg(object):
    def __init__(self, name, definition):
        self.name = name
        self.type = definition
        check_isinstance(definition, Definition)

    def __repr__(self):
        return '%s %s' % (self.name, self.type)


class SymbolTable(OrderedDict):
    '''SymbolTable is an ordered dict which supports adding items using item.name as a key,
    and prevents duplicate items.'''
    def __init__(self, parent=None, *args, **kwds):
        super(SymbolTable, self).__init__(*args, **kwds)
        self.parent = parent

    def add(self, item):
        '''Adds an item by with item.name as the key.'''
        self[item.name] = item

    def __setitem__(self, key, value):
        check_state(key not in self, '%s: duplicate item %s', self, key)
        super(SymbolTable, self).__setitem__(key, value)
