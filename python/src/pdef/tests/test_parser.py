# encoding: utf-8
import os.path
import unittest
from pdef.ast import Module
from pdef.parser import ModuleParser


class TestParser(unittest.TestCase):

    def _read(self, filename):
        filepath = os.path.join(os.path.dirname(__file__), filename)
        with open(filepath, 'r') as f:
            return f.read()

    def test_parse_module_with_messages(self):
        '''Should parse a test file with messages.'''
        s = self._read('messages.pdef')
        parser = ModuleParser()
        module = parser.parse(s)
        assert module
        assert isinstance(module, Module)
        assert module.name == 'test.messages'

        dd = list(module.definitions)
        assert len(dd) == 15
        assert dd[0].name == 'int32'
        assert dd[1].name == 'string'
        assert dd[10].name == 'Note'
