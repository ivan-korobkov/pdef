# encoding: utf-8
import unittest
from pdef_compiler import ast
from pdef_compiler.lang import *
from pdef_compiler.linker import *


class TestLookup(unittest.TestCase):
    def setUp(self):
        self.lookup = Lookup()

    def test_find__value(self):
        '''Should find a native type by its ref.'''
        module = Module('test')

        int64, errors = self.lookup.find(ast.ValueRef(Type.INT64), module)
        assert int64 is NativeTypes.INT64
        assert not errors

    def test_find__list(self):
        '''Should create and link a list by its ref.'''
        module = Module('test')
        list0, errors = self.lookup.find(ast.ListRef(ast.ValueRef(Type.STRING)), module)

        assert isinstance(list0, List)
        assert list0.element is NativeTypes.STRING
        assert not errors

    def test_find__set(self):
        '''Should create and link a set by its ref.'''
        module = Module('test')
        set0, errors = self.lookup.find(ast.SetRef(ast.ValueRef(Type.FLOAT)), module)

        assert isinstance(set0, Set)
        assert set0.element is NativeTypes.FLOAT
        assert not errors

    def test_find__map(self):
        '''Should create and link a map by its ref.'''
        module = Module('test')
        map0, errors = self.lookup.find(ast.MapRef(ast.ValueRef(Type.STRING),
                                                   ast.ValueRef(Type.INT32)),module)

        assert isinstance(map0, Map)
        assert map0.key is NativeTypes.STRING
        assert map0.value is NativeTypes.INT32
        assert not errors

    def test_find__user_definition(self):
        '''Should find up a user-defined definition by its reference.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefRef('Test')
        result, errors = self.lookup.find(ref, module)
        assert def0 is result
        assert not errors

    def test_find__enum_value(self):
        '''Should find an enum value by its name.'''
        enum = Enum('Number')
        one = enum.add_value('One')

        module = Module('test')
        module.add_definition(enum)

        def0, errors = self.lookup.find(ast.DefRef('Number.One'), module)
        assert def0 is one
        assert not errors

    def test_find__imported_definition(self):
        '''Should find an imported definition.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module0 = Module('test.module0')
        module0.add_definition(def0)

        module1 = Module('module1')
        module1.create_import('test.module0', module0)

        ref = ast.DefRef('test.module0.Test')
        result, errors = self.lookup.find(ref, module1)
        assert result is def0
        assert not errors

    def test_find__imported_enum_value(self):
        '''Should find an imported enum value.'''
        enum = Enum('Number')
        one = enum.add_value('One')

        module0 = Module('test.module0')
        module0.add_definition(enum)

        module1 = Module('module1')
        module1.create_import('module0', module0)

        ref = ast.DefRef('module0.Number.One')
        result, errors = self.lookup.find(ref, module1)
        assert result is one
        assert not errors
