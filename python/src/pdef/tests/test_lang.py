# encoding: utf-8
import unittest
from mock import Mock
from pdef.lang import *


class Test(unittest.TestCase):
    def test(self):
        int32 = Native('int32')
        string = Native('string')
        List = Native('List')
        List.add_variables(Variable('E'))

        builtin_types = Module('pdef.types')
        builtin_types.add_definitions(int32, string, List)

        builtin = Package('pdef', None)
        builtin.add_modules(builtin_types)

        msg = Message('Message1')
        msg.add_fields(Field('int', Reference('int32')))
        msg.add_fields(Field('str', Reference('string')))
        msg.add_fields(Field('list', Reference('List', Reference('string'))))
        msg.add_fields(Field('msg2', Reference('module2.Message2')))

        module1 = Module('test.module1')
        module1.add_imports(ModuleReference('test.module2', 'module2'))
        module1.add_definitions(msg)

        msg2 = Message('Message2', declared_fields=[
            Field('circular', Reference('test.module1.Message1'))
        ])
        module2 = Module('test.module2',
            imports=[ModuleReference('test.module1')],
            definitions=[msg2])

        pkg = Package('test', builtin)
        pkg.add_modules(module1, module2)
        pkg.build()

        msg_fields = msg.declared_fields
        assert msg_fields['int'].type == int32
        assert msg_fields['str'].type == string
        assert msg_fields['msg2'].type == msg2
        assert msg_fields['list'].type.rawtype == List
        assert list(msg_fields['list'].type.variables) == [string]

        msg2_fields = msg2.declared_fields
        assert msg2_fields['circular'].type == msg


class TestPackage(unittest.TestCase):
    def test_symbol(self):
        '''Should look up a symbol in a builtin package.'''
        int32 = Native('int32')
        builtin = Package('builtin')
        builtin.add_modules(Module('builtin.types', definitions=[int32]))

        pkg = Package('test', builtin)
        symbol = pkg.lookup('int32')
        assert symbol is int32

    def test_parameterized_symbol(self):
        List = Native('List', variables=[Variable('T')])
        int32 = Native('int32')

        pkg = Package('test')
        ptype = pkg.parameterized_symbol(List, int32)

        assert ptype.rawtype is List
        assert list(ptype.variables) == [int32]
        assert ptype in pkg.pqueue
        assert (List, (int32, )) in pkg.parameterized

    def test_parameterized_symbol_present(self):
        List = Native('List', variables=[Variable('T')])
        int32 = Native('int32')

        pkg = Package('test')
        ptype = pkg.parameterized_symbol(List, int32)
        ptype2 = pkg.parameterized_symbol(List, int32)
        assert ptype2 is ptype


class TestModule(unittest.TestCase):
    def test_symbol_from_definitions(self):
        '''Should look up a symbol in the module's definitions.'''
        int32 = Native('int32')
        module = Module('test')
        module.add_definitions(int32)

        symbol = module.lookup('int32')
        assert symbol is int32

    def test_symbol_from_imports(self):
        '''Should look up a symbol in the module's imports definitions.'''
        int32 = Native('int32')
        imported = Module('imported')
        imported.add_definitions(int32)

        module = Module('with_import')
        module.add_imports(imported)

        symbol = module.lookup('imported.int32')
        assert symbol is int32


class TestModuleReference(unittest.TestCase):
    def test_link(self):
        '''Should look up and return a module.'''
        module = Module('package.module')
        module2 = Module('package.module2')
        package = Package('package')
        package.add_modules(module, module2)

        imp = ModuleReference('package.module', 'module')
        imp.parent = module2
        imp.link()
        assert imp == module


class TestReference(unittest.TestCase):
    def test_link(self):
        '''Should look up a raw type when linking.'''
        int32 = Native('int32')
        module = Module('test')
        module.add_definitions(int32)

        ref = Reference('int32')
        ref.parent = module
        ref.link()
        assert ref == int32

    def test_link_not_found(self):
        '''Should add a type not found error.'''
        module = Module('test')
        ref = Reference('not_found')
        ref.link()

        assert ref.delegate is None
        assert len(ref.errors) == 1

    def test_link_parameterized_symbol(self):
        '''Should add a wrong number of arguments error.'''
        int32 = Native('int32')
        List = Native('List')
        List.add_variables(Variable('T'))

        module = Module('test')
        module.add_definitions(int32, List)

        mock = Mock()
        module.parent = mock

        ref = Reference('List')
        ref.parent = module
        ref.add_variables(Reference('int32'))
        ref.link()

        mock.package.parameterized_symbol(List, [int32])


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
        msg = Message('A', polymorphism=MessagePolymorphism(Reference('field'), 'A'))
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
        MyList.add_fields(Field('items', Reference('List', Reference('Set', Reference('E')))))

        MyMessage = Message('MyMessage')
        MyMessage.add_fields(Field('list', Reference('MyList', Reference('int32'))))

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
        Node.add_fields(Field('root', Reference('RootNode', Reference('N'))))

        R = Variable('R')
        Root = Message('RootNode')
        Root.add_variables(R)
        Root.set_base(Reference('Node', Reference('R')), 'root')

        Graph = Message('Graph')
        Graph.add_fields(Field('node', Reference('Node', Reference('int32'))))

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
