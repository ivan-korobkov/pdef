# encoding: utf-8
import os.path
import unittest
from pdef.compiler import parser


class TestParser(unittest.TestCase):

    def _read(self, filename):
        filepath = os.path.join(os.path.dirname(__file__), filename)
        with open(filepath, 'r') as f:
            return f.read()

    def _filepath(self, filename):
        return os.path.join(os.path.dirname(__file__), filename)

    def _parse(self, filename):
        path = self._filepath(filename)
        return parser.parse_file(path)

    def test_parse__messages(self):
        module = self._parse('sources/test_messages.pdef')
        assert module.name == 'pdef.test.messages';

    def test_parse__polymorphic_messages(self):
        module = self._parse('sources/test_polimorphic.pdef')
        assert module.name == 'pdef.test.polymorphic';

    def test_parse_interfaces(self):
        module = self._parse('sources/test_interfaces.pdef')
        assert module.name == 'pdef.test.interfaces';
