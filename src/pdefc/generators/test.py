# encoding: utf-8
import unittest

from pdefc.generators import Namespace


class TestNamespace(unittest.TestCase):
    def test_map(self):
        mapper = Namespace({'module.name': 'module_name'})
        assert mapper.map('module.name') == 'module_name'
        assert mapper.map('module.name.submodule') == 'module_name.submodule'

    def test_map__empty(self):
        mapper = Namespace()
        assert mapper.map('module') == 'module'
