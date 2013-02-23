# encoding: utf-8
import unittest
from mock import Mock
from pdef.lang import *


class TestModuleReference(unittest.TestCase):
    def test_link(self):
        '''Should look up and return a module.'''
        module = Module('package.module')
        module2 = Module('package.module2')
        package = Package('package')
        package.add_modules(module, module2)

        imp = ImportRef('package.module', 'module')
        imp.parent = module2
        imp.link()
        assert imp == module


class TestReference(unittest.TestCase):
    def test_link(self):
        '''Should look up a raw type when linking.'''
        int32 = Native('int32')
        module = Module('test')
        module.add_definitions(int32)

        ref = Ref('int32')
        ref.parent = module
        ref.link()
        assert ref == int32

    def test_link_not_found(self):
        '''Should add a type not found error.'''
        module = Module('test')
        ref = Ref('not_found')
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

        ref = Ref('List')
        ref.parent = module
        ref.add_variables(Ref('int32'))
        ref.link()

        mock.package.parameterized_symbol(List, [int32])
