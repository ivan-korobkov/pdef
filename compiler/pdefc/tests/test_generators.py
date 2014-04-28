# encoding: utf-8
import argparse
import unittest

from pdefc.generators import ModuleMapper, PrefixMapper, GeneratorCli


class TestGeneratorCli(unittest.TestCase):
    def test_parse_prefix_args(self):
        args = argparse.Namespace()
        args.prefixes = ['namespace:Ns']

        cli = GeneratorCli()
        result = cli._parse_prefix_args(args)
        assert result == [('namespace', 'Ns')]

    def test_parse_module_args(self):
        args = argparse.Namespace()
        args.modules = ['pdef.test:io.pdef']

        cli = GeneratorCli()
        result = cli._parse_module_args(args)
        assert result == [('pdef.test', 'io.pdef')]


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
