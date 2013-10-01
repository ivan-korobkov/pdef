# encoding: utf-8
import unittest


class TestPackage(unittest.TestCase):
    def test_parse_module__node(self):
        '''Should parse a module from an AST node and add it to this package.'''
        module_node = ast.File('module', definitions=[
            ast.Enum('Enum', values=['One', 'Two'])
        ])

        package = Package()
        package.parse_module(module_node)

        assert package.find_module_or_raise('module')

