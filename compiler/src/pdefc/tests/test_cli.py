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

        factory.assert_called_with(['second.package'], False)
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

        factory.assert_called_with(['second.package', 'third.package'], False)
        compiler.generate.assert_called_with('test.package', 'python', out='destination',
                                             namespace={'test': 'io.test', 'second': 'io.second'})

    def test_parse_namespace(self):
        cli = Cli()
        namespace = cli._parse_namespace(['pdef:io.pdef', 'test:io.tests'])
        assert namespace == {'pdef': 'io.pdef', 'test': 'io.tests'}

    def test_parse_namespace__wrong_namespace(self):
        cli = Cli()
        self.assertRaises(CompilerException, cli._parse_namespace, ['wrong:name:space'])
