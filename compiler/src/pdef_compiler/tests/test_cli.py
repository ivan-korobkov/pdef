# encoding: utf-8
import os
import tempfile
import unittest
import pdef_compiler


class TestCli(unittest.TestCase):
    def setUp(self):
        self.tempfiles = []

    def tearDown(self):
        for tf in self.tempfiles:
            try:
                os.remove(tf)
            except OSError:
                pass

    def test_check(self):
        paths = self._fixture_files()

        args = ['check']
        args += paths
        pdef_compiler.main(args)

    def test_generate(self):
        paths = self._fixture_files()

        args = ['generate']
        args += paths
        pdef_compiler.main(args)

    def _tempfile(self):
        fd, path = tempfile.mkstemp('.pdef', text=True)
        self.tempfiles.append(path)
        return path

    def _fixture_files(self):
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

        return path0, path1
