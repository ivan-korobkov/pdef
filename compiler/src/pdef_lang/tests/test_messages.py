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
        field = msg.create_field('field', NativeType.INT32)

        assert [field] == msg.declared_fields
        assert field.name == 'field'
        assert field.type == NativeType.INT32

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
        field0 = msg0.create_field('field0', NativeType.INT32)

        msg1 = Message('Msg1')
        msg1.base = msg0
        msg1.discriminator_value = type1
        field1 = msg1.create_field('field1', NativeType.STRING)

        assert msg1.fields == [type_field, field0, field1]
        assert msg1.inherited_fields == [type_field, field0]
        assert msg0.fields == [type_field, field0]
        assert msg0.inherited_fields == [type_field]

    # link.

    def test_link(self):
        message = Message('Message', base='base', discriminator_value='subtype')
        message.create_field('field', 'field_type')
        errors = message.link(lambda name: None)

        assert len(errors) == 3
        assert "type not found 'base'" in errors[0].message
        assert "type not found 'subtype'" in errors[1].message
        assert "type not found 'field_type'" in errors[2].message

    # build.

    def test_build___add_message_to_base_subtypes(self):
        '''Should add a message it to its base subtypes.'''
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg', base=base, discriminator_value=subtype)
        msg.build()

        assert msg.base is base
        assert msg.discriminator_value is subtype
        assert msg in base.subtypes

    def test_build__add_message_to_subtype_tree(self):
        '''Should add a message to to its base tree subtypes.'''
        enum = Enum('Type')
        type0 = enum.add_value('TYPE0')
        type1 = enum.add_value('TYPE1')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg0 = Message('Msg0', base=base, discriminator_value=type0)
        msg1 = Message('Msg1', base=msg0, discriminator_value=type1)

        msg0.build()
        msg1.build()
        assert msg0.subtypes == [msg1]
        assert base.subtypes == [msg0, msg1]

    # validate_base.

    def test_validate_base__self_inheritance(self):
        msg = Message('Msg')
        msg.base = msg

        errors = msg.validate()
        assert len(errors) == 1
        assert 'circular inheritance' in errors[0].message

    def test_validate_base__circular_inheritance(self):
        msg0 = Message('Msg0')
        msg1 = Message('Msg1')
        msg0.base = msg1
        msg1.base = msg0

        errors = msg0.validate()
        assert 'circular inheritance' in errors[0].message

    def test_validate_base__message_exception_clash(self):
        msg = Message('Msg')
        exc = Message('Exc', is_exception=True)
        exc.base = msg

        errors = exc.validate()
        assert 'wrong base type (message/exc)' in errors[0].message

    def test_validate_base__message_must_be_defined_after_base(self):
        base = Message('Base')
        msg = Message('Message')
        msg.base = base

        module = Module('module')
        module.add_definition(msg)
        module.add_definition(base)

        errors = msg.validate()
        assert 'Message must be defined after Base' in errors[0].message

    # validate_discriminator.

    def test_validate_discriminator__not_enum(self):
        msg = Message('Message', discriminator_value=NativeType.STRING)
        errors = msg.validate()

        assert 'discriminator value must be an enum value' in errors[0].message

    def test_validate_discriminator__no_discriminator_value_but_polymorphic_base(self):
        enum = Enum('Type')
        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg', base=base)
        errors = msg.validate()

        assert 'discriminator value required' in errors[0].message

    def test_validate_discriminator__discriminator_value_but_nonpolymorphic_base(self):
        base = Message('Base')
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        msg = Message('Msg', base=base, discriminator_value=subtype)
        errors = msg.validate()

        assert 'cannot set a discriminator value, the base is not polymorphic' in errors[0].message

    def test_validate_discrminator__value_type_does_not_match_base_discriminator_type(self):
        enum0 = Enum('Enum0')
        base = Message('Base')
        base.create_field('field', enum0, is_discriminator=True)

        enum1 = Enum('Enum1')
        subtype = enum1.add_value('SUBTYPE')
        msg = Message('Message', base=base, discriminator_value=subtype)
        errors = msg.validate()

        assert 'discriminator value does not match the base discriminator type' in errors[0].message

    def test_validate_discriminator__message_must_be_defined_after_discriminator_type(self):
        enum = Enum('Enum')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('field', enum, is_discriminator=True)
        msg = Message('Message', base=base, discriminator_value=subtype)

        module = Module('module')
        module.add_definition(base)
        module.add_definition(msg)
        module.add_definition(enum)
        errors = msg.validate()

        assert 'Message must be defined after Enum' in errors[0].message

    # validate_fields.

    def test_validate_fields__duplicate_fields(self):
        msg = Message('Msg')
        msg.create_field('field', NativeType.INT32)
        msg.create_field('field', NativeType.INT32)

        errors = msg.validate()
        assert 'duplicate field' in errors[0].message

    def test_validate_fields__duplicate_inherited_fields(self):
        msg0 = Message('Msg0')
        msg0.create_field('field', NativeType.STRING)

        msg1 = Message('Msg1', base=msg0)
        msg1.create_field('field', NativeType.STRING)

        errors = msg1.validate()
        assert 'duplicate field' in errors[0].message

    def test_validate_fields__multiple_discriminators(self):
        enum = Enum('Type')
        msg = Message('Msg')
        msg.create_field('type0', enum, is_discriminator=True)
        msg.create_field('type1', enum, is_discriminator=True)

        errors = msg.validate()
        assert 'multiple discriminator fields' in errors[0].message

    def test_validate_fields__multiple_discriminators_in_inheritance_tree(self):
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

    # validate_subtypes.

    def test_validate_subtypes__duplicate_subtype_discriminator_value(self):
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('field', enum, is_discriminator=True)
        msg0 = Message('Message0', base=base, discriminator_value=subtype)
        msg1 = Message('Message0', base=base, discriminator_value=subtype)

        msg0.build()
        msg1.build()

        errors = base.validate()
        assert "duplicate subtype with a discriminator value 'SUBTYPE'" in errors[0].message


class TestField(unittest.TestCase):
    def test_validate__must_be_datatype(self):
        '''Should prevent fields which are not data types.'''
        field = Field('field', NativeType.VOID)
        errors = field.validate()

        assert 'field must be a data type' in errors[0].message

    def test_validate__reference(self):
        field = Field('field', references.ListReference(NativeType.VOID))
        errors = field.validate()

        assert 'list element must be a data type' in errors[0].message

    def test_validate__discriminator_must_be_enum(self):
        '''Should ensure discriminator field type is an enum.'''
        enum = Enum('Enum')

        field0 = Field('field0', enum, is_discriminator=True)
        field1 = Field('field1', NativeType.INT32, is_discriminator=True)

        errors0 = field0.validate()
        errors1 = field1.validate()

        assert not errors0
        assert 'discriminator field must be an enum' in errors1[0].message
