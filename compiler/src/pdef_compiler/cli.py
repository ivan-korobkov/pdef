# encoding: utf-8
import argparse
import logging

import pdef_compiler


def main(argv=None):
    '''Run the compiler command-line interface.'''
    compiler = pdef_compiler.create_compiler()
    arg_parser = _create_arg_parser(compiler)
    args = arg_parser.parse_args(argv)
    return _execute_command(compiler, args)


def _create_arg_parser(compiler):
    parser = argparse.ArgumentParser(description='Pdef compiler')
    parser.add_argument('-v', '--verbose', action='store_true', help='verbose output')
    parser.add_argument('-d', '--debug', action='store_true', help='debug output')

    # Create command parsers.
    subparsers = parser.add_subparsers(dest='command', title='commands',
        description='To show a command help execute "pdefc {command} -h"')

    # Check command.
    check = subparsers.add_parser('check', help='check pdef files')
    check.add_argument('paths', metavar='path', nargs='+',
                       help='path to pdef files and directories')
    check.set_defaults(command_func=_check)

    # Generate command.
    generate = subparsers.add_parser('generate', help='generate source code from pdef files')
    generate.add_argument('paths', metavar='path', nargs='+',
                          help='Path to pdef files and directories')
    generate.set_defaults(command_func=_generate)

    for gm in compiler.generator_modules:
        name = gm.get_name()
        group = generate.add_argument_group(name)
        gm.fill_cli_group(group)

    return parser


def _execute_command(compiler, args):
    level = logging.DEBUG if args.debug else logging.INFO if args.verbose else logging.WARNING
    logging.basicConfig(level=level, format='%(message)s')

    # The command_func is set as the default in each subparser in _create_arg_parser.
    func = args.command_func
    if not func:
        raise ValueError('No command_func in %s' % args)

    try:
        func(compiler, args)
    except pdef_compiler.CompilerException, e:
        if level == logging.DEBUG:
            raise
        else:
            # Get rid of the traceback.
            logging.error('%s', e)


def _check(compiler, args):
    paths = args.paths
    return compiler.compile(*paths)


def _generate(compiler, args):
    paths = args.paths
    package = compiler.compile(*paths)

    for gm in compiler.generator_modules:
        generator = gm.create_generator_from_cli_args(args)
        if generator:
            generator.generate(package)
