# encoding: utf-8
from pdef import ast
from pdef.preconditions import *
from pdef.lang.symbols import SymbolTable
from pdef.lang.types import Type, ParameterizedType, Variable
from pdef.lang.enums import EnumValue


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

        self.base = None
        self.bases = None
        self.basetype = None

        self.base_tree = None
        self.root_tree = None

        self.fields = SymbolTable(self)
        self.declared_fields = SymbolTable(self)

        self.node = None

    @property
    def parent(self):
        return self.module

    @property
    def tree(self):
        return self.root_tree if self.root_tree else self.base_tree

    @property
    def type_field(self):
        return self.tree.field if self.tree else None

    def is_subtype_of(self, msg):
        if isinstance(msg, ParameterizedMessage):
            msg = msg.rawtype

        for base in self.bases:
            if isinstance(base, ParameterizedMessage):
                base = base.rawtype
            if base == msg:
                return True

        return False

    def _do_init(self):
        if not self.node:
            return
        node = self.node
        base = self.lookup(node.base) if node.base else None
        subtype = self.lookup(node.subtype) if node.subtype else None

        declared_fields = SymbolTable(self)
        for field_node in node.declared_fields:
            fname = field_node.name
            ftype = self.lookup(field_node.type)
            field = Field(fname, ftype)
            declared_fields.add(field)

        if node.type:
            _type = self.lookup(node.type) if node.type else None
            type_field_name = node.type_field
        else:
            _type = None
            type_field_name = None

        self.build(type=_type, type_field_name=type_field_name,
                   base=base, subtype=subtype,
                   declared_fields=declared_fields)

    def build(self, type=None, type_field_name=None, base=None, subtype=None, declared_fields=None):
        self.set_base(base, subtype)
        self.add_fields(*(declared_fields if declared_fields else ()))
        self.set_type(type, type_field_name)
        self.inited = True

    def set_base(self, base, basetype):
        '''Set this message base message and its type.'''
        if base is None and basetype is None:
            self.bases = []
            return

        check_isinstance(base, (Message, ParameterizedMessage))
        check_isinstance(basetype, EnumValue)
        check_argument(self != base, '%s: cannot inherit itself', self)
        check_argument(not base.is_subtype_of(self),
            '%s: circular inheritance %s',
            (self, '->'.join(str(b) for b in ([self, base] + list(base.bases)))))

        self.base = check_not_none(base)
        self.bases = tuple([base] + list(base.bases))
        self.base_tree = base.tree.subtree(self, basetype)
        self.basetype = basetype

        for field in self.base.fields:
            overriden = field.subtype(basetype) if field is self.base.type_field \
                    else field.override()
            self.fields.add(overriden)

    def add_fields(self, *declared_fields):
        for field in declared_fields:
            self.declared_fields.add(field)
            self.fields.add(field)

    def set_type(self, type, field_name):
        if type is None and field_name is None:
            return
        field = self.declared_fields.get(field_name)
        if not field:
            raise ValueError('%s: type field "%s" is not found' % (self, field_name))

        field.make_type(type)
        self.root_tree = RootTree(self, type, field)

    def _do_parameterize(self, *variables):
        '''Parameterize this message with the given arguments, return another message.'''
        return ParameterizedMessage(self, *variables)


class ParameterizedMessage(ParameterizedType):
    def __init__(self, rawtype, variables):
        super(ParameterizedMessage, self).__init__(rawtype, variables)

        self.base = None
        self.bases = None

        self.fields = None
        self.declared_fields = None

    @property
    def tree(self):
        return self.rawtype.tree

    @property
    def base_tree(self):
        return self.rawtype.base_tree

    @property
    def type_field(self):
        return self.rawtype.type_field

    @property
    def root_tree(self):
        return self.rawtype.root_tree

    def is_subtype_of(self, msg):
        return self.rawtype.is_subtype_of(msg)

    def _do_init(self):
        vmap = self.variables.as_map()
        rawtype = self.rawtype
        base = rawtype.base.bind(vmap) if rawtype.base else None

        self.base = base
        self.bases = tuple([base] + list(base.bases)) if base else tuple()

        self.fields = SymbolTable(self)
        self.declared_fields = SymbolTable(self)
        if base:
            self.fields += base.fields

        for field in rawtype.declared_fields:
            bfield = field.bind(vmap)
            self.declared_fields.add(bfield)
            self.fields.add(bfield)


class AbstractField(object):
    name = None
    type = None
    declaring_field = None

    is_declared = False
    is_overriden = False
    is_type = False
    is_subtype = False
    type_value = None

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self)

    def __str__(self):
        return '"%s" of %s' % (self.name, self.type)

    def override(self):
        return OverridenField(self)

    def subtype(self, type_value):
        return SubtypeField(self, type_value)

    def bind(self, vmap):
        '''Bind this field type and return a new field.'''
        btype = self.type.bind(vmap)
        if btype == self.type:
            return self

        return ParameterizedField(self, btype)


class Field(AbstractField):
    def __init__(self, name, type):
        self.name = name
        self.type = type

        self.declaring_field = self
        self.is_declared = True

        self.is_type = False
        self.type_value = None

    def make_type(self, type_value):
        check_state(not self.is_type, '%s is a type field already', self)
        self.is_type = True
        self.type_value = check_not_none(type_value)


class OverridenField(AbstractField):
    def __init__(self, declaring_field):
        self.name = declaring_field.name
        self.type = declaring_field.type

        self.declaring_field = declaring_field
        self.is_overriden = True


class SubtypeField(OverridenField):
    def __init__(self, declaring_field, type_value):
        super(SubtypeField, self).__init__(declaring_field)
        check_argument(declaring_field.is_type, '%s must be a type field', declaring_field)
        self.is_subtype = True
        self.type_value = type_value


class ParameterizedField(AbstractField):
    def __init__(self, declaring_field, bound_type):
        self.name = declaring_field.name
        self.type = bound_type
        self.declaring_field = declaring_field


class AbstractMessageTree(object):
    # Implement in subclasses
    field = None
    enum = None

    def __init__(self, message, type):
        self.basetree = None
        self.message = message
        self.type = type
        self.subtypes = SymbolTable(self)
        self.subtypes.add(message, type)

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self)

    def __str__(self):
        return 'tree on %s of %s in %s' % (self.field, self.enum, self.message)

    def subtree(self, submessage, subtype):
        self.add(submessage, subtype)
        return SubTree(submessage, subtype, self)

    def add(self, submessage, subtype):
        check_isinstance(submessage, Message)
        check_isinstance(subtype, EnumValue)

        message = self.message
        enum = self.enum
        subtypes = self.subtypes

        check_argument(submessage.is_subtype_of(message),
                '%s: %s must be a submessage', message, submessage)
        check_argument(subtype in enum,
                '%s: wrong subtype, it must be a value of enum %s, got %s',
                submessage, enum, subtype)
        check_state(subtype not in subtypes,
                '%s: duplicate messages %s, %s for subtype %s',
                message, submessage, subtypes.get(subtype), subtype)

        self.subtypes.add(submessage, subtype)
        if self.basetree:
            self.basetree.add(submessage, subtype)

    def as_map(self):
        return self.subtypes.as_map()


class RootTree(AbstractMessageTree):
    def __init__(self, message, type, field):
        super(RootTree, self).__init__(message, type)
        check_isinstance(type, EnumValue)
        check_isinstance(field, Field)
        check_argument(type in field.type)

        self.field = field
        self.enum = field.type


class SubTree(AbstractMessageTree):
    def __init__(self, message, type, basetree):
        super(SubTree, self).__init__(message, type)
        self.basetree = basetree

    @property
    def field(self):
        return self.basetree.field

    @property
    def enum(self):
        return self.basetree.enum
