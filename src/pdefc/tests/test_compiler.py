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
from pdefc.sources import InMemorySource


class TestCompiler(unittest.TestCase):
    def setUp(self):
        self.tempdirs = []

    def tearDown(self):
        for d in self.tempdirs:
            shutil.rmtree(d, ignore_errors=True)

    def test_compile(self):
        sources = Mock()
        compiler = Compiler(sources)

        module0 = 'message Message {}'
        module1 = 'interface Interface {}'
        self._add_source(sources, 'test', {'hello.world': module0, 'goodbye.world': module1})

        package = compiler.compile('test.package')
        assert len(package.modules) == 2
        assert package.modules[0].name == 'test.hello.world'
        assert package.modules[1].name == 'test.goodbye.world'
        sources.read_path.assert_called_with('test.package')

    def test_compile__errors(self):
        sources = Mock()
        compiler = Compiler(sources)

        module = 'here goes some garbage;'
        self._add_source(sources, 'test', {'module': module})

        try:
            compiler.compile('test.package')
            self.fail()
        except CompilerException as e:
            pass
        sources.read_path.assert_called_with('test.package')

    def _add_source(self, sources, name, modules_to_sources):
        info = PackageInfo(name, modules=list(modules_to_sources.keys()))
        source = InMemorySource(info, modules_to_sources)

        sources.read_path = Mock(return_value=source)
        sources.get = Mock(return_value=source)

    # Test real generation.

    def test_generate(self):
        module = '''
        message TestMessage {
            field string;
        }
        '''

        info = PackageInfo('test', modules=['test'])
        package_yaml = self._fixture_package_yaml(info, {'test': module})
        out = self._tempdir()

        generator = Mock()
        generator_factory = Mock(return_value=generator)
        compiler = Compiler()
        compiler._generators = {'test': generator_factory}
        compiler.generate(package_yaml, 'test', out=out, namespace={'key': 'value'})

        generator_factory.assert_called_with(out, namespace={'key': 'value'})
        args = generator.generate.call_args[0]
        package = args[0]
        assert package.name == 'test'
        assert len(package.modules) == 1
        assert package.modules[0].relative_name == 'test'

    def _fixture_package_yaml(self, info, modules_to_sources):
        directory = self._tempdir()

        package = os.path.join(directory, 'test.yaml')
        with open(package, 'wt') as f:
            f.write(info.to_yaml())

        for name, source in modules_to_sources.items():
            filename = os.path.join(directory, name + '.pdef')
            with open(filename, 'wt') as f:
                f.write(source)

        return package

    def _tempdir(self):
        path = tempfile.mkdtemp('_pdef_test')
        self.tempdirs.append(path)
        return path
