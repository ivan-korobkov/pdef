# encoding: utf-8
import unittest
from pdef import ast
from pdef.lang import Native


class TestNative(unittest.TestCase):
    def setUp(self):
        self.node = ast.Native('list', variables=['E'])

    def test_from_node(self):
        native = Native.from_node(self.node)
        assert native.name == self.node.name
        assert 'E' in native.variables
