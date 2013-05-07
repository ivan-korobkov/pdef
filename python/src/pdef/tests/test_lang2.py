# encoding: utf-8
import unittest
from pdef import ast
from pdef.consts import Type
from pdef.lang2 import *


class TestModule(unittest.TestCase):
    def test_lookup_value(self):
        '''Should lookup a value by its ref.'''
        module = Module('test')
        int64 = module.lookup(ast.Ref(Type.INT64))
        assert int64 is Values.INT64

    def test_lookup_list(self):
        '''Should create and link a list by its ref.'''
        module = Module('test')
        list0 = module.lookup(ast.ListRef(ast.Ref(Type.STRING)))
        assert isinstance(list0, List)
        assert list0.element is Values.STRING

    def test_lookup_set(self):
        '''Should create and link a set by its ref.'''
        module = Module('test')
        set0 = module.lookup(ast.SetRef(ast.Ref(Type.FLOAT)))
        assert isinstance(set0, Set)
        assert set0.element is Values.FLOAT

    def test_lookup_map(self):
        '''Should create and link a map by its ref.'''
        module = Module('test')
        map0 = module.lookup(ast.MapRef(ast.Ref(Type.STRING), ast.Ref(Type.INT32)))
        assert isinstance(map0, Map)
        assert map0.key is Values.STRING
        assert map0.value is Values.INT32

    def test_lookup_user_defined(self):
        '''Should look up a user-defined definition by its reference.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefinitionRef('Test')
        result = module.lookup(ref)
        assert def0 is result

    def test_lookup_link(self):
        '''Should look up a definition by its reference and link the definition.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefinitionRef('Test')
        result = module.lookup(ref)
        assert result._linked


class TestEnum(unittest.TestCase):
    def test_from_ast(self):
        '''Should create an enum from an AST node.'''
        node = ast.Enum('Number', values=('ONE', 'TWO', 'THREE'))
        enum = Enum.from_ast(node)

        assert len(enum.values) == 3
        assert 'ONE' in enum.values
        assert 'TWO' in enum.values
        assert 'THREE' in enum.values

    def test_add_value(self):
        '''Should add to enum a new value by its name.'''
        enum = Enum('Number')
        one = enum.add_value('ONE')

        assert isinstance(one, EnumValue)
        assert one.name == 'ONE'
        assert one.enum is enum
