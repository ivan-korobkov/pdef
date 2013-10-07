# encoding: utf-8
import os
import tempfile
import unittest

import pdef_compiler


class TestCompiler(unittest.TestCase):
    def setUp(self):
        self.compiler = pdef_compiler.create_compiler()
        self.tempfiles = []

    def tearDown(self):
        for tf in self.tempfiles:
            try:
                os.remove(tf)
            except OSError:
                pass

    def test_compile(self):
        s0 = '''
            module hello.world;
            message Message {}
        '''

        s1 = '''
            module goodbye.world;
            interface Interface {}
        '''

        path0 = self._tempfile()
        with open(path0, 'wt') as f:
            f.write(s0)

        path1 = self._tempfile()
        with open(path1, 'wt') as f:
            f.write(s1)

        package = self.compiler.compile([path0, path1])
        assert len(package.modules) == 2
        assert package.modules[0].name == 'hello.world'
        assert package.modules[1].name == 'goodbye.world'

    def test_compile__errors(self):
        s = '''
            module hello.world;
            here goes some garbage;
        '''

        path = self._tempfile()
        with open(path, 'wt') as f:
            f.write(s)

        try:
            self.compiler.compile([path])
            self.fail()
        except pdef_compiler.CompilerException as e:
            pass

    def _tempfile(self):
        fd, path = tempfile.mkstemp('.pdef', text=True)
        self.tempfiles.append(path)
        return path
