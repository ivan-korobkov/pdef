# encoding: utf-8
import argparse
import logging
import sys

import pdefc


def main(argv=None):
    '''Run the compiler command-line interface.'''
    cli = Cli()
    cli.run(argv)


class Cli(object):
    '''Pdef command-line interface.'''
    def run(self, argv=None, compiler=None):
        # Configure logging before the commands, because the latter
        # requires a functional logger.
        self._logging(argv)

        try:
            compiler = compiler or pdefc.create_compiler()
            args = self._parse(argv, compiler)
            return self._execute(args, compiler)

        except pdefc.CompilerException, e:
            # It's an expected exception.
            # Get rid of the traceback.
            logging.error('%s', e)
            sys.exit(1)

    def _parse(self, argv, compiler):
        parser = self._create_parser(compiler)
        args = parser.parse_args(argv)

        logging.debug('Arguments: %s', args)
        return args

    def _execute(self, args, compiler):
        # The command_func is set as the default in each subparser.
        func = args.command_func
        if not func:
            raise ValueError('No command_func in %s' % args)

        return func(args, compiler)

    # Parser.

    def _create_parser(self, compiler):
        parser = argparse.ArgumentParser(description='Pdef compiler')
        self._logging_args(parser)

        # Create command parsers.
        subparsers = parser.add_subparsers(dest='command', title='commands',
            description='To show a command help execute "pdefc {command} -h"')

        self._check_args(subparsers)
        self._generate_args(subparsers, compiler)
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

    def _check(self, args, compiler):
        paths = args.paths
        include_paths = args.include_paths
        return compiler.compile(paths, include_paths=include_paths)

    def _check_args(self, subparsers):
        # Check command.
        check = subparsers.add_parser('check', help='check pdef files')
        check.add_argument('--include', dest='include_paths', action='append', default=[],
                            help='path to pdef imports')
        check.add_argument('paths', metavar='path', nargs='+',
                           help='path to pdef files and directories')
        check.set_defaults(command_func=self._check)

    # Generate.

    def _generate(self, args, compiler):
        generator_name = args.generator
        paths = args.paths
        include_paths = args.include_paths
        out = args.out
        namespace = self._parse_namespace(args.namespace)

        return compiler.generate(generator_name, paths, out=out, namespace=namespace,
                                 include_paths=include_paths)

    def _generate_args(self, subparsers, compiler):
        generator_names = list(compiler.generators.keys())

        generate = subparsers.add_parser('generate', help='generate source code from pdef files')
        generate.add_argument('generator', metavar='generator', choices=generator_names,
                              help='available: %s' % ', '.join(generator_names))
        generate.add_argument('--out', dest='out', required=True,
                              help='directory for generated files')
        generate.add_argument('--ns', dest='namespace', action='append', default=[],
                              help='add a namespace which maps pdef names '
                                   'to generated names, i.e. "pdef.module:io.pdef.java"')
        generate.add_argument('--include', dest='include_paths', action='append', default=[],
                              help='path to pdef imports')
        generate.add_argument('paths', metavar='path', nargs='+',
                              help='path to pdef files and directories')

        generate.set_defaults(command_func=self._generate)

    def _parse_namespace(self, seq):
        if not seq:
            return {}

        result = {}
        error = False
        for item in seq:
            parts = item.split(':')
            if len(parts) != 2:
                logging.error('Wrong namespace %r, the namespace must be specified as '
                              '"pdef.module:lang.module"', item)
                error = True
                continue

            pmodule, lmodule = parts
            result[pmodule] = lmodule

        if error:
            sys.exit(1)

        logging.debug('Namespace %s', result)
        return result
