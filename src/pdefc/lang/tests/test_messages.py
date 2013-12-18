# encoding: utf-8
import unittest
from pdefc.lang import Enum
from pdefc.lang.packages import Package
from pdefc.lang.types import *
from pdefc.lang.messages import *
from pdefc.lang.modules import Module


class TestMessage(unittest.TestCase):
    def test_message_exception(self):
        msg = Message('Message')
        exc = Message('Exception', is_exception=True)

        assert msg.type == TypeEnum.MESSAGE
        assert msg.is_exception is False

        assert exc.type == TypeEnum.MESSAGE
        assert exc.is_exception

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
        type0 = enum.create_value('Type0')
        type1 = enum.create_value('Type1')

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

    def test_referenced_types(self):
        number = Enum('Number')
        one = number.create_value('ONE')

        base = Message('Base')
        message0 = Message('Message0')
        message1 = Message('Message1', base=base, discriminator_value=one)
        message1.create_field('field', message0)
        message1.create_field('self', message1)
        
        assert message1.referenced_types == [base, one, message0]

    # link.

    def test_link(self):
        message = Message('Message', base='base', discriminator_value='subtype')
        message.create_field('field', 'field_type')

        module = Module('module')
        errors = message.link(module)

        assert len(errors) == 3
        assert message.module is module
        assert "Type not found 'base'" in errors[0]
        assert "Type not found 'subtype'" in errors[1]
        assert "Type not found 'field_type'" in errors[2]

    # build.

    def test_build___add_message_to_base_subtypes(self):
        '''Should add a message it to its base subtypes.'''
        enum = Enum('Type')
        subtype = enum.create_value('SUBTYPE')

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
        type0 = enum.create_value('TYPE0')
        type1 = enum.create_value('TYPE1')

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
        assert 'circular inheritance' in errors[0]

    def test_validate_base__circular_inheritance(self):
        msg0 = Message('Msg0')
        msg1 = Message('Msg1')
        msg0.base = msg1
        msg1.base = msg0

        errors = msg0.validate()
        assert 'circular inheritance' in errors[0]

    def test_validate_base__invalid_base(self):
        base = Message('Base')
        base.create_field('field', NativeType.VOID)     # Field cannot be void.

        msg = Message('Msg')
        msg.base = base

        errors = msg.validate()
        assert 'invalid base' in errors[0]

    def test_validate_base__message_exception_clash(self):
        msg = Message('Msg')
        exc = Message('Exc', is_exception=True)
        exc.base = msg

        errors = exc.validate()
        assert 'wrong base type (message/exception)' in errors[0]

    def test_validate_base__must_be_declared_after_base(self):
        base = Message('Base')
        msg = Message('Message', base=base)

        module = Module('module', definitions=[msg, base])
        module.link()

        errors = msg.validate()
        assert 'must be declared after its base' in errors[0]

    def test_validate_base__prevent_base_from_dependent_module(self):
        base = Message('Base')
        msg = Message('Message', base=base)

        module0 = Module('module0', definitions=[msg])
        module0.link()

        module1 = Module('module1', definitions=[base])
        module1.add_imported_module('module0', module0)
        module1.link()

        errors = msg.validate()
        assert 'cannot inherit Base, it is in a dependent module "module1"' in errors[0]

    # validate_simple_inheritance.

    def test_validate_simple_inheritance__no_base(self):
        msg = Message('Message')
        errors = msg.validate()

        assert not errors

    def test_validate_simple_inheritance__ok(self):
        base = Message('Base')
        msg = Message('Message', base=base)

        errors = msg.validate()
        assert not errors

    def test_validate_simple_inheritance__but_base_polymorphic(self):
        enum = Enum('Number')
        base = Message('Base')
        base.create_field('field', enum, is_discriminator=True)
        msg = Message('Message', base=base)

        errors = msg.validate()
        assert 'discriminator value required, base is polymorphic' in errors[0]

    # validate_polymorphic_inheritance.

    def test_validate_polymorphic_inheritance__no_base(self):
        enum = Enum('Number')
        one = enum.create_value('ONE')
        msg = Message('Message', discriminator_value=one)

        errors = msg.validate()
        assert 'discriminator value is present, but no base' in errors[0]

    def test_validate_polymorphic_inheritance__different_packages(self):
        enum = Enum('Number')
        one = enum.create_value('ONE')
        base = Message('Base')
        base.create_field('field', type0=enum, is_discriminator=True)
        msg = Message('Message', base=base, discriminator_value=one)

        module0 = Module('module0', definitions=[base])
        module1 = Module('module1', definitions=[msg])
        package0 = Package('package0', modules=[module0])
        package1 = Package('package1', modules=[module1])

        package0._link()
        package1._link()

        errors = msg.validate()
        assert 'cannot inherit a polymorphic message from another package' in errors[0]

    def test_validate_polymorphic_inheritance__present_base_nonpolymorphic(self):
        number = Enum('Number')
        one = number.create_value('ONE')
        base = Message('Base')
        msg = Message('Message', base=base, discriminator_value=one)

        errors = msg.validate()
        assert 'base is not polymorphic, but the discriminator value is present' in errors[0]

    def test_validate_polymorphic_inheritance__does_not_match_base_discriminator_type(self):
        number = Enum('Number')
        letter = Enum('Letter')
        a = letter.create_value('A')

        base = Message('Base')
        base.create_field('field', number, is_discriminator=True)
        msg = Message('Message', base=base, discriminator_value=a)

        errors = msg.validate()
        assert 'discriminator value does not match the base discriminator type' in errors[0]

    def test_validate_polymorphic_inheritance__message_must_be_declared_after_discriminator_type(self):
        number = Enum('Number')
        one = number.create_value('ONE')

        base = Message('Base')
        base.create_field('field', number, is_discriminator=True)

        msg = Message('Message', base=base, discriminator_value=one)
        module = Module('module', definitions=[msg, number])
        module.link()

        errors = msg.validate()
        assert 'must be declared after the discriminator type' in errors[0]

    def test_validate_polymorphic_inheritance__from_dependent_module(self):
        number = Enum('Number')
        one = number.create_value('ONE')

        base = Message('Base')
        base.create_field('field', number, is_discriminator=True)
        msg = Message('Message', base=base, discriminator_value=one)

        module0 = Module('module0', definitions=[msg])
        module1 = Module('module1', definitions=[number])
        module1.add_imported_module('module0', module0)
        module0.link()
        module1.link()

        errors = msg.validate()
        assert 'cannot use Number as a discriminator, it is in a dependent module' in errors[0]

    # validate_fields.

    def test_validate_fields__duplicate_fields(self):
        msg = Message('Msg')
        msg.create_field('field', NativeType.INT32)
        msg.create_field('field', NativeType.INT32)

        errors = msg.validate()
        assert 'duplicate field' in errors[0]

    def test_validate_fields__duplicate_inherited_fields(self):
        msg0 = Message('Msg0')
        msg0.create_field('field', NativeType.STRING)

        msg1 = Message('Msg1', base=msg0)
        msg1.create_field('field', NativeType.STRING)

        errors = msg1.validate()
        assert 'duplicate field' in errors[0]

    def test_validate_fields__multiple_discriminators(self):
        enum = Enum('Type')
        msg = Message('Msg')
        msg.create_field('type0', enum, is_discriminator=True)
        msg.create_field('type1', enum, is_discriminator=True)

        errors = msg.validate()
        assert 'multiple discriminator fields' in errors[0]

    def test_validate_fields__multiple_discriminators_in_inheritance_tree(self):
        enum0 = Enum('Type0')
        enum1 = Enum('Type1')
        sub0 = enum0.create_value('SUB0')

        msg0 = Message('Msg0')
        msg0.create_field('type0', enum0, is_discriminator=True)

        msg1 = Message('Msg1')
        msg1.create_field('type1', enum1, is_discriminator=True)
        msg1.base = msg0
        msg1.discriminator_value = sub0

        errors = msg1.validate()
        assert 'multiple discriminator fields' in errors[0]

    # validate_subtypes.

    def test_validate_subtypes__duplicate_subtype_discriminator_value(self):
        enum = Enum('Type')
        subtype = enum.create_value('SUBTYPE')

        base = Message('Base')
        base.create_field('field', enum, is_discriminator=True)
        msg0 = Message('Message0', base=base, discriminator_value=subtype)
        msg1 = Message('Message0', base=base, discriminator_value=subtype)

        msg0.build()
        msg1.build()

        errors = base.validate()
        assert 'duplicate subtype with a discriminator value "SUBTYPE"' in errors[0]


class TestField(unittest.TestCase):
    def test_validate__must_be_datatype(self):
        '''Should prevent fields which are not data types.'''
        field = Field('field', NativeType.VOID)
        errors = field.validate()

        assert 'field must be a data type' in errors[0]

    def test_validate__reference(self):
        field = Field('field', references.ListReference(NativeType.VOID))
        errors = field.validate()

        assert 'List element must be a data type' in errors[0]

    def test_validate__discriminator_must_be_enum(self):
        '''Should ensure discriminator field type is an enum.'''
        enum = Enum('Enum')

        field0 = Field('field0', enum, is_discriminator=True)
        field1 = Field('field1', NativeType.INT32, is_discriminator=True)

        errors0 = field0.validate()
        errors1 = field1.validate()

        assert not errors0
        assert 'discriminator field must be an enum' in errors1[0]
