# encoding: utf-8
from __future__ import unicode_literals

import os
import shutil
import tempfile
import unittest
from mock import Mock

from pdefc.compiler import Compiler
from pdefc.exc import CompilerException
from pdefc.lang.packages import PackageInfo
from pdefc.sources import InMemoryPackageSource, ModuleSource, PackageSources


class TestCompiler(unittest.TestCase):
    def setUp(self):
        self.tempdirs = []

    def tearDown(self):
        for d in self.tempdirs:
            shutil.rmtree(d, ignore_errors=True)

    def test_compile(self):
        # Given a compiler.
        compiler = Compiler()

        # Create a test source.
        path = self._create_source('package', {'module0': 'namespace test; message Message{}'})
        compiler.sources.add_path(path)

        # Compile the package.
        package = compiler.compile(path)

        assert package.name == 'package'
        assert len(package.modules) == 1
        assert package.modules[0].name == 'module0'

    def test_compile__with_dependencies(self):
        # Given a compiler.
        compiler = Compiler()

        # Create a test dependency
        path0 = self._create_source('dependency', {'module0': 'namespace test;'})
        compiler.add_paths(path0)

        # Create a test source.
        path1 = self._create_source('package', {'module1': 'namespace test;'}, ['dependency'])

        # Compile the package.
        package = compiler.compile(path1)

        assert package.name == 'package'
        assert len(package.dependencies) == 1

    def test_compile__errors(self):
        # Given a compiler.
        compiler = Compiler()

        # Create a test source with errors.
        path = self._create_source('package', {'module0': 'wrong module'})

        # Compilation should raise a CompilerException.
        self.assertRaises(CompilerException, compiler.compile, path)

    def _create_source(self, package_name, modules, dependencies=None):
        # Create a temp directory.
        dirname = tempfile.mkdtemp()
        self.tempdirs.append(dirname)

        # Create module files.
        sources = []
        for name, source in modules.items():
            filename = name + '.pdef'
            sources.append(filename)

            path = os.path.join(dirname, filename)
            with open(path, 'wt') as f:
                f.write(source)

        # Create a package yaml file.
        info = PackageInfo(package_name, sources=sources, dependencies=dependencies)
        path = os.path.join(dirname, package_name + '.yaml')
        with open(path, 'wt') as f:
            f.write(info.to_yaml())

        return path
