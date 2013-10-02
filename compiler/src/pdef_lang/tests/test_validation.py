# encoding: utf-8
import unittest
from pdef_compiler.lang import *
from pdef_compiler.validator import *


class TestValidator(unittest.TestCase):
    def setUp(self):
        self.validator = Validator()

    def test_validate_module__duplicate_imports(self):
        module = Module('test')
        module.add_imported_module('submodule', Module('module0.submodule'))
        module.add_imported_module('submodule', Module('module1.submodule'))

        errors = self.validator._validate_module(module)
        assert len(errors) == 1
        assert 'duplicate import' in errors[0].message

    def test_validate_module__duplicate_definition(self):
        '''Should prevent adding a duplicate definition to a module.'''
        def0 = Definition(Type.REFERENCE, 'Test')
        def1 = Definition(Type.REFERENCE, 'Test')

        module = Module('test')
        module.add_definition(def0)
        module.add_definition(def1)

        errors = self.validator._validate_module(module)
        assert len(errors) == 1
        assert 'duplicate definition' in errors[0].message

    def test_validate_module__definition_import_clash(self):
        '''Should prevent adding a definition to a module when its name clashes with an import.'''
        module = Module('test')
        module.add_imported_module('clash', Module('imported'))

        def0 = Definition(Type.REFERENCE, 'clash')
        module.add_definition(def0)

        errors = self.validator._validate_module(module)
        assert len(errors) == 1
        assert 'duplicate definition or import' in errors[0].message


class TestAbstractValidator(unittest.TestCase):
    def __init__(self):
        self.validator = AbstractValidator()

    def test_has_import_circle__true(self):
        # 0 -> 1 -> 2 -> 0
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.add_imported_module('module1', module1)
        module1.add_imported_module('module2', module2)
        module2.add_imported_module('module0', module0)

        assert self.validator._has_import_circle(module0, module2)

    def test_has_import_circle__false(self):
        # 0 -> 1 -> 2
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.add_imported_module('module0', module1)
        module1.add_imported_module('module0', module2)

        assert self.validator._has_import_circle(module0, module2) is False


class TestMessageValidator(unittest.TestCase):
    def test_validate_message_base__self_inheritance(self):
        '''Should prevent self-inheritance.'''
        msg = Message('Msg')
        msg.base = msg

        errors = self.validator._validate_definition(msg)
        assert len(errors) == 1
        assert 'circular inheritance' in errors[0].message

    def test_validate_message_base__circular_inheritance(self):
        '''Should prevent circular inheritance.'''
        msg0 = Message('Msg0')
        msg1 = Message('Msg1')
        msg1.base = msg0
        msg0.base = msg1

        errors = self.validator._validate_definition(msg0)
        assert 'circular inheritance' in errors[0].message

    def test_validate_message_base__message_exception_clash(self):
        '''Should prevent message<->exception inheritance.'''
        msg = Message('Msg')
        exc = Message('Exc', is_exception=True)
        exc.base = msg

        errors = self.validator._validate_definition(msg)
        assert 'wrong base type (message/exc)' in errors[0].message

    def test_validate_message_base__no_enum_value_for_discriminator(self):
        '''Should prevent inheriting a polymorphic base by a non-polymorphic message.'''
        enum = Enum('Type')
        enum.add_value('Subtype')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg')
        msg.base = base

        errors = self.validator._validate_definition(msg)
        assert 'discriminator value required' in errors[0].message

    def test_validate_message_base__base_does_not_have_discriminator(self):
        '''Should prevent inheriting a non-polymorphic base by a polymorphic message.'''
        base = Message('Base')
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        msg = Message('Msg')
        msg.base = base
        msg.discriminator_value = subtype

        errors = self.validator._validate_definition(msg)
        assert 'cannot set a discriminator value' in errors[0].message

    def test_validate_base__base_must_be_referenced_before(self):
        base = Message('Base')
        msg = Message('Message')
        msg.set_base(base)

        module = Module('module')
        module.add_definitions(msg, base)
        module.link_imports()
        module.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'must be referenced before' in e.message

    def test_validate_fields__duplicate(self):
        '''Should prevent duplicate message fields.'''
        msg = Message('Msg')
        msg.create_field('field', NativeTypes.INT32)
        msg.create_field('field', NativeTypes.INT32)
        msg.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'Duplicate field' in e.message

    def test_validate_fields__duplicate_inherited_field(self):
        '''Should prevent duplicate fields with inherited fields.'''
        msg0 = Message('Msg0')
        msg0.create_field('field', NativeTypes.STRING)

        msg1 = Message('Msg1')
        msg1.set_base(msg0)
        msg1.create_field('field', NativeTypes.STRING)
        msg1.link()

        try:
            msg1.validate()
            self.fail()
        except CompilerException, e:
            assert 'Duplicate field' in e.message

    def test_validate_fields__duplicate_discriminator(self):
        '''Should prevent multiple discriminators in a message'''
        enum = Enum('Type')
        msg = Message('Msg')
        msg.create_field('type0', enum, is_discriminator=True)
        msg.create_field('type1', enum, is_discriminator=True)
        msg.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'Multiple discriminator fields' in e.message

    def test_validate_fields__duplicate_base_discriminator(self):
        '''Should forbid multiple discriminators in messages.'''
        enum0 = Enum('Type0')
        enum1 = Enum('Type1')
        sub0 = enum0.add_value('SUB0')

        msg0 = Message('Msg0')
        msg0.create_field('type0', enum0, is_discriminator=True)

        msg1 = Message('Msg1')
        msg1.create_field('type1', enum1, is_discriminator=True)
        msg1.set_base(msg0, sub0)
        msg1.link()

        try:
            msg1.validate()
            self.fail()
        except CompilerException, e:
            assert 'Multiple discriminator fields' in e.message

    def test_validate__must_be_datatype(self):
        '''Should prevent fields which are not data types.'''
        iface = Interface('Interface')
        message = mock.Mock()
        field = Field('field', iface, message)
        field.link()

        try:
            field.validate()
            self.fail()
        except CompilerException, e:
            assert 'Field must be a data type' in e.message

    def test_validate__discriminator_must_be_enum(self):
        '''Should ensure discriminator field type is an enum.'''
        enum = Enum('Enum')
        message = mock.Mock()

        field0 = Field('field0', enum, message, is_discriminator=True)
        field1 = Field('field1', NativeTypes.INT32, message, is_discriminator=True)

        field0.link()
        field1.link()
        try:
            field0.validate()
            field1.validate()
            self.fail()
        except CompilerException, e:
            assert 'Discriminator field must be an enum' in e.message
