# encoding: utf-8
import unittest
from pdef.lang import *


class Test(unittest.TestCase):
    def test(self):
        int32 = Native('int32')
        string = Native('string')
        List = Native('List', [Variable('E')])
        builtin_types = Module('pdef.types', definitions=[
            int32, string, List
        ])

        builtin = Package('pdef', None)
        builtin.add_module(builtin_types)

        msg = Message('Message1', declared_fields=[
            Field('int', Reference('int32')),
            Field('str', Reference('string')),
            Field('list', Reference('List', [Reference('string')])),
            Field('msg2', Reference('module2.Message2'))
        ])
        module1 = Module('test.module1',
            imports=[Import('test.module2', 'module2')],
            definitions=[msg])

        msg2 = Message('Message2', declared_fields=[
            Field('circular', Reference('test.module1.Message1'))
        ])
        module2 = Module('test.module2',
            imports=[Import('test.module1')],
            definitions=[msg2])

        pkg = Package('test', builtin=builtin)
        pkg.add_module(module1)
        pkg.add_module(module2)
        pkg.link()

        msg_fields = msg.declared_fields
        assert msg_fields['int'].type == int32
        assert msg_fields['str'].type == string
        assert msg_fields['msg2'].type == msg2
        assert msg_fields['list'].type.declaration == List
        assert msg_fields['list'].type.variables.values() == [string]

        msg2_fields = msg2.declared_fields
        assert msg2_fields['circular'].type == msg


class TestImport(unittest.TestCase):
    def test_link(self):
        module = Module('package.module')
        package = Package('package')
        package.add_module(module)

        imp = Import('package.module', 'module')
        imp.parent = module

        linked = imp.link()
        assert linked == module


class TestField(unittest.TestCase):
    def test_link(self):
        '''Should replace the field's type with a linked one.'''
        class SimpleType(Type):
            def link(self):
                return 'linked'

        field = Field('myfield', SimpleType('simple'))
        linked_field = field.link()
        assert field is linked_field
        assert field.type == 'linked'

    def test_create_special(self):
        '''Should return a new field with a specialized type.'''
        special = Type('special')
        class GenericType(Type):
            def special(self, arg_map):
                return special

        field = Field('field', GenericType('generic'))
        sfield = field.create_special(None)
        assert sfield is not field
        assert sfield.type == special

    def test_create_special_non_generic(self):
        '''Should return the same field when the type is not generic.'''
        class NonGenericType(Type):
            def special(self, arg_map):
                return self

        field = Field('field', NonGenericType('nongeneric'))
        sfield = field.create_special(None)
        assert field is sfield
