# encoding: utf-8
import unittest
from pdefc.ast import NativeType
from pdefc.ast.interfaces import *
from pdefc.ast.messages import Message
from pdefc.ast.modules import Module


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

        assert [method] == iface.declared_methods
        assert method.name == 'sum'
        assert method.result is NativeType.INT32
        assert method.args[0].name == 'i0'
        assert method.args[1].name == 'i1'

    def test_link(self):
        iface = Interface('Interface', exc='exc')
        iface.create_method('method', 'result', [('arg', 'arg_type')])
        errors = iface.link(lambda name: None)

        assert len(errors) == 3

    # validate_exc

    def test_validate_exc__tries_to_throw_non_exception(self):
        '''Should prevent setting interface exception to a non-exception type.'''
        nonexc = NativeType.INT32
        iface = Interface('Interface', exc=nonexc)

        errors = iface.validate()
        assert 'exc must be an exception' in errors[0]

    # validate_methods

    def test_validate_methods__duplicates(self):
        iface0 = Interface('Interface0')
        iface0.create_method('method')
        iface0.create_method('method')

        errors = iface0.validate()
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

    def test_validate__post_must_be_remote(self):
        result = Interface('Interface')
        method = Method('method', result, is_post=True)

        errors = method.validate()
        assert '@post method must be remote' in errors[0]

    def test_validate__duplicate_args(self):
        method = Method('method')
        method.create_arg('arg', NativeType.INT32)
        method.create_arg('arg', NativeType.INT32)

        errors = method.validate()
        assert 'duplicate argument' in errors[0]


class TestMethodArg(unittest.TestCase):
    def test_link(self):
        scope = lambda name: None
        arg = MethodArg('arg', 'module.Message')

        errors = arg.link(scope)
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
