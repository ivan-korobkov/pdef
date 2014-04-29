# encoding: utf-8
import argparse
import logging
import sys

import pdefc


def main(argv=None):
    '''Run the compiler command-line interface.'''

    argv = argv or sys.argv[1:]
    _logging(argv)

    try:
        parser = create_parser()
        args = parser.parse_args(argv)
        logging.debug('Arguments: %s', args)

        # The command_func is set in each command parser.
        func = args.command_func
        if not func:
            raise ValueError('No "command_func" in args %s' % args)

        return func(args)
    
    except pdefc.CompilerException as e:
        logging.error('%s', e)
        if e.errors:
            logging.error('%s', '\n'.join(e.errors))
        sys.exit(1)


def create_parser():
    parser = argparse.ArgumentParser(description='Pdef compiler, github.com/pdef')
    parser.add_argument('-v', '--verbose', action='store_true', help='verbose output')
    parser.add_argument('--debug', action='store_true', help='debug output')

    commands = parser.add_subparsers(dest='command', title='commands',
                                     description='Run "pdefc command -h" for a command help')
    version_command(commands)
    check_command(commands)
    gen_java_command(commands)
    gen_objc_command(commands)
    return parser


def _logging(argv):
    level = logging.WARN
    if ('-v' in argv) or ('--verbose' in argv):
        level = logging.INFO
    elif '--debug' in argv:
        level = logging.DEBUG

    logging.basicConfig(level=level, format='%(message)s')


def version(args):
    print('Pdef Compiler %s' % pdefc.__version__)


def version_command(commands):
    p = commands.add_parser('version', help='display the compiler version')
    p.set_defaults(command_func=version)


def check(args):
    path = args.path
    pdefc.compile(path)


def check_command(commands):
    p = commands.add_parser('check', help='check a package')
    p.set_defaults(command_func=check)
    p.add_argument('src', help='path to a pdef package')


def generate_java(args):
    src = args.src
    dst = args.dst
    package = args.package
    pdefc.generate_java(src, dst, jpackage_name=package)


def gen_java_command(p):
    p = p.add_parser('gen-java', help='Generate Java files')
    p.add_argument('src', help='pdef package path')
    p.add_argument('dst', help='destination directory')
    p.add_argument('--package', dest='package', help='java package, i.e. "io.pdef"')
    p.set_defaults(command_func=generate_java)


def generate_objc(args):
    src = args.src
    dst = args.dst
    prefix = args.prefix
    pdefc.generate_objc(src, dst, prefix=prefix)


def gen_objc_command(p):
    p = p.add_parser('gen-objc', help='Generate Objective-C files')
    p.add_argument('src', help='pdef package path')
    p.add_argument('dst', help='destination directory')
    p.add_argument('--prefix', help='objective-c class prefix, i.e. "NS"')
    p.set_defaults(command_func=generate_java)
