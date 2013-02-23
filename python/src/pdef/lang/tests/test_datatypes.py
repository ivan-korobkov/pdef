# encoding: utf-8
import unittest
from pdef.lang import *


class TestNative(unittest.TestCase):
    def test_parameterize(self):
        t = Variable('T')
        List = Native('List')
        List.add_variables(t)

        string = Native('string')
        special = List.parameterize(string)
        assert special.rawtype is List
        assert list(special.variables) == [string]


class TestMessage(unittest.TestCase):
    def test_bases(self):
        msg = Message('Message')
        msg2 = Message('Message2', base=msg, base_type='msg2')
        msg3 = Message('Message3', base=msg2, base_type='msg3')

        assert list(msg3.bases) == [msg2, msg]
        assert list(msg2.bases) == [msg]

    def test_parameterize(self):
        '''Should create a parameterized message.'''
        t = Variable('T')
        msg = Message('Message')
        msg.add_variables(t)
        msg.add_fields(Field('field', t))

        int32 = Native('int32')
        pmsg = msg.parameterize(int32)
        assert pmsg.rawtype == msg
        assert list(pmsg.variables) == [int32]

    def test_compile_fields(self):
        int32 = Native('int32')
        f1 = Field('z', int32)
        msg = Message('A')
        msg.add_fields(f1)

        f2 = Field('y', int32)
        msg2 = Message('B', base=msg, base_type='b')
        msg2.add_fields(f2)

        f3 = Field('x', int32)
        msg3 = Message('C', base=msg2, base_type='c')
        msg3.add_fields(f3)

        msg3.compile_fields()
        assert list(msg3.fields) == [f1, f2, f3]

    def test_compile_fields_clash(self):
        int32 = Native('int32')
        f1 = Field('field', int32)
        msg = Message('A')
        msg.add_fields(f1)

        f2 = Field('field', int32)
        msg2 = Message('B', base=msg, base_type='b')
        msg2.add_fields(f2)

        self.assertRaises(ValueError, msg2.compile_fields)

    def test_check_circular_inheritance(self):
        msg = Message('Message')
        msg2 = Message('Message2', base=msg, base_type='msg2')
        msg3 = Message('Message3', base=msg2, base_type='msg3')
        msg.set_base(msg3, 'type')

        msg2.check_circular_inheritance()
        assert len(msg2.errors) == 1
        assert 'circular inheritance' in msg2.errors[0]

    def test_compile_base_type(self):
        msg = Message('A', polymorphism=MessagePolymorphism(Ref('field'), 'A'))
        msg2 = Message('B', base=msg, base_type='B')
        msg2.compile_base_type()

        assert msg.polymorphism.map == {'A': msg, 'B': msg2}


class TestParameterizedMessage(unittest.TestCase):
    def test_build(self):
        t = Variable('T')
        msg = Message('Message')
        msg.add_variables(t)
        msg.add_fields(Field('field', t))

        int32 = Native('int32')
        pmsg = msg.parameterize(int32)
        pmsg.build()

        assert pmsg.rawtype == msg
        assert pmsg.declared_fields['field'].type == int32


class TestParameterization(unittest.TestCase):
    def test_recursive(self):
        '''Should support recursive parameterization.'''
        # MyMessage:
        #   MyList<int32> list
        #
        # MyList<E>:
        #   List<Set<E>> items
        int32 = Native('int32')
        Set = Native('Set', variables=[Variable('T')])
        List = Native('List', variables=[Variable('T')])

        MyList = Message('MyList', variables=[Variable('E')])
        MyList.add_fields(Field('items', Ref('List', Ref('Set', Ref('E')))))

        MyMessage = Message('MyMessage')
        MyMessage.add_fields(Field('list', Ref('MyList', Ref('int32'))))

        module = Module('test.module')
        module.add_definitions(int32, Set, List, MyList, MyMessage)

        pkg = Package('test')
        pkg.add_modules(module)
        pkg.build()

        # MyList<int32>:
        #   List<Set<int32>> items
        pmylist = MyMessage.declared_fields['list'].type

        items = pmylist.declared_fields['items']
        plist = items.type
        assert plist.rawtype == List

        pset = list(plist.variables)[0]
        assert pset.rawtype == Set

        pelement = list(pset.variables)[0]
        assert pelement == int32

    def test_circular(self):
        '''Should support circular parameterization.'''
        # Node<N>:
        #   RootNode<N> root
        # RootNode<R> extends Node<R>
        # Graph:
        #   Node<int32> node
        int32 = Native('int32')

        N = Variable('N')
        Node = Message('Node')
        Node.add_variables(N)
        Node.add_fields(Field('root', Ref('RootNode', Ref('N'))))

        R = Variable('R')
        Root = Message('RootNode')
        Root.add_variables(R)
        Root.set_base(Ref('Node', Ref('R')), 'root')

        Graph = Message('Graph')
        Graph.add_fields(Field('node', Ref('Node', Ref('int32'))))

        module = Module('test')
        module.add_definitions(int32, Node, Root, Graph)

        pkg = Package('test')
        pkg.add_modules(module)
        pkg.build()

        # First, check that node-root circle.
        nroot = Node.declared_fields['root'].type
        assert nroot.rawtype is Root
        assert list(nroot.variables) == [N]
        assert nroot.base is Node

        # Second, check the bindings in Graph.
        gnode = Graph.declared_fields['node'].type
        assert gnode.rawtype == Node
        assert list(gnode.variables) == [int32]

        groot = gnode.declared_fields['root'].type
        assert groot.rawtype == Root
        assert list(gnode.variables) == [int32]

        gbase = groot.base
        assert gnode == gbase
