# encoding: utf-8
import unittest
from mock import Mock
from pdef.lang import *


class TestVariable(unittest.TestCase):
    def test_bind(self):
        int32 = Native('int32')
        var = Variable('T')
        bound = var.bind({var: int32})
        assert bound == int32


class TestType(unittest.TestCase):
    def test_symbol_variables(self):
        '''Should return variables as symbols.'''
        # Message<T>
        t = Variable('T')
        msg = Type('Message')
        msg.add_variables(t)

        symbol = msg.lookup('T')
        assert symbol is t


class TestParameterizedType(unittest.TestCase):
    def test_bind(self):
        # Parameterized Map<int32, V>
        int32 = Native('int32')
        string = Native('string')

        K = Variable('K')
        V = Variable('V')
        Map = Native('Map')
        Map.add_variables(K, V)
        parent = Mock()

        ptype = ParameterizedType(Map, int32, V)
        ptype.parent = parent

        # Bind V to string
        ptype.bind({V: string})

        # Should get Map<int32, string>
        parent.package.parameterized_symbol.assert_called_with(Map, int32, string)
