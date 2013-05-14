# encoding: utf-8
import json
from pdef.java.translator import JavaTranslator
from pdef.lang import Pdef


class Compiler(object):
    def __init__(self):
        self.pdef = Pdef()
        self._paths = []
        self._deps = []

        self._defs = None

    @property
    def defs(self):
        return self._defs

    def package(self):
        '''Generates a package from the package.json in the current directory.'''
        with open('package.json', 'rt') as f:
            text = f.read()

        info = json.loads(text)
        self.path(*info['path'])
        del info['path']

        if 'deps' in info:
            self.deps(*info['deps'])
            del info['deps']

        for key, value in info.items():
            getattr(self, key)(**value)

    def deps(self, *deps):
        self._deps += deps

    def path(self, *path):
        self._paths += path

    def java(self, out, async=True):
        translator = JavaTranslator(out, async)
        for def0 in self.defs:
            translator.write_definition(def0)
