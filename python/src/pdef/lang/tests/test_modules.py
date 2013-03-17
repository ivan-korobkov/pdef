# encoding: utf-8
import unittest
from pdef import ast
from pdef.lang import *


class TestModule(unittest.TestCase):
    def setUp(self):
        self.node = ast.Module('package.module',
            imports=(
                ast.ImportRef('package.module2', 'module2'),
                ast.ImportRef('package.module3')
            ),
            definitions=(
                ast.Native('int32'),
                ast.Native('int64')
            ))

    def test_from_node(self):
        module = Module.from_node(self.node)
        assert module.node is self.node
        assert module.name == self.node.name
        assert 'int32' in module.definitions
        assert 'int64' in module.definitions

    def test_link(self):
        module2 = Module('package.module2')
        module3 = Module('package.module3')
        package = Package('package')
        package.add_modules(module2, module3)

        module = Module.from_node(self.node, package)
        module.link()

        assert 'module2' in module.symbols
        assert 'package.module3' in module.symbols
        assert module.imports['module2'] is module2
        assert module.imports['package.module3'] is module3
