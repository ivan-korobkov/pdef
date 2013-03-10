# encoding: utf-8
import json
import os
import os.path
import logging

from pdef.lang import Package
from pdef.parser import ModuleParser
from pdef.preconditions import *

FILE_EXT = '.pdef'
PACKAGE_INFO_FILE = 'package.json'


class Pdef(object):
    def __init__(self, builtin=True):
        self.sources = []
        self.packages = {}

        bpath = os.path.join(os.path.dirname(__file__), 'builtin')
        bdir = PackageDirectory.read(bpath)
        self.builtin = check_not_none(bdir.get('pdef'))

    def add_dirs(self, *paths):
        for path in paths:
            d = PackageDirectory.read(path, self.builtin)
            self.sources.append(d)

    def get(self, package_name):
        for source in self.sources:
            package = source.get(package_name)
            if package:
                return package


class PackageDirectory(object):
    @classmethod
    def read(cls, path, builtin=None):
        info_path = os.path.join(path, PACKAGE_INFO_FILE)
        logging.info('Parsing %s', info_path)

        with open(info_path, 'r') as f:
            info = json.load(f)

        package = Package(info['name'], version=info['version'], builtin_package=builtin)
        return PackageDirectory(path, package)

    def __init__(self, path, package):
        self._path = check_not_none(path)
        self._package = check_not_none(package)
        self._parsed = False

    def get(self, package_name):
        if package_name != self._package.name:
            return

        if not self._parsed:
            self.parse()
        return self._package

    def parse(self):
        for module_file in self.module_files:
            module = module_file.parse()
            self._package.add_modules(module)

        self._package.build()

    @property
    def module_files(self):
        for filepath in self.files:
            yield ModuleFile(filepath)

    @property
    def files(self):
        for dirpath, dirnames, filenames in os.walk(self._path):
            logging.debug('Scanning %s', dirpath)
            for filename in filenames:
                ext = os.path.splitext(filename)[1].lower()
                if ext != FILE_EXT:
                    continue

                filepath = os.path.join(dirpath, filename)
                logging.debug('Adding %s', filepath)
                yield filepath


class ModuleFile(object):
    def __init__(self, filepath):
        self._filepath = filepath

    def parse(self):
        logging.info('Parsing %s', self._filepath)
        with open(self._filepath, 'r') as f:
            text = f.read()

        debug = logging.root.isEnabledFor(logging.DEBUG)
        parser = ModuleParser(debug)
        ast = parser.parse(text)

        if parser.errors:
            raise ValueError(parser.errors)

        return ast
