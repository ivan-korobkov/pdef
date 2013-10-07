# encoding: utf-8
import argparse
import logging
import sys

import pdef_compiler


def main(argv=None):
    '''Run the compiler command-line interface.'''
    # Configure logging before the commands, because the latter
    # require a functional logger.
    _configure_logging(argv)

    # Create a commands parser.
    parser = argparse.ArgumentParser(description='Pdef compiler')
    compiler = pdef_compiler.create_compiler()
    _build_argument_parser(parser, compiler)

    args = parser.parse_args(argv)
    return _execute_command(compiler, args)


def _configure_logging(argv=None):
    argv = argv or sys.argv

    level = logging.WARN
    if ('-v' in argv) or ('--verbose' in argv):
        level = logging.INFO
    elif '--debug' in argv:
        level = logging.DEBUG

    logging.basicConfig(level=level, format='%(message)s')


def _build_argument_parser(parser, compiler):
    # Create command parsers.
    subparsers = parser.add_subparsers(dest='command', title='commands',
        description='To show a command help execute "pdefc {command} -h"')
    parser.add_argument('-v', '--verbose', action='store_true', help='verbose output')
    parser.add_argument('--debug', action='store_true', help='debug output')

    # Check command.
    check = subparsers.add_parser('check', help='check pdef files')
    check.add_argument('paths', metavar='path', nargs='+',
                       help='path to pdef files and directories')
    check.set_defaults(command_func=_check)

    # Generate command.
    generate = subparsers.add_parser('generate', help='generate source code from pdef files')
    generate.add_argument('--ns', dest='namespaces', action='append', default=[],
                          help='add a language namespace, i.e. "java:pdef.module:io.pdef.java"')
    generate.add_argument('paths', metavar='path', nargs='+',
                          help='Path to pdef files and directories')
    generate.set_defaults(command_func=_generate)

    for gname, gfactory in compiler.generators.items():
        gdoc = gfactory.__doc__
        generate.add_argument('--' + gname, help='outdir for ' + gdoc)

    return parser


def _execute_command(compiler, args):
    # The command_func is set as the default in each subparser in _create_arg_parser.
    func = args.command_func
    if not func:
        raise ValueError('No command_func in %s' % args)

    try:
        func(compiler, args)
    except pdef_compiler.CompilerException, e:
        # Get rid of the traceback.
        logging.error('%s', e)


def _check(compiler, args):
    paths = args.paths
    return compiler.compile(paths)


def _generate(compiler, args):
    argd = vars(args)

    paths = args.paths
    outs = {}
    namespaces = {}

    # Parse out directories.
    for gname in compiler.generators.keys():
        out = argd.get(gname)
        if out is None:
            continue
        outs[gname] = argd[gname]

    # Parse namespaces.
    error = False
    for gname_namespace in args.namespaces:
        parts = gname_namespace.split(':')
        if len(parts) != 3:
            logging.error('Wrong namespace %r, the namespace must be specified as '
                          '"generator:pdef.module:lang.module"', gname_namespace)
            error = True

        gname, pmodule, lmodule = parts
        if gname not in compiler.generators:
            logging.error('Unknown generator in a namespace %r', gname_namespace)
            error = True

        if gname not in namespaces:
            namespaces[gname] = {}

        namespaces[gname][pmodule] = lmodule

    if error:
        sys.exit(1)

    compiler.generate(paths, outs=outs, namespaces=namespaces)
