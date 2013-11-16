# encoding: utf-8
import argparse
import logging
import sys

import pdefc
from pdefc.exc import CompilerException


def main(argv=None):
    '''Run the compiler command-line interface.'''
    cli = Cli()
    cli.run(argv)


class Cli(object):
    '''Pdef command-line interface.'''
    def _create_compiler(self, paths=None):
        return pdefc.create_compiler(paths)

    def run(self, argv=None):
        # Configure logging before the commands, because the latter
        # requires a functional logger.
        self._logging(argv)

        try:
            args = self._parse(argv)
            return self._execute(args)

        except pdefc.CompilerException, e:
            # It's an expected exception.
            # Get rid of the traceback.
            logging.error('%s', e)
            sys.exit(1)

    def _parse(self, argv):
        parser = self._create_parser()
        args = parser.parse_args(argv)

        logging.debug('Arguments: %s', args)
        return args

    def _execute(self, args):
        # The command_func is set as the default in each subparser.
        func = args.command_func
        if not func:
            raise ValueError('No command_func in %s' % args)

        return func(args)

    # Parser.

    def _create_parser(self):
        parser = argparse.ArgumentParser(description='Pdef compiler, see http://github.com/pdef')
        self._logging_args(parser)

        # Create command parsers.
        subparsers = parser.add_subparsers(dest='command', title='commands',
            description='To show a command help execute "pdefc {command} -h"')

        self._check_args(subparsers)
        self._generate_args(subparsers)
        return parser

    # Logging.

    def _logging(self, argv=None):
        argv = argv or sys.argv

        level = logging.WARN
        if ('-v' in argv) or ('--verbose' in argv):
            level = logging.INFO
        elif '--debug' in argv:
            level = logging.DEBUG

        logging.basicConfig(level=level, format='%(message)s')

    def _logging_args(self, parser):
        parser.add_argument('-v', '--verbose', action='store_true', help='verbose output')
        parser.add_argument('--debug', action='store_true', help='debug output')

    # Check.

    def _check(self, args):
        package = args.package
        paths = args.paths

        compiler = self._create_compiler(paths=paths)
        return compiler.check(package)

    def _check_args(self, subparsers):
        # Check command.
        check = subparsers.add_parser('check', help='check a package')
        check.add_argument('package', help='path to a package yaml file')
        check.add_argument('--include', dest='paths', action='append', default=[],
                            help='paths to package dependencies')
        check.set_defaults(command_func=self._check)

    # Generate.

    def _generate(self, args):
        package = args.package
        generator = args.generator
        paths = args.paths
        out = args.out
        namespace = self._parse_namespace(args.namespace)

        compiler = self._create_compiler(paths=paths)
        return compiler.generate(package, generator, out=out, namespace=namespace)

    def _generate_args(self, subparsers):
        generator_names = list(pdefc.find_generators().keys())

        generate = subparsers.add_parser('generate', help='generate source code from a package')
        generate.add_argument('package', help='path to a package yaml file')
        generate.add_argument('--generator', choices=generator_names, required=True,
                              help='available: %s' % ', '.join(generator_names))
        generate.add_argument('--out', dest='out', required=True,
                              help='directory for generated files')
        generate.add_argument('--ns', dest='namespace', action='append', default=[],
                              help='adds a namespace which maps pdef names '
                                   'to generated names, i.e. "pdef.module:io.pdef.java"')
        generate.add_argument('--include', dest='paths', action='append', default=[],
                              help='paths to package dependencies')

        generate.set_defaults(command_func=self._generate)

    def _parse_namespace(self, seq):
        if not seq:
            return {}

        result = {}
        error = False
        for item in seq:
            parts = item.split(':')
            if len(parts) != 2:
                logging.error('Wrong namespace "%s", the namespace must be specified as '
                              '"pdef.module:lang.module"', item)
                error = True
                continue

            pmodule, lmodule = parts
            result[pmodule] = lmodule

        if error:
            raise CompilerException('Wrong arguments')

        logging.debug('Namespace %s', result)
        return result
