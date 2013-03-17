# encoding: utf-8
import unittest
from pdef import ast
from pdef.lang import *


class TestMessage(unittest.TestCase):
    def setUp(self):
        # enum Type { BASE, EVENT };
        # enum EventType { EVENT };
        # Base polymorphic on "type" as Type.Base {
        #   type Type;
        # }
        # Message<E> inherits Base as Type.EVENT
        #            polymorphic on "event_type" as EventType.EVENT {
        #   event_type EventType;
        #   object     E;
        # }
        self.node = ast.Message('Message', variables=('E'),
                base=ast.Ref('Base'), base_tree_type=ast.Ref('Type.EVENT'),
                tree_field='event_type', tree_type=ast.Ref('EventType.EVENT'),
                declared_fields=(
                    ast.Field('event_type', ast.Ref('EventType')),
                    ast.Field('object', ast.Ref('E'))
                ))

        module = Module('module')

        type = Enum('Type', module)
        type_base = EnumValue('BASE', type)
        type_event = EnumValue('EVENT', type)

        event_type = Enum('EventType', module)
        event_type_value = EnumValue('EVENT', event_type)

        type_field = Field('type', type)
        base = Message('Base')
        base.do_init(tree_type=type_base, tree_field=type_field, declared_fields=(type_field,))

        module.add_definitions(type, event_type, base)

        self.module = module
        self.type = type
        self.type_event = type_event
        self.event_type = event_type
        self.event_type_value = event_type_value
        self.base = base

    def test_from_node(self):
        message = Message.from_node(self.node)
        assert message.node is self.node
        assert message.name == self.node.name
        assert 'E' in message.symbols
        assert 'E' in message.variables

    def test_init(self):
        message = Message.from_node(self.node, self.module)
        message.init()

        assert message.base is self.base
        assert message.base_tree_type is self.type_event
        assert list(message.bases) == [self.base]

        assert message.tree_type == self.event_type_value
        assert message.tree_field == message.fields['event_type']
        assert message.tree == {self.event_type_value: message}

        assert 'event_type' in message.declared_fields
        assert 'type' in message.fields
        assert 'event_type' in message.fields

    def test_set_tree_type(self):
        type = Enum('Type')
        type_object = EnumValue('OBJECT', type)

        field = Field('type', type)
        message = Message('Message')
        message._set_tree_type(type_object, field)
        assert message.tree_type == type_object
        assert message.tree_field == field
        assert message.tree == {type_object: message}

    def test_set_tree_type_wrong_enum(self):
        type = Enum('Type')
        wrong = Enum('Wrong')
        wrong_value = EnumValue('WRONG', wrong)

        field = Field('type', type)
        message = Message('Message')
        self.assertRaises(ValueError, message._set_tree_type, wrong_value, field)

    def test_set_base(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_message = EnumValue('MESSAGE', type)

        type_field = Field('type', type)
        base = Message('Base')
        base.do_init(tree_type=type_base, tree_field=type_field)

        message = Message('Message')
        message.do_init(base=base, base_tree_type=type_message)

        assert message.base is base
        assert message.base_tree_type is type_message
        assert list(message.bases) == [base]
        assert message.tree == {type_message: message}

    def test_set_base_circular(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_msg = EnumValue('MSG', type)
        type_msg2 = EnumValue('MSG2', type)
        type_field = Field('type', type)

        base = Message('Base')
        base.do_init(type_base, type_field)
        base.init()

        msg = Message('Message')
        msg.do_init(base=base, base_tree_type=type_msg)
        msg.init()

        msg2 = Message('Message2')
        msg2.do_init(base=msg, base_tree_type=type_msg2)
        msg2.init()

        self.assertRaises(ValueError, base._set_base, msg2, type_base)

    def test_set_fields(self):
        int32 = Native('int32')
        int64 = Native('int64')
        field1 = Field('field1', int32)
        field2 = Field('field2', int64)

        msg = Message('Message')
        msg._set_fields(field1, field2)

        assert 'field1' in msg.fields
        assert 'field2' in msg.fields
        assert 'field1' in msg.declared_fields
        assert 'field2' in msg.declared_fields

    def test_set_fields_duplicates(self):
        int32 = Native('int32')
        field1 = Field('field', int32)
        field2 = Field('field', int32)

        msg = Message('Message')
        self.assertRaises(ValueError, msg._set_fields, field1, field2)

    def test_set_fields_overriden_duplicate(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_msg = EnumValue('MSG', type)
        type_field = Field('type', type)

        base = Message('Base')
        base.do_init(type_base, type_field, declared_fields=[type_field])
        base.init()

        message = Message('Message')
        message.do_init(base=base, base_tree_type=type_msg)

        duplicate_field = Field('type', type)
        self.assertRaises(ValueError, message._set_fields, duplicate_field)

    def test_add_subtype_duplicate(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_msg = EnumValue('MSG', type)
        type_field = Field('type', type)

        base = Message('Base')
        base.do_init(tree_type=type_base, tree_field=type_field)
        base.init()

        msg = Message('Message')
        msg.do_init(base=base, base_tree_type=type_msg)

        msg2 = Message('Message2')
        self.assertRaises(ValueError, msg2.do_init, base=base, base_tree_type=type_msg)

    def test_add_subtype_of_wrong_enum(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_field = Field('type', type)

        wrong = Enum('Wrong')
        wrong_value = EnumValue('WRONG', wrong)

        base = Message('Base')
        base.do_init(tree_type=type_base, tree_field=type_field)
        base.init()

        msg = Message('Message')
        self.assertRaises(ValueError, msg.do_init, base=base, base_tree_type=wrong_value)

    def test_parameterize(self):
        assert False


class TestEnum(unittest.TestCase):
    def setUp(self):
        self.node = ast.Enum('Type', values=(
            'BASE', 'OBJECT', 'EVENT'
        ))

    def test_from_node(self):
        enum = Enum.from_node(self.node)
        assert enum.name == self.node.name
        assert 'BASE' in enum.values
        assert 'OBJECT' in enum.values
        assert 'EVENT' in enum.values

    def test_duplicate_value(self):
        enum = Enum('Type')
        EnumValue('BASE', enum)
        self.assertRaises(ValueError, EnumValue, 'BASE', enum)


class TestNative(unittest.TestCase):
    def setUp(self):
        self.node = ast.Native('list', variables=['E'])

    def test_from_node(self):
        native = Native.from_node(self.node)
        assert native.name == self.node.name
        assert 'E' in native.variables
