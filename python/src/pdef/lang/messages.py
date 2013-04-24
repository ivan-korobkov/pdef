# encoding: utf-8
from pdef import ast
from pdef.preconditions import *
from pdef.lang.symbols import SymbolTable
from pdef.lang.types import Type
from pdef.lang.enums import EnumValue


class Message(Type):
    @classmethod
    def from_node(cls, node, module=None):
        check_isinstance(node, ast.Message)
        message = Message(node.name, module=module)
        message.node = node
        return message

    def __init__(self, name, module=None):
        super(Message, self).__init__(name, module)
        self.base = None
        self.bases = None
        self.basetype = None
        self.subtypes = None

        self.fields = SymbolTable(self)
        self.declared_fields = SymbolTable(self)

        self.node = None
        self.is_exception = False

    def __repr__(self):
        m = 'Exception' if self.is_exception else 'Message'
        return '<%s %s>' % (m, self.fullname)

    @property
    def parent(self):
        return self.module

    def is_subtype_of(self, msg):
        for base in self.bases:
            if base == msg:
                return True

        return False

    def _init(self):
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

        self.build(type=_type, type_field_name=type_field_name, base=base, subtype=subtype,
                   declared_fields=declared_fields, is_exception=node.is_exception)

    def build(self, type=None, type_field_name=None, base=None, subtype=None, declared_fields=None,
              is_exception=False):
        self.is_exception = is_exception
        self.set_base(base, subtype)
        self.add_fields(*(declared_fields if declared_fields else ()))
        self.set_type(type, type_field_name)
        self.inited = True

    def set_base(self, base, basetype):
        '''Set this message base message and its type.'''
        if base is None and basetype is None:
            self.bases = []
            return

        check_isinstance(base, Message)
        check_isinstance(basetype, EnumValue)
        check_argument(self != base, '%s: cannot inherit itself', self)
        check_argument(not base.is_subtype_of(self),
            '%s: circular inheritance %s',
            (self, '->'.join(str(b) for b in ([self, base] + list(base.bases)))))
        check_argument(self.is_exception == base.is_exception, '%s: cannot inherit %s', self, base)

        self.base = check_not_none(base)
        self.bases = tuple([base] + list(base.bases))
        self.basetype = basetype
        self.subtypes = base.subtypes.subclass(self, basetype)

        for field in self.base.fields:
            overriden = field.subtype(basetype) if field is self.subtypes.field \
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
        self.subtypes = RootSubtypes(self, type, field)


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


class AbstractSubtypes(object):
    field = None
    enum = None
    basetypes = None
    is_root = False

    def __init__(self, message, type):
        self.message = message
        self.type = type
        self.subtypes = SymbolTable(self)
        self.subtypes.add(message, type)

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self)

    def __str__(self):
        return 'subtypes on %s of %s' in (self.field, self.enum)

    def subclass(self, submessage, subtype):
        self.add(submessage, subtype)
        return SubclassSubtypes(submessage, subtype, self)

    def add(self, submessage, subtype):
        check_isinstance(submessage, Message)
        check_isinstance(subtype, EnumValue)

        message = self.message
        enum = self.enum
        subtypes = self.subtypes

        check_argument(submessage.is_subtype_of(message), '%s: %s must be a submessage',
                       message, submessage)
        check_argument(subtype in enum, '%s: wrong subtype, it must be a value of enum %s, got %s',
                       submessage, enum, subtype)
        check_state(subtype not in subtypes, '%s: duplicate messages %s, %s for subtype %s',
                    message, submessage, subtypes.get(subtype), subtype)

        self.subtypes.add(submessage, subtype)
        if self.basetypes:
            self.basetypes.add(submessage, subtype)

    def as_map(self):
        return self.subtypes.as_map()


class RootSubtypes(AbstractSubtypes):
    is_root = True
    def __init__(self, message, type, field):
        super(RootSubtypes, self).__init__(message, type)
        check_isinstance(type, EnumValue)
        check_isinstance(field, Field)
        check_argument(type in field.type)

        self.field = field
        self.enum = field.type


class SubclassSubtypes(AbstractSubtypes):
    def __init__(self, message, type, basetypes):
        super(SubclassSubtypes, self).__init__(message, type)
        self.basetypes = basetypes

    @property
    def field(self):
        return self.basetypes.field

    @property
    def enum(self):
        return self.basetypes.enum
