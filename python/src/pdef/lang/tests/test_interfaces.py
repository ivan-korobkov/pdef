# encoding: utf-8
import unittest
from pdef import ast
from pdef.lang import *


class TestInterface(unittest.TestCase):
    def test_from_node(self):
        '''Should init an interface from a node.'''
        int32 = Native('int32')
        generic = Interface('GenericApp')
        module = Module('module')
        module.add_definitions(int32, generic)

        node = ast.Interface('App', variables=['T'],
            bases=[ast.Ref('GenericApp')],
            methods=[
                ast.Method('sum', args=[
                    ast.MethodArg('v0', ast.Ref('int32')),
                    ast.MethodArg('v1', ast.Ref('int32'))]),
                ast.Method('hash', args=[
                    ast.MethodArg('v', ast.Ref('T'))
                ])
            ])

        iface = Interface.from_node(node, module)
        iface.init()

        assert 'T' in iface.variables
        assert generic in iface.bases
        assert 'sum' in iface.methods
        assert 'hash' in iface.methods

    def test_add_base(self):
        '''Should add a base to interface bases.'''
        base = Interface('Base')
        iface = Interface('Iface')
        iface.add_base(base)
        assert base in iface.bases

    def test_add_base_multiple(self):
        '''Should add multiple bases to interface bases.'''
        base0 = Interface('base0')
        base1 = Interface('base1')
        iface = Interface('Iface')
        iface.add_base(base0)
        iface.add_base(base1)
        assert base0 in iface.bases
        assert base1 in iface.bases

    def test_add_base_circular(self):
        '''Should prevent circular interface inheritance.'''
        a = Interface('A')
        b = Interface('B')
        c = Interface('C')

        b.add_base(a)
        c.add_base(b)
        self.assertRaises(ValueError, a.add_base, c)

    def test_add_base_methods(self):
        '''Should add base methods to interface methods.'''
        text = Native('text')
        echo = Method('echo', args=[MethodArg('value', text)])
        a = Interface('A')
        a.add_method(echo)

        b = Interface('B')
        b.add_base(a)
        assert 'echo' in b.methods

    def test_add_method(self):
        '''Should add a method to interface declared methods.'''
        int32 = Native('int32')
        square = Method('square', args=[MethodArg('value', int32)])

        a = Interface('A')
        a.add_method(square)
        assert 'square' in a.methods
        assert 'square' in a.declared_methods

    def test_add_method_clash_with_base(self):
        '''Should prevent adding a method which name clashes with a base method name.'''
        text = Native('text')
        echo = Method('echo', args=[MethodArg('value', text)])
        a = Interface('A')
        a.add_method(echo)

        b = Interface('B')
        b.add_base(a)
        echo2 = Method('echo', args=[MethodArg('value', text)])
        self.assertRaises(ValueError, b.add_method, echo2)

    def test_parameterize(self):
        '''Should return a parameterized interface.'''
        var = Variable('T')
        iface = Interface('Generic', variables=[var])
        iface.add_method(Method('generic', args=[MethodArg('arg', var)]))

        int32 = Native('int32')
        pface = iface.parameterize(int32)
        assert pface.is_parameterized
        assert pface.rawtype is iface
        assert pface.variables[var] is int32


class TestParameterizedInterface(unittest.TestCase):
    def test_init(self):
        '''Should bind bases and methods when initialized.'''
        var0 = Variable('E')
        base = Interface('Base', variables=[var0])
        base.add_method(Method('method0', args=[MethodArg('arg', var0)]))

        var1 = Variable('T')
        iface = Interface('Generic', variables=[var1])
        iface.add_base(base.parameterize(var1))
        iface.add_method(Method('method1', args=[MethodArg('arg', var1)]))

        int32 = Native('int32')
        pface = iface.parameterize(int32)
        pface.init()
        assert pface.is_parameterized
        assert 'method0' in pface.methods
        assert 'method1' in pface.methods
        assert 'method0' not in pface.declared_methods
        assert 'method1' in pface.declared_methods
        assert pface.methods['method0'].args['arg'].type is int32
        assert pface.methods['method1'].args['arg'].type is int32


class TestMethod(unittest.TestCase):
    def test_add_arg(self):
        int32 = Native('int32')
        method = Method('method')
        method.add_arg(MethodArg('name', int32))
        assert 'name' in method.args

    def test_bind(self):
        int32 = Native('int32')
        var = Variable('T')

        method = Method('method')
        method.add_arg(MethodArg('name', var))

        bmethod = method.bind({var: int32})
        assert bmethod.is_parameterized
        assert bmethod.declaring_method is method
        assert bmethod.args['name'].type is int32
