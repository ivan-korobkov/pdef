# encoding: utf-8
import os.path
import unittest
from pdef.parser import Parser


class TestParser(unittest.TestCase):

    def _open(self, filename):
        filepath = os.path.join(os.path.dirname(__file__), filename)
        return open(filepath, 'r')

    def _read_simple(self):
        with self._open('simple.pdef') as f:
            return f.read()

    def test_parse_no_errors(self):
        '''Should parse a test file without errors.'''
        s = self._read_simple()
        parser = Parser()
        result = parser.parse(s)
        assert result

