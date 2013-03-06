#!/usr/bin/env python
#encoding: utf-8
import argparse
import logging

from pdef import Pdef
from pdef.java import JavaPackage


class Compiler(object):
    def __init__(self, path):
        self.pdef = Pdef()
        self.pdef.add_dirs(*path)

    def compile_java(self, outdir, *package_names):
        for package_name in package_names:
            package = self.pdef.get(package_name)
            if not package:
                raise ValueError('Package "%s" is not found' % package_name)

            jpackage = JavaPackage(package)
            jpackage.write_to(outdir)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('-v', '--verbose', help='increase output verbosity', action='store_true')
    parser.add_argument('--debug', help='enable debug output', action='store_true', default=False)
    parser.add_argument('--java', help='output directory for java files')
    parser.add_argument('--path', help='specify package directories', nargs='+')
    parser.add_argument('--packages', help='package names to compile', nargs='+')
    args = parser.parse_args()

    level = logging.DEBUG if args.verbose or args.debug else logging.INFO
    logging.basicConfig(format='%(message)s', level=level)

    compiler = Compiler(args.path)
    if args.java:
        compiler.compile_java(args.java, *args.packages)
