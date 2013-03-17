# encoding: utf-8
from pdef import ast
from pdef.preconditions import *
from pdef.lang.symbols import SymbolTable
from pdef.lang.types import Type, ParameterizedType, Variable


class Message(Type):
    @classmethod
    def from_node(cls, node, module=None):
        check_isinstance(node, ast.Message)
        message = Message(node.name, variables=(Variable(var) for var in node.variables),
                          module=module)
        message.node = node
        return message

    def __init__(self, name, variables=None, module=None):
        super(Message, self).__init__(name, variables, module)

        self.tree = None
        self.tree_type = None

        self.base = None
        self.base_tree_type = None
        self.bases = None

        self.fields = None
        self.declared_fields = None

        self.node = None

    @property
    def parent(self):
        return self.module if self.module else None

    def init(self):
        if self.inited: return
        self.inited = True
        if not self.node: return
        node = self.node

        # Force base initialization for correct type tree and fields checks.
        base = self.lookup(node.base) if node.base else None
        base.init()
        base_tree_type = self.lookup(node.base_tree_type) if node.base_tree_type else None

        declared_fields = SymbolTable(self)
        for field_node in node.declared_fields:
            name = field_node.name
            type = self.lookup(field_node.type)
            field = Field(name, type)
            declared_fields.add(field)

        tree_type = self.lookup(node.tree_type) if node.tree_type else None
        tree_field_name = node.tree_field
        tree_field = declared_fields.get(tree_field_name)
        if not tree_field:
            raise ValueError('Tree field "%s" is not found in %s' % (tree_field_name, self))

        self.do_init(tree_type=tree_type, tree_field=tree_field,
                     base=base, base_tree_type=base_tree_type,
                     declared_fields=declared_fields)

    def do_init(self, tree_type=None, tree_field=None, base=None, base_tree_type=None,
                declared_fields=None):
        self._set_tree_type(tree_type, tree_field)
        self._set_base(base, base_tree_type)
        self._set_fields(*(declared_fields if declared_fields else ()))

        self.inited = True

    def _set_tree_type(self, tree_type, tree_field):
        if tree_type is None and tree_field is None:
            return
        check_isinstance(tree_type, EnumValue)
        check_isinstance(tree_field, Field)
        check_argument(tree_field.type == tree_type.enum,
                       'Wrong tree value in %s, it must be of type "%s", got "%s"',
                       self, tree_field.type, tree_type)

        self.tree_type = tree_type
        self.tree_field = tree_field
        self.tree = {tree_type: self}

    def _set_base(self, base, base_tree_type):
        '''Set this message base message and its type.'''
        if base is None and base_tree_type is None:
            self.bases = []
            return

        check_isinstance(base, Message)
        check_isinstance(base_tree_type, EnumValue)
        check_argument(base.inited, '%s must be initialized to be used as base of %s', base, self)
        check_argument(self != base, '%s cannot inherit itself', self)
        check_argument(not self in base.bases, 'Circular inheritance: %s',
                       '->'.join(str(b) for b in ([self, base] + list(base.bases))))

        self.base = check_not_none(base)
        self.base_tree_type = check_not_none(base_tree_type)
        self.bases = tuple([base] + list(base.bases))
        base.add_subtype(self)
        if self.tree_type:
            return

        self.tree = {base_tree_type: self}

    def _set_fields(self, *declared_fields):
        self.fields = SymbolTable(self)
        self.declared_fields = SymbolTable(self)
        if self.base:
            self.fields += self.base.fields

        for field in declared_fields:
            self.declared_fields.add(field)
            self.fields.add(field)

    @property
    def is_tree_root(self):
        return bool(self.tree_type)

    def add_subtype(self, sub_message):
        check_state(self.inited, '%s must be initialized', self)
        check_isinstance(sub_message, Message)

        sub_type = sub_message.base_tree_type
        check_isinstance(sub_type, EnumValue)
        check_argument(self in sub_message.bases, '%s must inherit %s', sub_message, self)
        check_state(sub_type not in self.tree, 'Duplicate values %s, %s for subtype %s in %s tree',
                    sub_message, self.tree.get(sub_type), sub_type, self)
        check_argument(sub_type.enum == self.tree_type.enum,
                       'Wrong subtype value in %s, it must be a value of enum %s, got %s',
                       sub_message, self.tree_type.enum, sub_type)

        self.tree[sub_type] = sub_message
        if self.is_tree_root:
            return

        check_state(self.base, 'Cannot add a subtype %s to %s '
                   'which is neither a tree root nor a subtype', self)
        self.base.add_subtype(sub_message)

    def _do_parameterize(self, *variables):
        '''Parameterize this message with the given arguments, return another message.'''
        return ParameterizedMessage(self, *variables)


class ParameterizedMessage(ParameterizedType):
    def __init__(self, rawtype, variables):
        super(ParameterizedMessage, self).__init__(rawtype, variables)

        self.base = None
        self.base_tree_type = None
        self.bases = None

        self.fields = None
        self.declared_fields = None

    @property
    def tree(self):
        return self.rawtype.tree

    @property
    def tree_type(self):
        return self.rawtype.tree_type

    @property
    def is_tree_root(self):
        return self.rawtype.is_tree_root

    def init(self):
        super(ParameterizedMessage, self).init()

        vmap = self.variables.as_map()
        rawtype = self.rawtype
        base = rawtype.base.bind(vmap) if rawtype.base else None

        self.base = base
        self.base_tree_type = base.base_tree_type if base else None
        self.bases = tuple([base] + base.bases) if base else tuple()

        self.fields = SymbolTable(self)
        self.declared_fields = SymbolTable(self)
        if base:
            self.fields += base.fields

        for field in rawtype.declared_fields:
            bfield = field.bind(vmap)
            self.declared_fields.add(bfield)


class Field(object):
    def __init__(self, name, type):
        self.name = name
        self.type = type

    def bind(self, vmap):
        '''Bind this field type and return a new field.'''
        btype = self.type.bind(vmap)
        if btype == self.type:
            return self

        return Field(self.name, btype)


class Enum(Type):
    @classmethod
    def from_node(cls, node):
        check_isinstance(node, ast.Enum)
        enum = Enum(node.name)
        for name in node.values:
            EnumValue(name, enum)

        return enum

    def __init__(self, name, module=None, values=None):
        super(Enum, self).__init__(name, module=module)
        self.values = SymbolTable(self)
        if values:
            self.add_values(*values)

    def add_value(self, value):
        self.values.add(value)
        self.symbols.add(value)

    def add_values(self, *values):
        map(self.add_value, values)


class EnumValue(Type):
    def __init__(self, name, enum):
        super(EnumValue, self).__init__(name, module=enum.module)
        self.enum = enum
        enum.add_value(self)

    @property
    def parent(self):
        return self.enum


class Native(Type):
    @classmethod
    def from_node(cls, node):
        return Native(node.name, variables=(Variable(var) for var in node.variables))

    def __init__(self, name, variables=None, options=None, module=None):
        super(Native, self).__init__(name, variables, module=module)
        self.options = options

    def _do_parameterize(self, variables):
        '''Parameterize this native with the given variables and return a new one.'''
        return ParameterizedNative(self, variables)


class NativeOptions(object):
    def __init__(self, java_type=None, java_boxed=None, java_descriptor=None, java_default=None):
        self.java_type = java_type
        self.java_boxed = java_boxed
        self.java_descriptor = java_descriptor
        self.java_default = java_default


class ParameterizedNative(ParameterizedType):
    @property
    def options(self):
        return self.rawtype.options
