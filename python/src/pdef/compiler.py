# encoding: utf-8
import argparse
import logging
import time

from pdef.lang import Package
from pdef.java import JavaTranslator
from pdef.types import PdefException


def cli(argv=None):
    '''Run command-line interface which uses sys.argv.'''
    parser = argparse.ArgumentParser(description='Protocol definition compiler')
    parser.add_argument('--java', action='store', help='java output directory')
    parser.add_argument('--verbose', '-v', action='store_true', help='verbose output')
    parser.add_argument('--debug', action='store_true', help='debug output')
    parser.add_argument('paths', metavar='path', nargs='+',
                        help='path to pdef files and directories')
    args = parser.parse_args(argv)

    level = logging.WARN
    if args.verbose:
        level = logging.INFO

    if args.debug:
        level = logging.DEBUG

    logging.basicConfig(level=level, format='%(message)s')

    try:
        compile_translate(args.paths, args.java)
    except PdefException, e:
        logging.error('error: %s' % e)  # To get rid of a traceback


def compile_translate(paths, java=None):
    '''Compile pdef files and translate them into other languages, return a package.'''
    package = _parse(paths)
    if java:
        _translate_to_java(package, java)
    return package


def _parse(paths):
    t0 = time.time()
    package = Package()
    for path in paths:
        package.parse_path(path)

    package.link()

    t1 = time.time()
    t = (t1 - t0) * 1000
    logging.info('Parsed files in %dms', t)
    return package


def _translate_to_java(package, out):
    logging.info('\nTranslating to java...')
    t0 = time.time()
    translator = JavaTranslator(out)
    translator.translate(package)

    t1 = time.time()
    t = (t1 - t0) * 1000
    logging.info('Translated to java in %dms', t)


if __name__ == '__main__':
    cli()
