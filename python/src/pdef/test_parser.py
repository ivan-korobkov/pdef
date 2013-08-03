# encoding: utf-8
import os.path
import unittest
import pdef.parser


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
        module = pdef.parser.parse_file(s)

        assert module.name == 'io.pdef.test';
