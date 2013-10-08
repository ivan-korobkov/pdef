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
        return compiler.compile(paths)

    def _check_args(self, subparsers):
        # Check command.
        check = subparsers.add_parser('check', help='check pdef files')
        check.add_argument('paths', metavar='path', nargs='+',
                           help='path to pdef files and directories')
        check.set_defaults(command_func=self._check)

    # Generate.

    def _generate(self, args, compiler):
        paths = args.paths
        outs = self._generate_parse_outs(args, compiler)
        namespaces = self._generate_parse_namespaces(args, compiler)

        return compiler.generate(paths, outs=outs, namespaces=namespaces)

    def _generate_args(self, subparsers, compiler):
        parser = subparsers.add_parser('generate', help='generate source code from pdef files')
        parser.add_argument('--ns', dest='namespaces', action='append', default=[],
                              help='add a language namespace, i.e. "java:pdef.module:io.pdef.java"')
        parser.add_argument('paths', metavar='path', nargs='+',
                             help='Path to pdef files and directories')

        self._generate_args_generators(parser, compiler)
        parser.set_defaults(command_func=self._generate)

    def _generate_args_generators(self, parser, compiler):
        for gname, gfactory in compiler.generators.items():
            gdoc = gfactory.__doc__
            parser.add_argument('--' + gname, help='outdir for ' + gdoc, metavar='out',
                                dest='outs', action=_dict_action(gname))

    def _generate_parse_outs(self, args, compiler):
        if not args.outs:
            return {}

        result = {}
        for name, out in args.outs.items():
            if name not in compiler.generators:
                logging.warn('Generator not found %r', name)
                continue

            result[name] = out

        logging.debug('Outdirs %s', result)
        return result

    def _generate_parse_namespaces(self, args, compiler):
        if not args.namespaces:
            return {}

        error = False
        result = {}
        for gname_namespace in args.namespaces:
            parts = gname_namespace.split(':')
            if len(parts) != 3:
                logging.error('Wrong namespace %r, the namespace must be specified as '
                              '"generator:pdef.module:lang.module"', gname_namespace)
                error = True
                continue

            gname, pmodule, lmodule = parts
            if gname not in compiler.generators:
                logging.error('Unknown generator in a namespace %r', gname_namespace)
                error = True
                continue

            if gname not in result:
                result[gname] = {}

            result[gname][pmodule] = lmodule

        if error:
            sys.exit(1)

        logging.debug('Namespaces %s', result)
        return result


def _dict_action(key):
    '''Aggregates values in a dest dict, can be used with multiple arguments.'''
    class DictAction(argparse.Action):
        def __call__(self, parser, namespace, values, option_string=None):
            if hasattr(namespace, self.dest) and getattr(namespace, self.dest) is not None:
                outs = getattr(namespace, self.dest)
            else:
                outs = {}
                setattr(namespace, self.dest, outs)

            outs[key] = values

    return DictAction
