# encoding: utf-8
import unittest
from pdef import ast
from pdef.lang import Package, Pdef, Module


class TestPackage(unittest.TestCase):
    def setUp(self):
        self.node = ast.Package('test', version='1.0',
            dependencies=('dep1', 'dep2'),
            modules=(ast.Module('test.module1'), ast.Module('test.module2')))

    def test_from_node(self):
        package = Package.from_node(self.node)
        assert package.node == self.node
        assert package.name == self.node.name
        assert len(package.modules) == 2
        assert 'test.module1' in package.modules
        assert 'test.module2' in package.modules

    def test_link(self):
        dep1 = Package('dep1')
        dep2 = Package('dep2')
        pdef = Pdef()
        pdef.add_packages(dep1, dep2)

        package = Package.from_node(self.node, pdef)
        package.link()
        assert package.dependencies['dep1'] is dep1
        assert package.dependencies['dep2'] is dep2

    def test_add_dependency(self):
        dep1 = Package('dep')
        package = Package('test')
        package.add_dependency(dep1)
        assert 'dep' in package.symbols
        assert package.dependencies['dep'] is dep1

        # Duplicate
        dep2 = Package('dep')
        self.assertRaises(ValueError, package.add_dependency, dep2)

    def test_add_module(self):
        module = Module('test.module')
        package = Package('test')
        package.add_module(module)
        assert 'test.module' in package.symbols
        assert package.modules['test.module'] is module

        # Duplicate
        module2 = Module('test.module')
        self.assertRaises(ValueError, package.add_module, module2)

    def test_add_module_wrong_name(self):
        module = Module('module')
        package = Package('test')
        self.assertRaises(ValueError, package.add_module, module)
