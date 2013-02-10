# encoding: utf-8
import unittest
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
        pkg.link()

        msg_fields = msg.declared_fields
        assert msg_fields['int'].type == int32
        assert msg_fields['str'].type == string
        assert msg_fields['msg2'].type == msg2
        assert msg_fields['list'].type.declaration == List
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
        symbol = pkg.symbol('int32')
        assert symbol is int32


class TestModule(unittest.TestCase):
    def test_symbol_from_definitions(self):
        '''Should look up a symbol in the module's definitions.'''
        int32 = Native('int32')
        module = Module('test')
        module.add_definitions(int32)

        symbol = module.symbol('int32')
        assert symbol is int32

    def test_symbol_from_imports(self):
        '''Should look up a symbol in the module's imports definitions.'''
        int32 = Native('int32')
        imported = Module('imported')
        imported.add_definitions(int32)

        module = Module('with_import')
        module.add_imports(imported)

        symbol = module.symbol('imported.int32')
        assert symbol is int32

    def test_link_imports(self):
        '''Should replace the import references, and link the modules.'''
        module1 = Module('module1')
        module2 = Module('module2')

        module3 = Module('module3')
        module3.add_imports(module1, ModuleReference('module2'))

        pkg = Package('test')
        pkg.add_modules(module1, module2, module3)

        module3.link()
        assert list(module3.imports) == [module1, module2]


class TestModuleReference(unittest.TestCase):
    def test_link(self):
        '''Should look up and return a module.'''
        module = Module('package.module')
        package = Package('package')
        package.add_modules(module)

        imp = ModuleReference('package.module', 'module')
        imp.parent = module

        linked = imp.link()
        assert linked == module


class TestReference(unittest.TestCase):
    def test_link_rawtype(self):
        '''Should look up a raw type when linking.'''
        int32 = Native('int32')
        module = Module('test')
        module.add_definitions(int32)

        ref = Reference('int32')
        ref.parent = module
        symbol = ref.link()
        assert symbol is int32

    def test_link_not_found(self):
        '''Should add a type not found error.'''
        module = Module('test')
        ref = Reference('not_found')
        ref.parent = module

        symbol = ref.link()
        assert symbol is None
        assert len(module.errors) == 1

    def test_link_wrong_number_of_args(self):
        '''Should add a wrong number of arguments error.'''
        List = Native('List')
        List.add_variables(Variable('T'))

        module = Module('test')
        module.add_definitions(List)

        ref = Reference('List')
        ref.parent = module
        ref.add_args(Reference('K'), Reference('V'))

        symbol = ref.link()
        assert symbol is None
        assert len(module.errors) == 1
        assert module.errors[0].endswith('wrong number of generic arguments')

    def test_link_specialize(self):
        '''Should create a specialization with the linked rawtype and arguments.'''
        int32 = Native('int32')
        string = Native('string')

        # List<T>
        List = Native('List')
        List.add_variables(Variable('T'))

        # Map<K, V>
        Map = Native('Map')
        Map.add_variables(Variable('K'), Variable('V'))

        module = Module('test')
        module.add_definitions(int32, string, List, Map)

        ref = Reference('Map')
        ref.parent = module
        ref.add_args(Reference('int32'))
        ref.add_args(Reference('List', Reference('string')))

        # Reference Map<int32, List<string>>
        special = ref.link()
        assert isinstance(special, Specialization)
        assert special.rawtype is Map

        args = list(special.args)
        assert len(args) == 2
        assert args[0] is int32

        special_list = args[1]
        assert isinstance(special_list, Specialization)
        assert special_list.rawtype is List
        assert len(special_list.args) == 1
        assert list(special_list.args)[0] is string


class TestMessage(unittest.TestCase):
    def test_link(self):
        '''Should link the declared message fields.'''
        msg = Message('Message')
        msg.add_fields(Field('field', Reference('Message2')))

        msg2 = Message('Message2')
        msg2.add_fields(Field('field', Reference('Message')))

        module = Module('test')
        module.add_definitions(msg, msg2)
        module.link()

        assert msg.declared_fields['field'].type == msg2;
        assert msg2.declared_fields['field'].type == msg;

    def test_link_variables(self):
        '''Should link the fields to message variables.'''
        # Message<T>
        t = Variable('T')
        field = Field('field', Reference('T'))
        msg = Message('Message')
        msg.add_variables(t)
        msg.add_fields(field)
        msg.link()

        assert field.type is t

    def test_link_circular(self):
        '''Should link the fields and bases with cirular references.'''
        # Node<V>:
        #   RootNode<V> root
        # RootNode<T> extends Node<T>
        node = Message('Node')
        node.add_variables(Variable('V'))
        node.add_fields(Field('root', Reference('RootNode', Reference('V'))))

        root = Message('RootNode')
        root.add_variables(Variable('T'))
        root.set_base(Reference('Node', Reference('Node', Reference('T'))))

        module = Module('test')
        module.add_definitions(node, root)
        module.link()
