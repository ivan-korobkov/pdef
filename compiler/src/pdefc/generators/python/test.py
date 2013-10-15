# encoding: utf-8
import unittest
from pdefc.ast import *
from pdefc.generators.python import *


class TestPythonGenerator(unittest.TestCase):
    def test_modules_with_children(self):
        mnames = ['service.server',
                  'service.client',
                  'service.client.submodule',
                  'service2.server',
                  'service3']

        parent_names = PythonGenerator._modules_with_children(mnames)
        assert parent_names == {'service', 'service.client', 'service2'}


class TestPythonModule(unittest.TestCase):
    def _fixture(self):
        msg = Message('Message')
        iface = Interface('Interface')
        enum = Enum('Enum')
        imported = Module('imported.module')

        module = Module('test')
        module.add_definition(msg)
        module.add_definition(iface)
        module.add_definition(enum)
        module.add_imported_module('module', imported)

        return PythonModule(module)

    def test_constructor(self):
        pymodule = self._fixture()

        assert pymodule.name == 'test'
        assert len(pymodule.imports) == 1
        assert len(pymodule.definitions) == 3

    def test_render(self):
        templates = pytemplates()
        pymodule = self._fixture()
        code = pymodule.render(templates)

        assert code

    def test_filename_dirname__directory(self):
        module = Module('module')
        pymodule = PythonModule(module)
        pymodule.is_directory = True

        assert pymodule.filename == '__init__.py'
        assert pymodule.dirname == 'module'

    def test_filename_dirname__directory_with_children(self):
        module = Module('test.service.module')
        pymodule = PythonModule(module)
        pymodule.is_directory = True

        assert pymodule.filename == '__init__.py'
        assert pymodule.dirname == os.path.join('test', 'service', 'module')

    def test_filename_dirname__file(self):
        module = Module('module')
        pymodule = PythonModule(module)

        assert pymodule.filename == 'module.py'
        assert pymodule.dirname is None

    def test_filename_dirname__file_with_children(self):
        module = Module('test.service.module')
        pymodule = PythonModule(module)

        assert pymodule.filename == 'module.py'
        assert pymodule.dirname == os.path.join('test', 'service')


class TestPythonEnum(unittest.TestCase):
    def _fixture(self):
        enum = Enum('Number')
        enum.create_value('ONE')
        enum.create_value('TWO')
        enum.create_value('THREE')

        return PythonDefinition.create(enum, None)

    def test_constructor(self):
        pyenum = self._fixture()
        assert pyenum.name == 'Number'
        assert pyenum.values == ['ONE', 'TWO', 'THREE']

    def test_render(self):
        templates = pytemplates()
        pyenum = self._fixture()
        code = pyenum.render(templates)

        assert code


class TestPythonMessage(unittest.TestCase):
    def _fixture(self):
        enum = Enum('Type')
        type0 = enum.create_value('MESSAGE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Message', base=base, discriminator_value=type0)
        msg.create_field('field', NativeType.BOOL)

        return PythonDefinition.create(msg, scope=pyreference)

    def test_constructor(self):
        pymsg = self._fixture()
        assert pymsg.name == 'Message'
        assert pymsg.is_exception is False
        assert pymsg.base.name == 'Base'
        assert pymsg.discriminator_value.name == 'Type.MESSAGE'
        assert pymsg.subtypes == []

        assert len(pymsg.declared_fields) == 1
        assert len(pymsg.inherited_fields) == 1
        assert len(pymsg.fields) == 2

    def test_render(self):
        templates = pytemplates()
        pymsg = self._fixture()
        code = pymsg.render(templates)
        assert code


class TestPythonInterface(unittest.TestCase):
    def _fixture(self):
        exc = Message('Exception', is_exception=True)

        iface = Interface('Interface', exc=exc)
        iface.create_method('method0', NativeType.INT32, ('arg', NativeType.INT32))
        iface.create_method('method1', NativeType.STRING, ('name', NativeType.STRING))

        return PythonDefinition.create(iface, scope=pyreference)

    def test_constructor(self):
        pyiface = self._fixture()
        assert pyiface.name == 'Interface'
        assert pyiface.exc.name == 'Exception'
        assert len(pyiface.declared_methods) == 2

    def test_render(self):
        templates = pytemplates()
        pyiface = self._fixture()
        code = pyiface.render(templates)
        assert code


class TestPythonImport(unittest.TestCase):
    def test(self):
        module = Module('my.test')
        imodule = ImportedModule('alias', module)

        assert pyimport(imodule) == 'my.test'

    def test_namespaces(self):
        module = Module('my.test.module')
        imodule = ImportedModule('alias', module)
        namespaces = pynamespace({'my.test': 'my_test'})

        assert pyimport(imodule, namespaces) == 'my_test.module'


class TestPythonRefeference(unittest.TestCase):
    def test_native(self):
        for ntype in NativeType.all():
            ref = pyreference(ntype)
            assert ref is NATIVE_TYPES[ntype.type]

    def test_list(self):
        list0 = List(NativeType.INT32)
        ref = pyreference(list0)

        assert ref.name == 'list'
        assert ref.descriptor == 'pdef.descriptors.list0(pdef.descriptors.int32)'

    def test_set(self):
        set0 = Set(NativeType.INT32)
        ref = pyreference(set0)

        assert ref.name == 'set'
        assert ref.descriptor == 'pdef.descriptors.set0(pdef.descriptors.int32)'

    def test_map(self):
        map0 = Map(NativeType.INT32, NativeType.INT64)
        ref = pyreference(map0)

        assert ref.name == 'dict'
        assert ref.descriptor == 'pdef.descriptors.map0(pdef.descriptors.int32, pdef.descriptors.int64)'

    def test_enum(self):
        enum = Enum('Number')
        module = Module('test')
        module.add_definition(enum)

        ref = pyreference(enum)
        assert ref.name == 'test.Number'
        assert ref.descriptor == 'test.Number.DESCRIPTOR'

    def test_enum_value(self):
        enum = Enum('Number')
        enum.create_value('ONE')
        two = enum.create_value('TWO')
        module = Module('test')
        module.add_definition(enum)

        ref = pyreference(two)
        assert ref.name == 'test.Number.TWO'
        assert ref.descriptor is None

    def test_message(self):
        def0 = Message('Message')
        module = Module('test')
        module.add_definition(def0)

        ref = pyreference(def0)
        assert ref.name == 'test.Message'
        assert ref.descriptor == 'test.Message.DESCRIPTOR'

    def test_interface(self):
        def0 = Interface('Interface')
        module = Module('test')
        module.add_definition(def0)

        ref = pyreference(def0)
        assert ref.name == 'test.Interface'
        assert ref.descriptor == 'test.Interface.DESCRIPTOR'

    def test_in_module_scope(self):
        def0 = Message('Message')
        module = Module('test')
        module.add_definition(def0)

        ref = pyreference(def0, module)
        assert ref.name == 'Message'
        assert ref.descriptor == 'Message.DESCRIPTOR'

    def test_map_module_name(self):
        def0 = Message('Message')
        module = Module('my.test.submodule')
        module.add_definition(def0)

        namespaces = pynamespace({'my.test': 'my_test'})
        ref = pyreference(def0, namespace=namespaces)
        assert ref.name == 'my_test.submodule.Message'
        assert ref.descriptor == 'my_test.submodule.Message.DESCRIPTOR'


class TestPythonDoc(unittest.TestCase):
    def test_none(self):
        assert pydoc(None) == ''

    def test_one_line(self):
        assert pydoc(' one-line ') == 'one-line'

    def test_multi_line(self):
        assert pydoc(' \n\nmulti-\nline\n\n\n ') == '\nmulti-\nline\n\n'
