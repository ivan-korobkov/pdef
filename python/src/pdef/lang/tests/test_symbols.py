# encoding: utf-8
import unittest
from pdef.ast import Ref
from pdef.lang import Node


class TestNode(unittest.TestCase):
    def test_lookup_child(self):
        node = Node('node')
        symbol = Node('symbol')
        node.symbols.add(symbol)

        assert node.lookup('symbol') is symbol

    def test_lookup_child_relative(self):
        # node -> a -> b -> symbol
        node = Node('node')
        b = Node('a')
        c = Node('b')
        node.symbols.add(b)
        b.symbols.add(c)

        symbol = Node('symbol')
        c.symbols.add(symbol)

        assert node.lookup('a.b.symbol') is symbol

    def test_lookup_parent(self):
        # a -> b -> node
        #   -> c -> symbol
        node = Node('node')
        a = Node('a')
        b = Node('b')
        c = Node('c')

        node.parent = b
        b.parent = a
        c.parent = a
        a.symbols.add(b)
        a.symbols.add(c)

        symbol = Node('symbol')
        c.symbols.add(symbol)

        assert node.lookup('c.symbol') is symbol

    def test_lookup_ref(self):
        # node -> symbol
        node = Node('node')
        symbol = Node('symbol')
        node.symbols.add(symbol)

        ref = Ref('symbol')
        assert node.lookup(ref) is symbol

    def test_lookup_ref_with_variables(self):
        # node -> rawtype<k, v>
        #      -> int32
        #      -> int64
        class GenericNode(Node):
            variables = ('K', 'V')
            generic = True
            def parameterize(self, *vars):
                self.vars = vars
                return self

        rawtype = GenericNode('rawtype')
        int32 = Node('int32')
        int64 = Node('int64')

        node = Node('node')
        node.symbols.add(rawtype)
        node.symbols.add(int32)
        node.symbols.add(int64)

        ref = Ref('rawtype', variables=(Ref('int32'), Ref('int64')))
        generic = node.lookup(ref)
        assert generic is rawtype
        assert generic.vars == (int32, int64)

    def test_init_on_lookup(self):
        # node -> symbol
        class InitNode(Node):
            def init(self):
                self.inited = True

        symbol = InitNode('symbol')
        node = Node('node')
        node.symbols.add(symbol)

        node.lookup('symbol')
        assert symbol.inited
