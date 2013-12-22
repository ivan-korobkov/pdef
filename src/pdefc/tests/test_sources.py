# encoding: utf-8
from __future__ import unicode_literals
import os
import tempfile
import unittest
from mock import Mock
from pdefc import CompilerException
from pdefc.lang.packages import PackageInfo
from pdefc.sources import UrlPackageSource, FilePackageSource, InMemoryPackageSource, PackageSources, UTF8, \
    PackageSource


class TestPackageSources(unittest.TestCase):
    def setUp(self):
        self.tempfiles = []

    def tearDown(self):
        for path in self.tempfiles:
            os.remove(path)

    def test_add_source(self):
        info = PackageInfo('test')
        source = InMemoryPackageSource(info)
        sources = PackageSources()
        sources.add_source(source)

        assert sources.get('test') is source

    def test_add_path__file(self):
        source = Mock()
        source.package_name = 'test'
        _, filename = tempfile.mkstemp('pdef-tests')

        sources = PackageSources()
        sources._create_file_source = lambda filename: source
        sources.add_path(filename)
        os.remove(filename)

        assert sources.get('test') is source

    def test_add_path__url(self):
        source = Mock()
        source.package_name = 'test'

        sources = PackageSources()
        sources._create_url_source = lambda url: source
        sources.add_path('http://localhost:8080/test/api.yaml')

        assert sources.get('test') is source

    def test_get__not_found(self):
        sources = PackageSources()
        self.assertRaises(CompilerException, sources.get, 'absent')


class TestFilePackageSource(unittest.TestCase):
    def test(self):
        # Given a fixture package info and files.
        info = PackageInfo('project_api', sources=['users.pdef', 'users/events.pdef'])
        files = {
            '../../test.yaml': info.to_yaml(),
            '../../users.pdef': 'users module',
            '../../users/events.pdef': 'events module'
        }

        # Create a package source.
        source = FilePackageSource('../../test.yaml')
        source._read_file = lambda filepath: files[filepath]

        # The source should read the info and the modules.
        assert source.package_name == 'project_api'
        assert source.package_info.to_dict() == info.to_dict()

        assert source.module_sources[0].filename == 'users.pdef'
        assert source.module_sources[1].filename == 'users/events.pdef'

        assert source.module_sources[0].data == 'users module'
        assert source.module_sources[1].data == 'events module'


class TestUrlPackageSource(unittest.TestCase):
    def test_module(self):
        # Given a fixture package info and urls.
        info = PackageInfo('project_api', sources=['users.pdef', 'users/events.pdef'])
        urls = {
            'http://localhost/project/api/api.yaml': info.to_yaml(),
            'http://localhost/project/api/users.pdef': 'users module',
            'http://localhost/project/api/users/events.pdef': 'events module'
        }

        # Create a package source.
        source = UrlPackageSource('http://localhost/project/api/api.yaml')
        source._fetch_url = lambda url: urls[url]

        # The source should read the info and the modules.
        assert source.package_name == 'project_api'
        assert source.package_info.to_dict() == info.to_dict()

        assert source.module_sources[0].filename == 'users.pdef'
        assert source.module_sources[1].filename == 'users/events.pdef'

        assert source.module_sources[0].data == 'users module'
        assert source.module_sources[1].data == 'events module'

    def test_file_url(self):
        source = UrlPackageSource('http://localhost:8080/project/api/api.yaml')
        path = source._file_url('users/internal/events.pdef')
        assert path == 'http://localhost:8080/project/api/users/internal/events.pdef'

    def test_fetch_unicode(self):
        # Given a UTF-8 encoded URL source.
        class File(object):
            def read(self):
                return 'Привет, как дела?'.encode(UTF8)

        # Download the source.
        source = UrlPackageSource('http://localhost/test.yaml')
        source._download = lambda url: File()

        # The data should be decoded as UTF8
        data = source._fetch_url('http://localhost/')
        assert data == 'Привет, как дела?'
