# encoding: utf-8
import unittest
from pdef import ast
from pdef.lang import Enum, EnumValue


class TestEnum(unittest.TestCase):
    def setUp(self):
        self.node = ast.Enum('Type', values=(
            'BASE', 'OBJECT', 'EVENT'
            ))

    def test_from_node(self):
        enum = Enum.from_node(self.node)
        assert enum.name == self.node.name
        assert 'BASE' in enum.values
        assert 'OBJECT' in enum.values
        assert 'EVENT' in enum.values

    def test_duplicate_value(self):
        enum = Enum('Type')
        EnumValue('BASE', enum)
        self.assertRaises(ValueError, EnumValue, 'BASE', enum)

    def test_contains(self):
        enum = Enum('Type')
        base = EnumValue('BASE', enum)
        assert base in enum

        enum2 = Enum('Type2')
        assert base not in enum2
