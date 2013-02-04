# encoding: utf-8
import os.path
import unittest
from pdef.parser import Parser


class TestParser(unittest.TestCase):

    def _read(self, filename):
        filepath = os.path.join(os.path.dirname(__file__), filename)
        with open(filepath, 'r') as f:
            return f.read()

    def test_parse_messages(self):
        '''Should parse a test file without errors.'''
        s = self._read('messages.pdef')
        parser = Parser()
        result = parser.parse(s)
        assert result

    def test_parse_interfaces(self):
        '''Should parse a test file with interfaces without errors.'''
        return
        s = self._read('interfaces.pdef')
        parser = Parser()
        result = parser.parse(s)
        assert result
