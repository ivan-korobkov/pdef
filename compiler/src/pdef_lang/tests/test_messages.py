# encoding: utf-8
import unittest
from pdef_lang import Enum, Interface
from pdef_lang.definitions import *
from pdef_lang.messages import *
from pdef_lang.modules import Module


class TestMessage(unittest.TestCase):
    def test_create_field(self):
        '''Should create and add a field to a message.'''
        msg = Message('Msg')
        field = msg.create_field('field', NativeTypes.INT32)

        assert [field] == msg.declared_fields
        assert field.name == 'field'
        assert field.type == NativeTypes.INT32

    def test_create_field__set_discriminator(self):
        '''Should set a message discriminator when a field is a discriminator.'''
        enum = Enum('Type')
        msg = Message('Msg')
        field = msg.create_field('type', enum, is_discriminator=True)

        assert field.is_discriminator
        assert msg.discriminator is field

    def test_inherited_fields(self):
        '''Should correctly compute message inherited fields.'''
        enum = Enum('Type')
        type0 = enum.add_value('Type0')
        type1 = enum.add_value('Type1')

        base = Message('Base')
        type_field = base.create_field('type', enum, is_discriminator=True)

        msg0 = Message('Msg0')
        msg0.base = base
        msg0.discriminator_value = type0
        field0 = msg0.create_field('field0', NativeTypes.INT32)

        msg1 = Message('Msg1')
        msg1.base = msg0
        msg1.discriminator_value = type1
        field1 = msg1.create_field('field1', NativeTypes.STRING)

        assert msg1.fields == [type_field, field0, field1]
        assert msg1.inherited_fields == [type_field, field0]
        assert msg0.fields == [type_field, field0]
        assert msg0.inherited_fields == [type_field]

    def test_validate_message_base__self_inheritance(self):
        '''Should prevent self-inheritance.'''
        msg = Message('Msg')
        msg.base = msg

        errors = msg.validate()
        assert len(errors) == 1
        assert 'circular inheritance' in errors[0].message

    def test_validate_message_base__circular_inheritance(self):
        '''Should prevent circular inheritance.'''
        msg0 = Message('Msg0')
        msg1 = Message('Msg1')
        msg0.base = msg1
        msg1.base = msg0

        errors = msg0.validate()
        assert 'circular inheritance' in errors[0].message

    def test_validate_message_base__message_exception_clash(self):
        '''Should prevent message<->exception inheritance.'''
        msg = Message('Msg')
        exc = Message('Exc', is_exception=True)
        exc.base = msg

        errors = exc.validate()
        assert 'wrong base type (message/exc)' in errors[0].message

    def test_validate_message_base__no_enum_value_for_discriminator(self):
        '''Should prevent inheriting a polymorphic base by a non-polymorphic message.'''
        enum = Enum('Type')
        enum.add_value('Subtype')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg')
        msg.base = base

        errors = msg.validate()
        assert 'discriminator value required' in errors[0].message

    def test_validate_message_base__base_does_not_have_discriminator(self):
        '''Should prevent inheriting a non-polymorphic base by a polymorphic message.'''
        base = Message('Base')
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        msg = Message('Msg')
        msg.base = base
        msg.discriminator_value = subtype

        errors = msg.validate()
        assert 'cannot set a discriminator value' in errors[0].message

    def test_validate_base__base_must_be_referenced_before(self):
        base = Message('Base')
        msg = Message('Message')
        msg.base = base

        module = Module('module')
        module.add_definition(msg)
        module.add_definition(base)

        errors = msg.validate()
        assert 'must be defined before' in errors[0].message

    def test_validate_fields__duplicate(self):
        '''Should prevent duplicate message fields.'''
        msg = Message('Msg')
        msg.create_field('field', NativeTypes.INT32)
        msg.create_field('field', NativeTypes.INT32)

        errors = msg.validate()
        assert 'duplicate field' in errors[0].message

    def test_validate_fields__duplicate_inherited_field(self):
        '''Should prevent duplicate fields with inherited fields.'''
        msg0 = Message('Msg0')
        msg0.create_field('field', NativeTypes.STRING)

        msg1 = Message('Msg1', base=msg0)
        msg1.create_field('field', NativeTypes.STRING)

        errors = msg1.validate()
        assert 'duplicate field' in errors[0].message

    def test_validate_fields__duplicate_discriminator(self):
        '''Should prevent multiple discriminators in a message'''
        enum = Enum('Type')
        msg = Message('Msg')
        msg.create_field('type0', enum, is_discriminator=True)
        msg.create_field('type1', enum, is_discriminator=True)

        errors = msg.validate()
        assert 'multiple discriminator fields' in errors[0].message

    def test_validate_fields__duplicate_base_discriminator(self):
        '''Should forbid multiple discriminators in messages.'''
        enum0 = Enum('Type0')
        enum1 = Enum('Type1')
        sub0 = enum0.add_value('SUB0')

        msg0 = Message('Msg0')
        msg0.create_field('type0', enum0, is_discriminator=True)

        msg1 = Message('Msg1')
        msg1.create_field('type1', enum1, is_discriminator=True)
        msg1.base = msg0
        msg1.discriminator_value = sub0

        errors = msg1.validate()
        assert 'multiple discriminator fields' in errors[0].message

    # Building.

    def test_link_message__base_with_type(self):
        '''Should link message and add to it to its base subtypes.'''
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        module = Module('test')
        module.add_definitions(enum, base)

        msg = Message('Msg')
        msg.set_base(Reference(ast.DefRef('Base'), module),
                     Reference(ast.DefRef('Type.SUBTYPE'), module))

        self.linker._link_def(msg)
        assert msg.base is base
        assert msg.discriminator_value is subtype
        assert msg in base.subtypes

    def test_link_message__base_subtype_tree(self):
        '''Should set a message base with a base type and add the message to the subtype tree.'''
        enum = Enum('Type')
        type0 = enum.add_value('Type0')
        type1 = enum.add_value('Type1')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg0 = Message('Msg0')
        msg0.set_base(base, type0)

        msg1 = Message('Msg1')
        msg1.set_base(msg0, type1)

        module = Module('test')
        module.add_definitions(enum, base, msg0, msg1)

        self.linker._link_def(msg1)
        assert msg0.subtypes == [msg1]
        assert base.subtypes == [msg0, msg1]


class TestField(unittest.TestCase):
    def test_validate__must_be_datatype(self):
        '''Should prevent fields which are not data types.'''
        iface = Interface('Interface')
        field = Field('field', iface)

        errors = field.validate()
        assert 'field must be a data type' in errors[0].message

    def test_validate__discriminator_must_be_enum(self):
        '''Should ensure discriminator field type is an enum.'''
        enum = Enum('Enum')

        field0 = Field('field0', enum, is_discriminator=True)
        field1 = Field('field1', NativeTypes.INT32, is_discriminator=True)

        errors0 = field0.validate()
        errors1 = field1.validate()

        assert not errors0
        assert 'discriminator field must be an enum' in errors1[0].message
