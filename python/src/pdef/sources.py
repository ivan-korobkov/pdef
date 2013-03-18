# encoding: utf-8
import json
import os.path
import logging

from pdef import ast
from pdef.parser import ModuleParser
from pdef.preconditions import *

FILE_EXT = '.pdef'
PACKAGE_INFO_FILE = 'package.json'


class Source(object):
    def get(self, package_name):
        raise NotImplementedError


class DirectorySource(object):
    def __init__(self, path):
        self.path = check_not_none(path)
        self.info_path = os.path.join(path, PACKAGE_INFO_FILE)
        logging.info('Parsing %s', self.info_path)

        with open(self.info_path, 'r') as f:
            info = json.load(f)

        self.info = ast.Package(info['name'], version=info['version'])
        self.modules = []
        self.package = None

    def get(self, package_name):
        if package_name != self.info.name:
            return

        if not self.package:
            self.package = self.parse()
        return self.package

    def parse(self):
        modules = []
        for filepath in self.files:
            module = self.parse_file(filepath)
            modules.append(module)

        info = self.info
        return ast.Package(info.name, info.version, modules=modules)

    def parse_file(self, filepath):
        logging.info('Parsing %s', filepath)
        with open(filepath, 'r') as f:
            text = f.read()

        debug = logging.root.isEnabledFor(logging.DEBUG)
        parser = ModuleParser(debug)
        ast = parser.parse(text)

        if parser.errors:
            raise ValueError(parser.errors)

        return ast

    @property
    def files(self):
        for dirpath, dirnames, filenames in os.walk(self.path):
            logging.debug('Scanning %s', dirpath)
            for filename in filenames:
                ext = os.path.splitext(filename)[1].lower()
                if ext != FILE_EXT:
                    continue

                filepath = os.path.join(dirpath, filename)
                logging.debug('Adding %s', filepath)
                yield filepath
