# encoding: utf-8
import logging
from pdef import ast
from pdef.consts import Type
from pdef.preconditions import *


class Module(object):
    def __init__(self, name, definitions=None):
        self.name = name
        self.definitions = SymbolTable(self)

        self._node = None
        self._imports_linked = False
        self._defs_linked = False

        if definitions:
            map(self.add_definition, definitions)

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.name)

    def link_imports(self):
        if self._imports_linked: return
        self._imports_linked = True
        if not self._node: return
        #        for node in self._ast.imports:
    #            imported = self.package.lookup(node.name)
    #            if not imported:
    #                raise ValueError('Import not found "%s"' % node.name)
    #
    #            self.add_import(imported, node.alias)

    def link_definitions(self):
        if self._defs_linked: return
        self._defs_linked = True

        for definition in self.definitions:
            definition.init()

    def add_definition(self, definition):
        check_isinstance(definition, Definition)

        self.definitions.add(definition)
        logging.info('%s: added "%s"', self, definition.name)

    def add_definitions(self, *definitions):
        map(self.add_definition, definitions)

    def lookup(self, ref_or_def):
        '''Lookups a definition if a reference, then links the definition, and returns it.'''
        if isinstance(ref_or_def, Definition):
            def0 = ref_or_def
        elif isinstance(ref_or_def, ast.Ref):
            def0 = self._lookup_ref(ref_or_def)
        else:
            raise ValueError('%s: unsupported lookup reference or definition %s' % (self, ref_or_def))

        def0.link()
        return def0

    def _lookup_ref(self, ref):
        def0 = Values.get_by_type(ref.type)
        if def0: return def0 # It's a simple value.

        t = ref.type
        if t == Type.LIST: return List(ref.element, module=self)
        elif t == Type.SET: return Set(ref.element, module=self)
        elif t == Type.MAP: return Map(ref.key, ref.value, module=self)

        # It must be an import or a user defined type.
        def0 = self.definitions.get(ref.name)
        if def0 : return def0

        raise ValueError('%s: type "%s" is not found' % (self, ref))


class Definition(object):
    def __init__(self, type, name, module=None):
        self.type = type
        self.name = name
        self.module = module
        self._linked = False

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    @property
    def fullname(self):
        if self.module:
            return '%s %s' % (self.module.name, self.name)
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

    _BY_TYPE = {
        Type.BOOL: BOOL,
        Type.INT16: INT16,
        Type.INT32: INT32,
        Type.INT64: INT64,
        Type.FLOAT: FLOAT,
        Type.DOUBLE: DOUBLE,
        Type.DECIMAL: DECIMAL,
        Type.DATE: DATE,
        Type.DATETIME: DATETIME,
        Type.STRING: STRING,
        Type.UUID: UUID,
        Type.OBJECT: OBJECT,
        Type.VOID: VOID
    }

    @classmethod
    def get_by_type(cls, t):
        '''Returns a value by its type or none.'''
        return cls._BY_TYPE.get(t)


class List(Definition):
    def __init__(self, element, module=None):
        super(List, self).__init__(Type.LIST, 'List', module=module)
        self.element = element

    def _link(self):
        self.element = self.module.lookup(self.element)


class Set(Definition):
    def __init__(self, element, module=None):
        super(Set, self).__init__(Type.SET, 'Set', module=module)
        self.element = element

    def _link(self):
        self.element = self.module.lookup(self.element)


class Map(Definition):
    def __init__(self, key, value, module=None):
        super(Map, self).__init__(Type.MAP, 'Map', module=module)
        self.key = key
        self.value = value

    def _link(self):
        self.key = self.module.lookup(self.key)
        self.value = self.module.lookup(self.value)


class Enum(Definition):
    @classmethod
    def from_ast(cls, node, module=None):
        check_isinstance(node, ast.Enum)
        return Enum(node.name, values=node.values, module=module)

    def __init__(self, name, values=None, module=None):
        super(Enum, self).__init__(Type.ENUM, name, module=module)
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


class EnumValue(object):
    '''Single enum value which has a name and a pointer to the declaring enum.'''
    def __init__(self, enum, name):
        self.enum = enum
        self.name = name


class Message(Definition):
    '''User-defined message.'''
    @classmethod
    def from_ast(cls, node, module=None):
        '''Creates a new unlinked message from an AST node.'''
        check_isinstance(node, ast.Message)
        msg = Message(node.name, module=module)
        msg._node = node
        return msg

    def __init__(self, name, is_exception=False, module=None):
        super(Message, self).__init__(Type.MESSAGE, name, module=module)
        self.is_exception = is_exception

        self.base = None
        self.subtypes = None

        self.fields = SymbolTable(self)
        self.declared_fields = SymbolTable(self)

        self._node = None

    def set_base(self, base, base_type=None):
        check_isinstance(base, Message)
        if base_type: check_isinstance(base_type, EnumValue)

        check_argument(self != base, '%s: cannot inherit itself', self)
        check_argument(self not in base._bases, '%s: circular inheritance %s',
                       self, '->'.join(b.fullname for b in [self, base] + base._bases))
        check_argument(self.is_exception == base.is_exception, '%s: cannot inherit %s',
                       self, base.fullname)

        self.base = base
        self.base_type = base_type

    @property
    def _bases(self):
        bases = []

        b = self
        while b.base:
            bases.append(b.base)
            b = b.base

        return bases

    def add_field(self, name, definition):
        '''Adds a new field to this message.'''
        field = Field(name, definition)
        self.declared_fields.add(field)

    def _link(self):
        '''Initializes this message from its AST node if it's present.'''
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
            self.add_field(fname, ftype)


class Field(object):
    '''Single message field.'''
    def __init__(self, name, type):
        self.name = name
        self.type = type
        check_isinstance(type, Definition)

    def __repr__(self):
        return '<%s %s>' % (self.name, self.type)


class SymbolTable(object):
    def __init__(self, parent=None):
        self.parent = parent
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

    def __setitem__(self, name, item):
        if name in self.map:
            s = 'duplicate symbol "%s"' % name
            if self.parent:
                s = '%s: %s' % (self.parent, s)
            raise ValueError(s)

        self.map[name] = item
        self.items.append(item)

    def __add__(self, other):
        new = SymbolTable()
        new += self
        new += other
        return new

    def __iadd__(self, other):
        for item in other:
            self.add(item)
        return self

    def add(self, item, name=None):
        name = name if name else item.name
        self[name] = item

    def get(self, name, default=None):
        return self.map.get(name, default)

    def as_map(self):
        return dict(self.map)
