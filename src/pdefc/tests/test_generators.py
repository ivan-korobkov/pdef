# encoding: utf-8
import unittest

from pdefc.generators import ModuleMapper, PrefixMapper


class TestPrefixMapper(unittest.TestCase):
    def test_get_prefix(self):
        mapper = PrefixMapper([('module.submodule', 'Sb'), ('module', 'Md')])
        assert mapper.get_prefix('module') == 'Md'
        assert mapper.get_prefix('module.submodule.Message') == 'Sb'


class TestModuleMapper(unittest.TestCase):
    def test_get_module(self):
        mapper = ModuleMapper([('module.name', 'module_name')])
        assert mapper('module.name') == 'module_name'
        assert mapper('module.name.submodule') == 'module_name.submodule'

    def test_get_module__empty(self):
        mapper = ModuleMapper()
        assert mapper('module') == 'module'
