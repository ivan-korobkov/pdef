# encoding: utf-8
import io
import logging
import os
from pdefc import CompilerException
from pdefc.packages import PackageInfo


MODULE_EXT = '.pdef'
PACKAGE_EXT = '.package'
MODULE_SEP = '/'
UTF8 = 'utf-8'


def create_sources(paths=None):
    return Sources(paths)


class Sources(object):
    '''Package sources.'''
    def __init__(self, paths):
        self._sources = {}  # Package sources by names.

        if paths:
            map(self.add_path, paths)

    def add_source(self, source):
        '''Add a package source, raise CompilerException if a duplicate package.'''
        name = source.name
        if name in self._sources:
            raise CompilerException('Duplicate package source "%s"' % source)

        self._sources[name] = source
        logging.debug('Added a source "%s"' % source)
        return source

    def add_path(self, path):
        '''Add a source path.'''
        if os.path.isfile(path):
            self.read_file(path)
        elif os.path.isdir(path):
            self._walk_directory(path)
        else:
            raise CompilerException('Unsupported path: %s' % path)

    def read_path(self, path):
        '''Read a package source path, add and return the source.'''
        return self.read_file(path)

    def read_file(self, filename):
        '''Read a package source file, add and return the source.'''
        source = FileSource(filename)
        return self.add_source(source)

    def get(self, package_name):
        '''Return a package source by a package name.'''
        source = self._sources.get(package_name)
        if not source:
            raise CompilerException('Package not found "%s"' % package_name)

        return source

    def _walk_directory(self, dirname, package_ext=PACKAGE_EXT):
        if not os.path.exists(dirname):
            raise CompilerException('Directory does not exist: %s' % dirname)

        if not os.path.isdir(dirname):
            raise CompilerException('Not a directory: %s' % dirname)

        result = {}
        for root, dirs, files in os.walk(dirname):
            for file0 in files:
                ext = os.path.splitext(file0)[1]
                if ext.lower() != package_ext:
                    continue

                filename = os.path.join(root, file0)
                self.read_file(filename)

        return result


class Source(object):
    '''Package source.'''
    name = None     # Package name
    info = None     # Package info

    def module(self, module_name):
        '''Return a module source.'''
        raise NotImplementedError

    def module_path(self, module_name):
        '''Return a debug module path which can be used in errors, logs, etc.'''
        raise NotImplementedError

    def _module_filename(self, module_name, ext=MODULE_EXT, sep=MODULE_SEP):
        return module_name.replace('.', sep) + ext


class InMemorySource(Source):
    def __init__(self, info, modules_to_sources):
        self.name = info.name
        self.info = info
        self.modules = dict(modules_to_sources) if modules_to_sources else {}

    def module(self, module_name):
        return self.modules.get(module_name)

    def module_path(self, module_name):
        return self._module_filename(module_name)


class FileSource(Source):
    def __init__(self, filename):
        self.filename = filename
        self.dirname = os.path.dirname(filename)
        s = self._read(filename)

        try:
            self.info = PackageInfo.from_json(s)
            self.name = self.info.name
        except Exception as e:
            raise CompilerException('Failed to read package info: %s, e=%s' % (filename, e))

    def __str__(self):
        return self.name + ':' + self.filename

    def __repr__(self):
        return '<FileSource %r, file=%r at %s>' % (self.name, self.filename, hex(id(self)))

    def module(self, module_name):
        filepath = self.module_path(module_name)
        return self._read(filepath)

    def module_path(self, module_name):
        filename = self._module_filename(module_name)
        return os.path.join(self.dirname, filename)

    def _read(self, filepath):
        if not os.path.exists(filepath):
            raise CompilerException('File does not exist: %s' % filepath)

        with io.open(filepath, 'r', encoding=UTF8) as f:
            try:
                return f.read()
            except Exception as e:
                raise CompilerException('Failed to read a file: %s, e=%s' % (filepath, e))
