# encoding: utf-8
import unittest

from pdef_compiler.generator import Namespaces


class TestNameMapper(unittest.TestCase):
    def test_map(self):
        mapper = Namespaces({'module.name': 'module_name'})
        print mapper.map('module.name')
        assert mapper.map('module.name') == 'module_name'
        assert mapper.map('module.name.submodule') == 'module_name.submodule'

    def test_map__empty(self):
        mapper = Namespaces()
        assert mapper.map('module') == 'module'
