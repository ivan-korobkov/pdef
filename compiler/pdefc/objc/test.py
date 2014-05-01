# encoding: utf-8
import os
import shutil
import tempfile
import unittest

import pdefc
from pdefc import lang, objc


class TestGenerator(unittest.TestCase):
    def setUp(self):
        self.generator = objc.Generator(prefix='PD')
    
    def package(self):
        path = os.path.join(os.path.dirname(__file__), 'test.pdef')
        return pdefc.compile(path)
    
    def test_generate__should_generate_objc_files(self):
        dst = tempfile.mkdtemp('objc-pdef-tests')
        
        try:
            package = self.package()
            objc.generate(package, dst, prefix='PD')
            
            files = ['PDTestNumber', 'PDTestStruct', 'PDTestInterface']
            for file in files:
                path = os.path.join(dst, file)
                header = '%s.h' % path
                impl = '%s.m' % path
                assert os.path.exists(header), 'Header file does not exist %s' % header
                assert os.path.exists(impl), 'Impl file does not exist %s' % impl
        
        finally:
            shutil.rmtree(dst, ignore_errors=True)
    
    def test_objc_name__should_prefix_and_suffix_struct(self):
        struct = lang.Struct('Test')
        name = self.generator.objc_name(struct)
        
        assert name == 'PDTestStruct'
    
    def test_objc_name__should_prefix_and_suffix_iface(self):
        iface = lang.Interface('Test')
        name = self.generator.objc_name(iface)
        
        assert name == 'PDTestInterface'
    
    def test_method_options__should_join_method_type_and_request(self):
        method = lang.Method('method', type=lang.MethodType.POST,
                             args=[lang.Argument('arg', lang.INT32)],
                             is_request=True)
        options = self.generator.objc_method_options(method)
        
        assert options == 'PDMethodPost|PDMethodRequest'
