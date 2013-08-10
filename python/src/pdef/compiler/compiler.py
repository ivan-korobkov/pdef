# encoding: utf-8
import argparse
import logging
import time

from pdef.compiler.exc import PdefCompilerException
from pdef.compiler.lang import Package
from pdef.java import JavaTranslator
from pdef.python.translator import PythonTranslator


def cli(argv=None):
    '''Run command-line interface which uses sys.argv.'''
    parser = argparse.ArgumentParser(description='Protocol definition compiler')
    parser.add_argument('--verbose', '-v', action='store_true', help='verbose output')
    parser.add_argument('--debug', action='store_true', help='debug output')
    parser.add_argument('--java', action='store', help='java output directory')
    parser.add_argument('--python', action='store', help='python output directory')

    parser.add_argument('paths', metavar='path', nargs='+',
                        help='path to pdef files and directories')
    args = parser.parse_args(argv)

    level = logging.DEBUG if args.debug else logging.INFO if args.verbose else logging.WARNING
    logging.basicConfig(level=level, format='%(message)s')

    run = lambda: compile_translate(args.paths, args.java, args.python)
    if args.debug:
        run()
    else:
        try:
            run()
        except PdefCompilerException, e:
            # Get rid of the traceback.
            logging.error('error: %s' % e)


def compile_translate(paths, java=None, python=None):
    '''Compile pdef files and translate them into other languages, return a package.'''
    package = _parse(paths)
    if java:
        _translate_to_java(package, java)
    if python:
        _translate_to_python(package, python)
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

def _translate_to_python(package, out):
    logging.info('\nTranslating to python...')
    t0 = time.time()
    translator = PythonTranslator(out)
    translator.translate(package)

    t1 = time.time()
    t = (t1 - t0) * 1000
    logging.info('Translated to python in %dms', t)


if __name__ == '__main__':
    cli()
