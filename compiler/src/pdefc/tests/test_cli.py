# encoding: utf-8
import argparse
import os
import shutil
import tempfile
import unittest
from mock import Mock
from pdefc import CompilerException

from pdefc.cli import Cli
from pdefc.compiler import Compiler
import pdefc.generators.java
import pdefc.generators.python


class TestCli(unittest.TestCase):
    def setUp(self):
        self.tempfiles = []
        self.tempdirs = []

    def tearDown(self):
        for tf in self.tempfiles:
            try:
                os.remove(tf)
            except OSError:
                pass

        for d in self.tempdirs:
            shutil.rmtree(d, ignore_errors=True)

    def test_check(self):
        args = ['check', 'test.package']
        args += ['--include=second.package']

        compiler = Mock()
        factory = Mock(return_value=compiler)

        cli = Cli()
        cli._create_compiler = factory
        cli.run(args)

        factory.assert_called_with(paths=['second.package'])
        compiler.check.assert_called_with('test.package')

    def test_generate(self):
        args = ['generate', 'test.package']
        args += ['--generator', 'python']
        args += ['--out', 'destination']
        args += ['--ns', 'test:io.test']
        args += ['--ns', 'second:io.second']
        args += ['--include', 'second.package']
        args += ['--include', 'third.package']

        compiler = Mock()
        factory = Mock(return_value=compiler)
        cli = Cli()
        cli._create_compiler = factory
        cli.run(args)

        factory.assert_called_with(paths=['second.package', 'third.package'])
        compiler.generate.assert_called_with('test.package', 'python', out='destination',
                                             namespace={'test': 'io.test', 'second': 'io.second'})

    def test_parse_namespace(self):
        cli = Cli()
        namespace = cli._parse_namespace(['pdef:io.pdef', 'test:io.tests'])
        assert namespace == {'pdef': 'io.pdef', 'test': 'io.tests'}

    def test_parse_namespace__wrong_namespace(self):
        cli = Cli()
        self.assertRaises(CompilerException, cli._parse_namespace, ['wrong:name:space'])

    def _test_generate__java(self):
        paths = self._fixture_paths()
        include_path = self._fixture_include_path()
        out = self._tempdir()

        args = ['generate', 'java']
        args += ['--include=' + include_path]
        args += ['--out=' + out]
        args += paths

        compiler = Compiler()
        compiler._generators = {
            'java': pdefc.generators.java.generate,
            }

        cli = Cli()
        cli.run(args, compiler)

        assert os.path.exists(os.path.join(out, 'hello/world/Message.java'))
        assert not os.path.exists(os.path.join(out, 'include/world'))  # No includes.

    def _tempfile(self):
        fd, path = tempfile.mkstemp('.pdef', text=True)
        self.tempfiles.append(path)
        return path

    def _tempdir(self):
        path = tempfile.mkdtemp('_pdef_test')
        self.tempdirs.append(path)
        return path

    def _fixture_paths(self):
        s0 = '''
            module hello.world;
            import include.world;

            message Message {
                field   include.world.IncludeMessage;
            }
        '''

        s1 = '''
            module goodbye.world;
            interface Interface {}
        '''

        path0 = self._tempfile()
        with open(path0, 'wt') as f:
            f.write(s0)

        path1 = self._tempfile()
        with open(path1, 'wt') as f:
            f.write(s1)

        return path0, path1

    def _fixture_include_path(self):
        s = '''
            module include.world;
            message IncludeMessage {}
        '''

        path = self._tempfile()
        with open(path, 'wt') as f:
            f.write(s)

        return path
