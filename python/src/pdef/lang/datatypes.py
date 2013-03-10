# encoding: utf-8
from pdef.preconditions import *
from pdef.lang.symbols import SymbolTable
from pdef.lang.types import Type, ParameterizedType


class Message(Type):
    def __init__(self, name, variables=None, module=None):
        super(Message, self).__init__(name, variables, module)

        self.tree = None
        self.tree_type = None

        self.base = None
        self.base_tree_type = None
        self.bases = None

        self.fields = None
        self.declared_fields = None

    def init(self, tree_type=None, tree_field=None, base=None, base_tree_type=None,
              declared_fields=None):
        check_state(not self.is_initialized, '%s is already initialized', self)

        self._set_tree_type(tree_type, tree_field)
        self._set_base(base, base_tree_type)
        self._set_fields(declared_fields if declared_fields else ())

        self.is_initialized = True

    def _set_tree_type(self, tree_type, tree_field):
        if tree_type is None and tree_field is None:
            return
        check_isinstance(tree_type, EnumValue)
        check_isinstance(tree_field, Field)

        self.tree_type = tree_type
        self.tree_field = tree_field

    def _set_base(self, base, base_tree_type):
        '''Set this message base message and its type.'''
        if base is None and base_tree_type is None:
            self.bases = []
            return

        check_isinstance(base, Message)
        check_isinstance(base_tree_type, EnumValue)
        check_argument(base.is_initialized, '%s must be initialized to be used as base of %s',
                       base, self)

        self.base = check_not_none(base)
        self.base_tree_type = check_not_none(base_tree_type)
        self.bases = tuple([base] + base.bases)
        base.add_subtype(self, base_tree_type)

    def _set_fields(self, declared_fields):
        self.fields = SymbolTable()
        self.declared_fields = SymbolTable()
        if self.base:
            self.fields += self.base.fields

        for field in declared_fields:
            self.declared_fields.add(field)
            self.fields.add(field)

    @property
    def is_tree_root(self):
        return bool(self.tree_type)

    def add_subtype(self, sub_message, sub_type):
        check_isinstance(sub_message, Message)
        check_isinstance(sub_type, EnumValue)
        check_argument(sub_message is not self)
        check_argument(self in sub_message.bases, '%s must inherit %s', sub_message, self)
        check_state(sub_type not in self.tree, 'Duplicate subtype %s in %s tree', sub_type, self)

        self.tree[sub_type] = sub_message
        if self.is_tree_root:
            return

        check_state(self.base, 'Cannot add a subtype %s to %s '
                   'which is neither a tree root nor a subtype', self)
        self.base.add_tree_subtype(sub_message, sub_type)

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

        self.fields = SymbolTable()
        self.declared_fields = SymbolTable()
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
    def __init__(self, name, module=None):
        super(Enum, self).__init__(name, module=module)
        self.values = SymbolTable()

    def add_value(self, value):
        self.values.add(value)


class EnumValue(Type):
    def __init__(self, name, enum):
        super(EnumValue, self).__init__(name, module=enum.module)
        self.enum = enum
        enum.add_value(self)


class Native(Type):
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
