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
                base=ast.Ref('Base'), subtype=ast.Ref('Type.EVENT'),
                type_field='event_type', type=ast.Ref('EventType.EVENT'),
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
        base.build(type=type_base, type_field=type_field, declared_fields=(type_field,))

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
        assert list(message.bases) == [self.base]

        assert message.root_tree.type == self.event_type_value
        assert message.root_tree.field == message.fields['event_type']

        assert 'event_type' in message.declared_fields
        assert 'type' in message.fields
        assert 'event_type' in message.fields

    def test_set_base(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_message = EnumValue('MESSAGE', type)

        type_field = Field('type', type)
        base = Message('Base')
        base.build(type=type_base, type_field=type_field)

        message = Message('Message')
        message._set_base(base, type_message)

        assert message.base is base
        assert message.base_tree.type is type_message
        assert list(message.bases) == [base]

    def test_set_base_circular(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_msg = EnumValue('MSG', type)
        type_msg2 = EnumValue('MSG2', type)
        type_field = Field('type', type)

        base = Message('Base')
        base.build(type_base, type_field)

        msg = Message('Message')
        msg.build(base=base, subtype=type_msg)

        msg2 = Message('Message2')
        msg2.build(base=msg, subtype=type_msg2)

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
        base.build(type_base, type_field, declared_fields=[type_field])
        base.init()

        message = Message('Message')
        message.build(base=base, subtype=type_msg)

        duplicate_field = Field('type', type)
        self.assertRaises(ValueError, message._set_fields, duplicate_field)

    def test_parameterize(self):
        k = Variable('K')
        v = Variable('V')
        map = Message('Map', variables=[k, v])
        map.build(declared_fields=[
            Field('key', k),
            Field('value', v)
        ])

        int32 = Native('int32')
        string = Native('string')
        pmap = map.parameterize(int32, string)

        assert pmap.rawtype is map
        assert list(pmap.variables) == [int32, string]


class TestParameterizedMessage(unittest.TestCase):
    def test_parameterize_base(self):
        type = Enum('Type')
        type_col = EnumValue('COL', type)
        type_list = EnumValue('LIST', type)

        e = Variable('E')
        type_field = Field('type', type)
        collection = Message('Collection', variables=[e])
        collection.build(type=type_col, type_field=type_field,
            declared_fields=[Field('element', e)])

        t = Variable('T')
        lst = Message('List', variables=[t])
        lst.build(base=collection.parameterize(t), subtype=type_list)

        int32 = Native('int32')
        plist = lst.parameterize(int32)

        assert plist.rawtype is lst
        assert plist.base.rawtype is collection
        assert list(plist.base.variables) == [int32]
        assert len(plist.fields) == 1
        assert len(plist.declared_fields) == 0
        assert plist.fields['element'].type is int32

    def test_parameterize_fields(self):
        k = Variable('K')
        v = Variable('V')
        map = Message('Map', variables=[k, v])
        map.build(declared_fields=[Field('key', k), Field('value', v)])

        int32 = Native('int32')
        int64 = Native('int64')
        pmap = map.parameterize(int32, int64)

        assert pmap.rawtype is map
        assert pmap.base is None
        assert list(pmap.variables) == [int32, int64]
        assert len(pmap.fields) == 2
        assert len(pmap.declared_fields) == 2
        assert pmap.fields['key'].type is int32
        assert pmap.fields['value'].type is int64


class TestTree(unittest.TestCase):
    def test_construct(self):
        type = Enum('Type')
        type_object = EnumValue('OBJECT', type)
        field = Field('type', type)
        message = Message('Message')

        tree = RootTree(message, type_object, field)
        assert tree.message == message
        assert tree.enum == type
        assert tree.type == type_object
        assert tree.field == field
        assert tree.as_map() == {type_object: message}

    def test_type_of_different_enum(self):
        type = Enum('Type')
        wrong = Enum('Wrong')
        wrong_value = EnumValue('WRONG', wrong)

        field = Field('type', type)
        message = Message('Message')
        self.assertRaises(ValueError, RootTree, message, wrong_value, field)

    def test_subtree(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_msg = EnumValue('MSG', type)

        base = Message('Base')
        msg = Message('Message')
        msg.bases = [base]

        field = Field('type', type)
        basetree = RootTree(base, type_base, field)
        msgtree = basetree.subtree(msg, type_msg)

        assert msgtree.field == basetree.field
        assert msgtree.enum == basetree.enum
        assert msgtree.type == type_msg
        assert msgtree.as_map() == {type_msg: msg}
        assert basetree.as_map() == {type_base: base, type_msg: msg}

    def test_multilevel_subtree(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_post = EnumValue('POST', type)
        type_note = EnumValue('NOTE', type)

        base = Message('Base')
        post = Message('Post')
        post.bases = [base]
        note = Message('Note')
        note.bases = [post, base]

        field = Field('type', type)
        basetree = RootTree(base, type_base, field)
        posttree = basetree.subtree(post, type_post)
        notetree = posttree.subtree(note, type_note)

        assert notetree.as_map() == {type_note: note}
        assert posttree.as_map() == {type_note: note, type_post: post}
        assert basetree.as_map() == {type_note: note, type_post: post, type_base: base}

    def test_subtree_duplicate(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_post = EnumValue('POST', type)

        base = Message('Base')
        post = Message('Post')
        post.bases = [base]
        post2 = Message('Post')
        post2.bases = [base]

        field = Field('type', type)
        tree = RootTree(base, type_base, field)
        tree.subtree(post, type_post)
        self.assertRaises(ValueError, tree.subtree, post2, type_post)

    def test_subtree_subtype_of_different_enum(self):
        type = Enum('Type')
        type_base = EnumValue('BASE', type)
        type_field = Field('type', type)

        wrong = Enum('Wrong')
        wrong_value = EnumValue('WRONG', wrong)

        base = Message('Base')
        tree = RootTree(base, type_base, type_field)

        msg = Message('Message')
        msg.bases = [base]
        self.assertRaises(ValueError, tree.subtree, msg, wrong_value)


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

    def test_contains(self):
        enum = Enum('Type')
        base = EnumValue('BASE', enum)
        assert base in enum

        enum2 = Enum('Type2')
        assert base not in enum2


class TestNative(unittest.TestCase):
    def setUp(self):
        self.node = ast.Native('list', variables=['E'])

    def test_from_node(self):
        native = Native.from_node(self.node)
        assert native.name == self.node.name
        assert 'E' in native.variables
