# encoding: utf-8
import json
from pdef.lang import Pdef
from pdef.parser import Parser
from pdef.java import JavaTranslator
from pdef.csharp import CsharpTranslator


class Compiler(object):
    def __init__(self):
        self.pdef = Pdef()
        self.parser = Parser()
        self.paths = []
        self.deps = []

        self._defs = None

    @property
    def defs(self):
        if self._defs: return self._defs

        for path in self.deps:
            file_node = self.parser.parse_file(path)
            self.pdef.add_file_node(file_node)

        defs = []
        for path in self.paths:
            file_node = self.parser.parse_file(path)
            module, file_defs = self.pdef.add_file_node(file_node)
            defs += file_defs

        self._defs = tuple(defs)
        self.pdef.link()
        return self._defs

    def add_deps(self, *deps):
        '''Add dependency paths.'''
        self.deps += deps

    def add_paths(self, *paths):
        '''Add definition paths.'''
        self.paths += paths

    def java(self, out, async=True):
        translator = JavaTranslator(out, async)
        translator.translate(self.defs)

    def csharp(self, out):
        translator = CsharpTranslator(out)
        translator.write_definitions(self.defs)

    def execute_config(self, config):
        '''Reads a configuration file and executes it.'''
        with open(config, 'rt') as f:
            text = f.read()

        info = json.loads(text)
        self.add_paths(*info['path'])
        del info['path']

        if 'deps' in info:
            self.add_deps(*info['deps'])
            del info['deps']

        for key, value in info.items():
            getattr(self, key)(**value)
