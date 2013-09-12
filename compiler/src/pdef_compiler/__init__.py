import argparse
import logging
import time

from pdef_compiler.lang import Package
from pdef_compiler.java.translator import JavaTranslator
from pdef_compiler.python.translator import PythonTranslator
from pdef_compiler.exc import PdefCompilerException


def cli(argv=None):
    '''Run command-line interface which uses sys.argv.'''
    parser = argparse.ArgumentParser(description='Protocol definition compiler')
    parser.add_argument('--verbose', '-v', action='store_true', help='verbose output')
    parser.add_argument('--debug', action='store_true', help='debug output')
    parser.add_argument('--java', help='java output directory')
    parser.add_argument('--python', help='python output directory')
    parser.add_argument('--python-module', action='append', dest='python_modules',
                        help='python module name maps, for example pdef.tests:pdef_tests')

    parser.add_argument('paths', metavar='path', nargs='+',
                        help='path to pdef files and directories')
    args = parser.parse_args(argv)

    level = logging.DEBUG if args.debug else logging.INFO if args.verbose else logging.WARNING
    logging.basicConfig(level=level, format='%(message)s')

    run = lambda: compile_translate(args.paths, args.java, args.python, args.python_modules)
    if args.debug:
        run()
    else:
        try:
            run()
        except PdefCompilerException, e:
            # Get rid of the traceback.
            logging.error('error: %s' % e)


def compile_translate(paths, java=None, python=None, python_modules=None):
    '''Compile pdef files and translate them into other languages, return a package.'''
    package = _parse(paths)
    if java:
        _translate_to_java(package, java)
    if python:
        _translate_to_python(package, python, python_modules)
    return package


def _parse(paths):
    t0 = time.time()
    package = Package()
    for path in paths:
        package.parse_path(path)

    package.link()
    package.validate()

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


def _translate_to_python(package, out, python_modules=None):
    logging.info('\nTranslating to python...')
    t0 = time.time()

    module_name_map = {}
    if python_modules:
        for m in python_modules:
            key, value = m.split(':')
            module_name_map[key] = value

    translator = PythonTranslator(out, module_name_map)
    translator.translate(package)

    t1 = time.time()
    t = (t1 - t0) * 1000
    logging.info('Translated to python in %dms', t)


if __name__ == '__main__':
    cli()
