# encoding: utf-8
import os.path
import unittest
from pdef.parser import Parser


class TestParser(unittest.TestCase):

    def _read(self, filename):
        filepath = os.path.join(os.path.dirname(__file__), filename)
        with open(filepath, 'r') as f:
            return f.read()

    def _filepath(self, filename):
        return os.path.join(os.path.dirname(__file__), filename)

    def test_parse(self):
        '''Should parse a test pdef file.'''
        s = self._filepath('test.pdef')
        parser = Parser()
        module = parser.parse_file(s)

        imports = module.imports
        assert len(imports) == 2
        assert imports[0].module_name == 'another_module'
        assert imports[1].module_name == 'pdef.test.imports'
        assert imports[0].names == ('Message0',)
        assert imports[1].names == ('Exc0', 'Exc1', 'Interface0')

        defs = dict((d.name, d) for d in module.definitions)
        assert module.name == 'pdef.test'
        assert 'Message' in defs
        assert 'Enum' in defs
        assert 'Interface' in defs
