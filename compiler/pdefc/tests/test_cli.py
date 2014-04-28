# encoding: utf-8
import argparse
import unittest
from mock import Mock
from pdefc import CompilerException

from pdefc.cli import Cli, GenerateCommand, CheckCommand, VersionCommand


class TestCli(unittest.TestCase):
    def test_run(self):
        # Given a compiler and its command-line interface
        compiler = self._create_compiler()
        cli = Cli(compiler)

        # Run it with the test arguments.
        cli.run(['check', 'package.yaml'])

        # Verify the invocations.
        compiler.check.assert_called_with('package.yaml')

    def test_create_commands(self):
        # Given a compiler and its command-line interface
        compiler = self._create_compiler()
        cli = Cli(compiler)

        # Mock the generator classes.
        compiler.generator_classes = {'test': Mock()}

        # Create the commands.
        commands = cli._create_commands(compiler)

        # Check the commands.
        assert len(commands) == 3
        assert isinstance(commands[0], CheckCommand)
        assert isinstance(commands[1], GenerateCommand)
        assert isinstance(commands[2], VersionCommand)
        assert commands[1].generator_name == 'test'

    def _create_compiler(self):
        compiler = Mock()
        compiler.generator_classes = {}
        return compiler


class TestCheckCommand(unittest.TestCase):
    def test_build(self):
        # Given a parser.
        parser = argparse.ArgumentParser()
        subparsers = parser.add_subparsers(dest='command')

        # Create a command.
        command = self._create_command()
        command.build(subparsers)

        # Parse the arguments.
        argv = ['check', 'package.yaml']
        argv += ['--path', 'second.yaml']
        args = parser.parse_args(argv)

        # Check the arguments.
        assert args.package == 'package.yaml'
        assert args.paths == ['second.yaml']

    def test_execute(self):
        # Given parsed arguments.
        args = argparse.Namespace()
        args.package = 'package.yaml'
        args.paths = ['hello.yaml']

        # Create a command.
        compiler = Mock()
        command = self._create_command(compiler)

        # Execute the arguments.
        command.execute(args)

        # Check the invocations.
        compiler.add_paths.assert_called_with(*args.paths)
        compiler.check.assert_called_with(args.package)

    def _create_command(self, compiler=None):
        compiler = compiler or Mock()
        return CheckCommand(compiler)


class TestGenerateCommand(unittest.TestCase):
    def test_build(self):
        # Given a parser.
        parser = argparse.ArgumentParser()
        subparsers = parser.add_subparsers(dest='command')

        # Create a command.
        command = self._create_command()
        command.build(subparsers)

        # Parse the arguments.
        argv = self._create_argv()
        args = parser.parse_args(argv)

        # Check the results.
        assert args.package == 'package.yaml'
        assert args.out == 'dst/'
        assert args.paths == ['second.yaml', 'third.yaml']

    def test_execute(self):
        # Given parsed arguments.
        args = argparse.Namespace()
        args.package = 'package.yaml'
        args.paths = ['second.yaml']
        args.out = 'dst/'

        # Create a command.
        cli = Mock()
        cls = Mock()
        cls.create_cli = lambda: cli
        compiler = Mock()
        command = self._create_command(compiler, cls)

        # Execute the arguments.
        command.execute(args)

        # Check the invocations.
        compiler.add_paths.assert_called_with(*args.paths)
        cli.create_generator.assert_called_with(args.out, args)
        compiler.compile.assert_called_with(args.package)

    def _create_command(self, compiler=None, generator_class=None):
        compiler = compiler or Mock()
        generator_class = generator_class or Mock()
        return GenerateCommand(compiler, 'test', generator_class)

    def _create_argv(self):
        args = ['generate-test', 'package.yaml']
        args += ['--path', 'second.yaml']
        args += ['--path', 'third.yaml']
        args += ['--out', 'dst/']
        return args
