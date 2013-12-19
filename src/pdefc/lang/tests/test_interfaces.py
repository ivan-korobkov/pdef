# encoding: utf-8
import unittest
from pdefc.lang import Module, Message
from pdefc.lang.interfaces import *
from pdefc.lang.types import NativeType


class TestInterface(unittest.TestCase):
    def test_methods(self):
        iface0 = Interface('Iface0')

        method0 = iface0.create_method('method0')
        method1 = iface0.create_method('method1')

        assert iface0.methods == [method0, method1]

    def test_create_method(self):
        iface = Interface('Calc')
        method = iface.create_method('sum', NativeType.INT32,
            arg_tuples=[('i0', NativeType.INT32), ('i1', NativeType.INT32)])

        assert [method] == iface.methods
        assert method.name == 'sum'
        assert method.result is NativeType.INT32
        assert method.args[0].name == 'i0'
        assert method.args[1].name == 'i1'

    def test_referenced_types(self):
        exc = Message('Exception', is_exception=True)
        iface = Interface('Interface', exc=exc)
        iface.create_method('method', arg_tuples=[
            ('arg0', NativeType.INT32), ('arg1', NativeType.INT64)
        ])
        iface.create_method('self', result=iface)

        assert len(iface.referenced_types) == 4
        assert iface.referenced_types[0] == exc
        assert iface.referenced_types[1] == NativeType.VOID
        assert iface.referenced_types[2] == NativeType.INT32
        assert iface.referenced_types[3] == NativeType.INT64
    
    def test_link(self):
        module = Module('test')
        iface = Interface('Interface', exc='exc')
        iface.create_method('method', 'result', [('arg', 'arg_type')])

        errors = iface.link(module)
        assert iface.module is module
        assert len(errors) == 3
        assert iface.methods[0].interface is iface

    # validate_base

    def test_validate_base__ok(self):
        base = Interface('Base')
        interface = Interface('Interface', base=base)

        errors = interface.validate()
        assert not errors

    def test_validate_base__circular_inheritance(self):
        base = Interface('Base')
        interface = Interface('Interface', base=base)
        base.base = interface

        errors = interface.validate()
        assert 'circular inheritance' in errors[0]

    def test_validate_base__base_not_interface(self):
        message = Message('Message')
        interface = Interface('Interface', base=message)

        errors = interface.validate()
        assert 'base must be an interface' in errors[0]

    def test_validate_base__base_must_be_declared_before_subtype(self):
        base = Interface('Base')
        interface = Interface('Interface', base=base)

        module = Module('module', definitions=[interface, base])
        module.link()

        errors = interface.validate()
        assert 'must be declared after its base' in errors[0]

    def test_validate_base__prevent_base_from_dependent_module(self):
        base = Interface('Base')
        interface = Interface('Interface', base=base)

        module0 = Module('module0', definitions=[interface])
        module0.link()

        module1 = Module('module1', definitions=[base])
        module1.add_imported_module('module0', module0)
        module1.link()

        errors = interface.validate()
        assert 'cannot inherit Base, it is in a dependent module "module1"' in errors[0]

    # validate_exc

    def test_validate_exc__tries_to_throw_non_exception(self):
        '''Should prevent setting interface exception to a non-exception type.'''
        nonexc = NativeType.INT32
        iface = Interface('Interface', exc=nonexc)

        errors = iface.validate()
        assert 'exc must be an exception' in errors[0]

    # validate_methods

    def test_validate_methods__duplicate(self):
        iface0 = Interface('Interface0')
        iface0.create_method('method')
        iface0.create_method('method')

        errors = iface0.validate()
        assert 'duplicate method' in errors[0]

    def test_validate_methods__duplicate_inherited_methods(self):
        base = Interface('Base')
        base.create_method('method')

        interface = Interface('Interface', base=base)
        interface.create_method('method')

        errors = interface.validate()
        assert 'duplicate method' in errors[0]


class TestMethod(unittest.TestCase):
    def test_validate__required_result(self):
        method = Method('method', result=None)
        errors = method.validate()

        assert 'method result required' in errors[0]

    def test_validate__result_reference(self):
        method = Method('method', result=references.ListReference(NativeType.VOID))
        errors = method.validate()

        assert 'List element must be a data type' in errors[0]

    def test_validate__post_must_be_terminal(self):
        result = Interface('Interface')
        method = Method('method', result, is_post=True)

        errors = method.validate()
        assert '@post method must be terminal' in errors[0]

    def test_validate__duplicate_args(self):
        method = Method('method')
        method.create_arg('arg', NativeType.INT32)
        method.create_arg('arg', NativeType.INT32)

        errors = method.validate()
        assert 'duplicate argument' in errors[0]

    def test_validate__prevent_post_args_when_method_not_post(self):
        method = Method('method')
        method.create_arg('arg', NativeType.INT32, is_post=True)

        errors = method.validate()
        assert '@post arguments can be declared only in @post methods' in errors[0]

    def test_validate__prevent_query_when_method_post(self):
        method = Method('method', is_post=True)
        method.create_arg('arg', NativeType.INT32, is_query=True)

        errors = method.validate()
        assert '@query arguments can be declared only in terminal non-post methods' in errors[0]

class TestMethodArg(unittest.TestCase):
    def test_link(self):
        arg = MethodArg('arg', 'module.Message')
        method = Method('method')

        errors = arg.link(method)
        assert arg.method is method
        assert 'Type not found' in errors[0]

    def test_validate__type_reference(self):
        arg = MethodArg('arg', references.ListReference(NativeType.VOID))
        errors = arg.validate()

        assert 'List element must be a data type' in errors[0]

    def test_validate__type_required(self):
        arg = MethodArg('arg', None)

        errors = arg.validate()
        assert 'argument type required' in errors[0]

    def test_validate__is_data_type(self):
        iface = Interface('Interface')
        arg = MethodArg('arg', iface)

        errors = arg.validate()
        assert 'argument must be a data type' in errors[0]

    def test_validate__cannot_be_post_and_query(self):
        arg = MethodArg('arg', NativeType.STRING, is_query=True, is_post=True)
        errors = arg.validate()

        assert 'argument cannot be both @query and @post' in errors[0]
