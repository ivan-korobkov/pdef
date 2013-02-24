# encoding: utf-8
from pdef.lang import *
from pdef.lang.tests.test import PdefTest


class TestPackage(PdefTest):
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


class TestModule(PdefTest):
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
