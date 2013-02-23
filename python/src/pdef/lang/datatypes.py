# encoding: utf-8
from collections import OrderedDict
from pdef.preconditions import *
from pdef.lang.nodes import Node, Symbol, SymbolTable
from pdef.lang.types import Type, ParameterizedType
from pdef.lang.refs import Ref


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

