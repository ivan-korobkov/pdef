# encoding: utf-8
import unittest
from mock import Mock

from pdefc.compiler import Compiler
from pdefc.exc import CompilerException
from pdefc.packages import PackageInfo
from pdefc.sources import InMemorySource


class TestCompiler(unittest.TestCase):
    def setUp(self):
        self.sources = Mock()
        self.compiler = Compiler(self.sources)

    def test_compile(self):
        module0 = 'message Message {}'
        module1 = 'interface Interface {}'
        self._add_source('test', {'hello.world': module0, 'goodbye.world': module1})

        package = self.compiler.compile('test.package')
        assert len(package.modules) == 2
        assert package.modules[0].name == 'test.hello.world'
        assert package.modules[1].name == 'test.goodbye.world'
        self.sources.read_path.assert_called_with('test.package')

    def test_compile__errors(self):
        module = 'here goes some garbage;'
        self._add_source('test', {'module': module})

        try:
            self.compiler.compile('test.package')
            self.fail()
        except CompilerException as e:
            pass
        self.sources.read_path.assert_called_with('test.package')

    def _add_source(self, name, modules_to_sources):
        info = PackageInfo(name, modules=list(modules_to_sources.keys()))
        source = InMemorySource(info, modules_to_sources)
        self.sources.read_path = Mock(return_value=source)
        self.sources.get = Mock(return_value=source)
