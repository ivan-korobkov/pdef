# encoding: utf-8
import argparse
import os
import shutil
import tempfile
import unittest
from mock import Mock

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
        paths = self._fixture_files()

        args = ['check']
        args += paths

        cli = Cli()
        cli.run(args)

    def test_generate(self):
        paths = self._fixture_files()
        java = self._tempdir()
        python = self._tempdir()

        args = ['generate', '--java=' + java, '--python=' + python]
        args += paths

        compiler = Compiler()
        compiler._generators = {
            'java': pdefc.generators.java.generate,
            'python': pdefc.generators.python.generate
        }

        cli = Cli()
        cli.run(args, compiler)

        assert os.path.exists(os.path.join(java, 'hello/world/Message.java'))
        assert os.path.exists(os.path.join(python, 'hello/world.py'))

    def test_generate_parse_outs(self):
        compiler = Mock()
        compiler.generators = {'java': 'generator0', 'python': 'generator1'}

        args = Mock()
        args.outs = {'java': 'generated-sources', 'python': 'src/generated',
                     'unsupported': '/dev/null'}

        cli = Cli()
        outs = cli._generate_parse_outs(args, compiler)

        assert outs == {'java': 'generated-sources', 'python': 'src/generated'}

    def test_generate_parse_namespaces(self):
        compiler = Mock()
        compiler.generators = {'java': 'generator0', 'python': 'generator1'}

        args = Mock()
        args.namespaces = ['java:pdef:io.pdef', 'java:test:tests', 'python:pdef.rpc:pdef_rpc']

        cli = Cli()
        namespaces = cli._generate_parse_namespaces(args, compiler)
        assert namespaces == {'java': {'pdef': 'io.pdef', 'test': 'tests'},
                              'python': {'pdef.rpc': 'pdef_rpc'}}

    def _tempfile(self):
        fd, path = tempfile.mkstemp('.pdef', text=True)
        self.tempfiles.append(path)
        return path

    def _tempdir(self):
        path = tempfile.mkdtemp('_pdef_test')
        self.tempdirs.append(path)
        return path

    def _fixture_files(self):
        s0 = '''
            module hello.world;
            message Message {}
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
