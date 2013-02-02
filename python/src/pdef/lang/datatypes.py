# encoding: utf-8
from pdef.lang.core import *


class Native(TypeDefinition):
    @classmethod
    def from_node(cls, node, package):
        native = Native(node.name, package)

        for vname in node.variables:
            var = Variable(vname, native)
            if var.name in native.variable_map:
                var.error('Duplicate vairable')
                continue

            native.arguments.append(var)
            native.variables.append(var)
            native.variable_map[vname] = var

        native.options = Options(node.options)
        native.java_type = native.options.java_type

        return native

    def __init__(self, name, package):
        super(Native, self).__init__(name, package)

        self.java_type = None
        self.python_type = None
        self.rawtype = self

    def build_special(self):
        rawtype = self.rawtype

        self.options = rawtype.options
        self.java_type = rawtype.java_type
        self.python_type = rawtype.python_type


class Enum(TypeDefinition):
    @classmethod
    def from_node(cls, node, package):
        enum = Enum(node.name, package)

        for val in node.values:
            if val in enum.value_set:
                enum.error('Duplicate enum value %s', val)
                continue

            enum.values.append(val)
            enum.value_set.add(val)

        return enum

    def __init__(self, name, package):
        super(Enum, self).__init__(name, package)

        self.values = []
        self.value_set = set()


class Message(TypeDefinition):
    @classmethod
    def from_node(cls, node, package):
        message = Message(node.name, package)
        message.options = Options(node.options)

        for vname in node.variables:
            var = Variable(vname, message)
            if var.name in message.variable_map:
                var.error('Duplicate vairable')
                continue

            message.arguments.append(var)
            message.variables.append(var)
            message.variable_map[vname] = var

        basenode = node.base
        if basenode:
            message.baseref = TypeRef.from_node(basenode, message)

        for fnode in node.fields:
            field = Field.from_node(fnode, message)
            if field.name in message.declared_field_map:
                field.error('Duplicate field')
                continue

            message.declared_fields.append(field)
            message.declared_field_map[fnode.name] = field

        return message

    def __init__(self, name, package):
        super(Message, self).__init__(name, package)

        self.baseref = None
        self.declared_fields = []
        self.declared_field_map = {}

        self.base = None
        self.all_bases = []

        self.fields = []
        self.field_map = {}

        self.type_field = None
        self.is_type_base = None
        self.subtype_map = {} # Map {type_value: subtype}
        self.subtypes = []

        self.rawtype = self

    def symbol(self, name):
        if name in self.variable_map:
            return self.variable_map[name]

        return super(Message, self).symbol(name)

    def link(self):
        super(Message, self).link()

        baseref = self.baseref
        if baseref:
            self.base = baseref.link()

        for field in self.declared_fields:
            field.link()

        return self

    def build_special(self):
        if not self.arguments:
            raise ValueError("Not a special %s" % self)

        rawtype = self.rawtype
        arg_map = dict((var, arg) for (var, arg) in zip(rawtype.variables, self.arguments))

        base = rawtype.base
        if base:
            sbase = base.special(arg_map, self.pool)
            self.base = sbase

        declared_fields = []
        for field in rawtype.declared_fields:
            type = field.type
            stype = type.special(arg_map, self.pool)

            sfield = Field(field.name, self)
            sfield.type = stype
            sfield.options = field.options

            declared_fields.append(sfield)
        self.declared_fields = declared_fields


class Field(Node):
    @classmethod
    def from_node(cls, node, message):
        field = Field(node.name, message)
        field.typeref = TypeRef.from_node(node.type, field)
        field.options = Options(node.options)

        value = field.options.value
        if value:
            field.value_ref = EnumValueRef.from_node(value, field)

        return field

    def __init__(self, name, message):
        super(Field, self).__init__(name, message)
        self.message = message
        self.typeref = None

        self.type = None
        self.read_only = False
        self.is_type_field = False
        self.is_type_base_field = False
        self.value_ref = None
        self.value = None
        self.options = Options()

    def __repr__(self):
        if self.type:
            return "<Field %s, %s>" % (self.name, self.type.simple_repr)
        return "<Field %s, ref=%s>" % (self.name, self.typeref.simple_repr)

    def link(self):
        typeref = self.typeref
        self.type = typeref.link()

        value_ref = self.value_ref
        if not value_ref:
            return

        self.value = value_ref.link()
        return self



class EnumValue(Node):
    def __init__(self, value, type, parent):
        super(EnumValue, self).__init__(value, parent)
        self.value = value
        self.type = type


class EnumValueRef(Node):
    @classmethod
    def from_node(cls, text, parent):
        '''Translates "package.EnumType.VALUE" into EnumValueRef("package.EnumType", "VALUE").'''
        typename, _, valuename = text.rpartition('.')
        if not typename or not valuename:
            parent.error("Wrong enum value %s ", text)
            return

        typeref = TypeRef(typename, parent)
        return EnumValueRef(valuename, typeref, parent)

    def __init__(self, valuename, typeref, parent):
        super(EnumValueRef, self).__init__(valuename, parent)

        self.typeref = typeref
        self.valuename = valuename
        self.value = None

    def link(self):
        typeref = self.typeref
        self.type = typeref.link()

        if not self.type:
            # An error occurred.
            return

        if not isinstance(self.type, Enum):
            self.error('%s: wrong type %s, must be an enum',self, self.type)
            return

        enum = self.type
        valuename = self.valuename
        if valuename not in enum.value_set:
            self.error('%s: enum value %s is not found', self, valuename)
            return

        self.value = EnumValue(valuename, enum, self.parent)
        return self.value
