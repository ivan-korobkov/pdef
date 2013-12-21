# encoding: utf-8
import unittest
from mock import Mock
from pdefc import CompilerException

from pdefc.cli import Cli


class TestCli(unittest.TestCase):
    def test_check(self):
        args = ['check', 'test.package']
        args += ['--include=second.package']

        compiler = Mock()
        factory = Mock(return_value=compiler)

        cli = Cli()
        cli._create_compiler = factory
        cli.run(args)

        factory.assert_called_with(['second.package'])
        compiler.check.assert_called_with('test.package')

    def test_generate(self):
        args = ['generate', 'test.package']
        args += ['--generator', 'test']
        args += ['--out', 'destination']
        args += ['--module', 'test:io.test']
        args += ['--module', 'second:io.second']
        args += ['--prefix', 'test:T']
        args += ['--prefix', 'pdef:Pd']
        args += ['--include', 'second.package']
        args += ['--include', 'third.package']

        compiler = Mock()
        generator = Mock()
        factory = Mock(return_value=compiler)
        cli = Cli()
        cli._create_compiler = factory
        cli._find_generators = lambda: {'test': generator}
        cli.run(args)

        factory.assert_called_with(['second.package', 'third.package'])
        compiler.generate.assert_called_with('test.package', 'test', out='destination',
                module_names=[('test', 'io.test'), ('second', 'io.second')],
                prefixes=[('test', 'T'), ('pdef', 'Pd')])

    def test_parse_pairs(self):
        cli = Cli()
        mappings = cli._parse_pairs(['pdef:io.pdef', 'test:io.tests'])
        assert mappings == [('pdef', 'io.pdef'), ('test', 'io.tests')]

    def test_parse_pairs__wrong_pair(self):
        cli = Cli()
        self.assertRaises(CompilerException, cli._parse_pairs, ['wrong:name:space'])
