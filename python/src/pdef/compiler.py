# encoding: utf-8
import os
import os.path
import logging
from pdef.lang import Package

from pdef.linker import FieldCompiler
from pdef.parser import Parser
from pdef.preconditions import *
from pdef.translators.java import JavaTranslator


FILE_EXT = '.pdef'
BUILTIN_FILE = os.path.join(os.path.dirname(__file__), "builtin.pdef")


class Compiler(object):
    def __init__(self, dirs, outdir, file_ext=FILE_EXT, with_builtin=True, debug=False):
        self.dirs = tuple(dirs)
        self.outdir = check_not_none(outdir)
        self.file_ext = check_not_none(file_ext)

        self.builtin = BUILTIN_FILE
        self.with_builtin = with_builtin

        self.debug = debug
        self.errors = []

    def _error(self, msg, *args):
        logging.error(msg, *args)
        self.errors.append(msg % args)

    def compile(self):
        # Scan directories for files.
        filepaths = self._scan_dirs()
        if self.errors:
            return

        # Parse and translate the builtin package.
        if self.with_builtin:
            builtin_module = self._parse_file(self.builtin)
            if self.errors:
                return

            builtin = Package('builtin')
            builtin.add_modules(builtin_module)

            package = Package('default', builtin_package=builtin)
        else:
            package = Package('default')

        modules = self._parse_files(filepaths)
        if self.errors:
            return

        package.add_modules(*modules)
        if self.errors:
            return

        package.build()
        if self.errors:
            return

        print(package)
        for module in package.modules:
            print(module)

        #logging.info("Generating java code...")
        #java = JavaTranslator(self.outdir, pool)
        #java.translate()
        return package

    def _scan_dirs(self):
        filepaths = []
        for d in self.dirs:
            filepaths += self._scan_dir(d)

        return filepaths

    def _scan_dir(self, d, file_ext=FILE_EXT):
        for dirpath, dirnames, filenames in os.walk(d):
            logging.debug('Scanning %s', dirpath)
            for filename in filenames:
                ext = os.path.splitext(filename)[1].lower()
                if ext != file_ext:
                    continue

                filepath = os.path.join(dirpath, filename)
                logging.debug('Adding %s', filepath)
                yield filepath

    def _parse_files(self, filepaths):
        pkg_nodes = []
        for filename in filepaths:
            pkg_node = self._parse_file(filename)
            if not pkg_node:
                continue

            pkg_nodes.append(pkg_node)

        return pkg_nodes

    def _parse_file(self, filepath):
        logging.info('Parsing %s', filepath)
        with open(filepath, 'r') as f:
            text = f.read()

        parser = Parser(self.debug)
        ast = parser.parse(text)

        self.errors += parser.errors
        return ast
