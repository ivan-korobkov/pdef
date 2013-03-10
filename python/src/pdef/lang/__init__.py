# encoding: utf-8
import logging

from pdef.lang.datatypes import *
from pdef.lang.modules import *
from pdef.lang.types import *


class Pdef(object):
    def __init__(self):
        self.packages = SymbolTable()

    def package(self, package_name):
        return self.packages[package_name]

    def add_package(self, package):
        self.packages.add(package)
        logging.info('Added a package %s', package)
