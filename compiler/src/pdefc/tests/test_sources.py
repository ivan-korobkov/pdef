# encoding: utf-8
import os
import tempfile
import unittest
from mock import Mock
from pdefc import CompilerException
from pdefc.lang.packages import PackageInfo
from pdefc.sources import UrlSource, FileSource, InMemorySource, Sources


class TestSources(unittest.TestCase):
    def setUp(self):
        self.tempfiles = []

    def tearDown(self):
        for path in self.tempfiles:
            os.remove(path)

    def test_add_source(self):
        info = PackageInfo('test')
        source = InMemorySource(info)
        sources = Sources()
        sources.add_source(source)

        assert sources.get('test') is source

    def test_add_source__prevent_duplicates(self):
        info = PackageInfo('test')
        source0 = InMemorySource(info)
        source1 = InMemorySource(info)
        sources = Sources()
        sources.add_source(source0)

        self.assertRaises(CompilerException, sources.add_source, source1)

    def test_add_path__file(self):
        info = PackageInfo('test')
        file0 = self._tempfile('.yaml', info.to_yaml())

        sources = Sources()
        sources.add_path(file0)

        source = sources.get('test')
        assert isinstance(source, FileSource)
        assert source.name == 'test'

    def test_add_path__url(self):
        source = Mock()
        source.name = 'test'

        sources = Sources()
        sources._create_url_source = lambda url: source
        sources.add_path('http://localhost:8080/test/api.yaml')

        assert sources.get('test') is source

    def test_get__not_found(self):
        sources = Sources()
        self.assertRaises(CompilerException, sources.get, 'absent')

    def _tempfile(self, prefix, content):
        _, file0 = tempfile.mkstemp(prefix)
        with open(file0, 'wt') as f:
            f.write(content)

        return file0


class TestFileSource(unittest.TestCase):
    def test(self):
        class TestSource(FileSource):
            def __init__(self, filename, files):
                self.files = files
                super(TestSource, self).__init__(filename)

            def _read(self, filepath):
                return self.files[filepath]

        info = PackageInfo('project_api', modules=['users', 'users.events'])
        files = {
            '../../test.yaml': info.to_yaml(),
            '../../users.pdef': 'users module',
            '../../users/events.pdef': 'events module'
        }

        source = TestSource('../../test.yaml', files)
        assert source.name == 'project_api'
        assert source.info.to_dict() == info.to_dict()
        assert source.module('users') == 'users module'
        assert source.module('users.events') == 'events module'


class TestUrlSource(unittest.TestCase):
    def test(self):
        info = PackageInfo('project_api', modules=['users', 'users.events'])
        urls = {
            'http://localhost:8080/project/api/api.yaml': info.to_yaml(),
            'http://localhost:8080/project/api/users.pdef': 'users module',
            'http://localhost:8080/project/api/users/events.pdef': 'events module'
        }

        source = UrlSource('http://localhost:8080/project/api/api.yaml')
        source._fetch = lambda url: urls[url]

        assert source.name == 'project_api'
        assert source.info.to_dict() == info.to_dict()
        assert source.module('users') == 'users module'
        assert source.module('users.events') == 'events module'

    def test_module_path(self):
        source = UrlSource('http://localhost:8080/project/api/api.yaml')
        path = source.module_path('users.internal.events')
        assert path == 'http://localhost:8080/project/api/users/internal/events.pdef'
