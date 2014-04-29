# encoding: utf-8
from __future__ import unicode_literals
import os
import shutil
import tempfile
import unittest

import pdefc
from pdefc import java, lang


class TestGenerator(unittest.TestCase):
    def setUp(self):
        self.generator = java.Generator(package_name='io.pdef')

    def package(self):
        path = os.path.join(os.path.dirname(__file__), 'test.pdef')
        return pdefc.compile(path)

    def test_generate__should_generate_java_files(self):
        dst = tempfile.mkdtemp('java-pdef-tests')

        try:
            package = self.package()
            java.generate(package, dst, jpackage_name='io.pdef')

            enum = os.path.join(dst, 'io', 'pdef', 'test', 'TestNumber.java')
            struct = os.path.join(dst, 'io', 'pdef', 'test', 'TestStruct.java')
            iface = os.path.join(dst, 'io', 'pdef', 'test', 'TestInterface.java')

            assert os.path.exists(enum)
            assert os.path.exists(struct)
            assert os.path.exists(iface)

        finally:
            shutil.rmtree(dst, ignore_errors=True)

    def test_jpackage__should_return_java_package_name(self):
        type0 = lang.Struct('Test')
        file0 = lang.File('test/package')
        file0.add_type(type0)

        name = self.generator.jpackage(type0)
        assert name == 'io.pdef.test.package'

    def test_jname__should_append_struct_suffix(self):
        struct = lang.Struct('Test')
        name = self.generator.jname(struct)

        assert name == 'TestStruct'

    def test_jname___should_append_interface_suffix(self):
        iface = lang.Interface('Test')
        name = self.generator.jname(iface)

        assert name == 'TestInterface'

    def test_jtype__list(self):
        list0 = lang.List(lang.INT32)
        s = self.generator.jtype(list0)

        assert s == 'java.util.List<Integer>'

    def test_jtype__set(self):
        set0 = lang.Set(lang.INT32)
        s = self.generator.jtype(set0)

        assert s == 'java.util.Set<Integer>'

    def test_jtype__map(self):
        map0 = lang.Map(lang.INT32, lang.BOOL)
        s = self.generator.jtype(map0)

        assert s == 'java.util.Map<Integer, Boolean>'

    def test_jtype__struct(self):
        struct = lang.Struct('Test')
        file = lang.File('test')
        file.add_type(struct)
        s = self.generator.jtype(struct)

        assert s == 'io.pdef.test.TestStruct'
