# encoding: utf-8
import unittest
from pdef_lang import NativeTypes
from pdef_lang.interfaces import *
from pdef_lang.messages import Message
from pdef_lang.modules import Module


class TestInterface(unittest.TestCase):
    def test_methods(self):
        '''Should combine the inherited and declared methods.'''
        iface0 = Interface('Iface0')
        iface1 = Interface('Iface1', base=iface0)

        method0 = iface0.create_method('method0')
        method1 = iface1.create_method('method1')

        assert iface1.inherited_methods == [method0]
        assert iface1.methods == [method0, method1]

    def test_create_method(self):
        '''Should create a new method to this interface.'''
        iface = Interface('Calc')
        method = iface.create_method('sum', NativeTypes.INT32,
                                     ('i0', NativeTypes.INT32), ('i1', NativeTypes.INT32))

        assert [method] == iface.declared_methods
        assert method.name == 'sum'
        assert method.result is NativeTypes.INT32
        assert method.args[0].name == 'i0'
        assert method.args[1].name == 'i1'

    def test_link(self):
        pass

    def test_validate_base__self_inheritance(self):
        '''Should prevent interface self-inheritance.'''
        iface = Interface('Iface')
        iface.base = iface

        errors = iface.validate()
        assert 'circular inheritance' in errors[0].message

    def test_validate_base__circular_inheritance(self):
        '''Should prevent circular interface inheritance.'''
        iface0 = Interface('Iface0')
        iface1 = Interface('Iface1')
        iface2 = Interface('Iface2')

        iface0.base = iface2
        iface1.base = iface0
        iface2.base = iface1

        errors = iface2.validate()
        assert 'circular inheritance' in errors[0].message

    def test_validate_base__must_be_interface(self):
        '''Should prevent interface bases which are not interfaces.'''
        iface = Interface('Iface0')
        iface.base = NativeTypes.INT32

        errors = iface.validate()
        assert 'base must be an interface' in errors[0].message

    def test_validate_base__must_be_referenced_before(self):
        '''Base should be referenced before the interface.'''
        base = Interface('Base')
        iface = Interface('Interface', base=base)

        module = Module('module')
        module.add_definition(iface)
        module.add_definition(base)

        errors = iface.validate()
        assert 'must be defined before' in errors[0].message

    def test_validate_exc__tries_to_throw_non_exception(self):
        '''Should prevent setting interface exception to a non-exception type.'''
        nonexc = NativeTypes.INT32
        iface = Interface('Interface', exc=nonexc)

        errors = iface.validate()
        assert 'interface exc must be an exception' in errors[0].message

    def test_validate_methods__duplicates(self):
        iface0 = Interface('Interface0')
        iface0.create_method('method')

        iface1 = Interface('Interface1', base=iface0)
        iface1.create_method('method')

        errors = iface1.validate()
        assert 'duplicate method' in errors[0].message


class TestMethod(unittest.TestCase):
    def test_validate__post_must_be_remote(self):
        result = Interface('Interface')
        method = Method('method', result, is_post=True)

        errors = method.validate()
        assert '@post method must be remote' in errors[0].message

    def test_validate__duplicate_args(self):
        method = Method('method')
        method.create_arg('arg', NativeTypes.INT32)
        method.create_arg('arg', NativeTypes.INT32)

        errors = method.validate()
        assert 'duplicate argument' in errors[0].message

    def test_validate__form_field_clashes_with_arg(self):
        form = Message('Form', is_form=True)
        form.create_field('clash', NativeTypes.INT32)

        method = Method('method', NativeTypes.INT32)
        method.create_arg('clash', NativeTypes.INT32)
        method.create_arg('form', form)

        errors = method.validate()
        assert 'form fields clash with method args' in errors[0].message


class TestMethodArg(unittest.TestCase):
    def test_link(self):
        linker = lambda name: name
        arg = MethodArg('arg', 'module.Message')

        errors = arg.link(linker)
        assert not errors
        assert arg.type == 'module.Message'

    def test_validate__argument_is_data_type(self):
        iface = Interface('Interface')
        arg = MethodArg('arg', iface)

        errors = arg.validate()
        assert 'argument must be a data type' in errors[0].message
