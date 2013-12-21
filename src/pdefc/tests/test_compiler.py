# encoding: utf-8
from __future__ import unicode_literals

import os
import shutil
import tempfile
import unittest
from mock import Mock

from pdefc.compiler import Compiler
from pdefc.exc import CompilerException
from pdefc.lang.packages import PackageInfo
from pdefc.sources import InMemoryPackageSource, ModuleSource


class TestCompiler(unittest.TestCase):
    def setUp(self):
        self.tempdirs = []

    def tearDown(self):
        for d in self.tempdirs:
            shutil.rmtree(d, ignore_errors=True)

    def test_compile(self):
        sources = Mock()
        compiler = Compiler(sources)

        module0 = 'namespace test; message Message {}'
        module1 = 'namespace test; interface Interface {}'
        self._add_source(sources, 'test', [('hello.world', module0), ('goodbye.world', module1)])

        package = compiler.compile('test/path.yaml')
        assert len(package.modules) == 2
        assert package.modules[0].name == 'hello.world'
        assert package.modules[1].name == 'goodbye.world'
        sources.add_path.assert_called_with('test/path.yaml')

    def test_compile__errors(self):
        sources = Mock()
        compiler = Compiler(sources)

        module = 'here goes some garbage;'
        self._add_source(sources, 'test', [('module.pdef', module)])

        try:
            compiler.compile('test/path.yaml')
            self.fail()
        except CompilerException:
            pass
        sources.add_path.assert_called_with('test/path.yaml')

    def _add_source(self, sources, name, files_datas):
        info = PackageInfo(name, sources=list(filename for filename, _ in files_datas))
        source = InMemoryPackageSource(info, list(ModuleSource(filename, data)
                                                  for filename, data in  files_datas))

        sources.add_path = Mock(return_value=source)
        sources.get = Mock(return_value=source)

    # Test real generation.

    def test_generate(self):
        module_data = '''
        message TestMessage {
            field string;
        }
        '''

        info = PackageInfo('test', sources=['test.pdef'])
        package_yaml = self._fixture_package_yaml(info, {'test.pdef': module_data})
        out = self._tempdir()

        generator = Mock()
        generator_factory = Mock(return_value=generator)
        compiler = Compiler()
        compiler._generators = {'test': generator_factory}
        compiler.generate(package_yaml, 'test', out=out, module_names=[('key', 'value')],
                          prefixes=[('key', 'K')])

        generator_factory.assert_called_with(out, module_names=[('key', 'value')],
                                             prefixes=[('key', 'K')])
        args = generator.generate.call_args[0]
        package = args[0]
        assert package.name == 'test'
        assert len(package.modules) == 1
        assert package.modules[0].relative_name == 'test'

    def _fixture_package_yaml(self, info, files_to_data):
        directory = self._tempdir()

        package = os.path.join(directory, 'test.yaml')
        with open(package, 'wt') as f:
            f.write(info.to_yaml())

        for name, source in files_to_data.items():
            filename = os.path.join(directory, name)
            with open(filename, 'wt') as f:
                f.write(source)

        return package

    def _tempdir(self):
        path = tempfile.mkdtemp('_pdef_test')
        self.tempdirs.append(path)
        return path
