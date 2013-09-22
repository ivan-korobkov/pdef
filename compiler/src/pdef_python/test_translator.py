# encoding: utf-8
import unittest
from pdef_compiler.lang import *
from pdef_python.translator import *


class TestPythonModule(unittest.TestCase):
    def create(self):
        msg = Message('Message')
        iface = Interface('Interface')
        enum = Enum('Enum')

        imported = Module('imported.module')

        module = Module('test')
        module.add_definitions(msg, iface, enum)
        module.create_import('module', imported)
        module.link_imports()
        module.link()

        return PythonModule(module)

    def test_constructor(self):
        pymodule = self.create()

        assert pymodule.name == 'test'
        assert len(pymodule.imports) == 1
        assert len(pymodule.definitions) == 3

    def test_code(self):
        translator = PythonTranslator('/dev/null')
        pymodule = self.create()
        code = pymodule.render(translator)
        assert code


class TestPythonEnum(unittest.TestCase):
    def create(self):
        enum = Enum('Number')
        enum.add_value('ONE')
        enum.add_value('TWO')
        enum.add_value('THREE')

        return pydef(enum, None)

    def test_constructor(self):
        pyenum = self.create()
        assert pyenum.name == 'Number'
        assert pyenum.values == ['ONE', 'TWO', 'THREE']

    def test_render(self):
        translator = PythonTranslator('/dev/null')
        pyenum = self.create()
        code = pyenum.render(translator)
        assert code


class TestPythonMessage(unittest.TestCase):
    def create(self):
        enum = Enum('Type')
        type0 = enum.add_value('MESSAGE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Message')
        msg.set_base(base, type0)
        msg.create_field('field', NativeTypes.BOOL)

        module = Module('test')
        module.add_definitions(enum, base, msg)
        module.link_imports()
        module.link()

        return pydef(msg, ref=lambda x: pyref(x, module))

    def test_constructor(self):
        pymsg = self.create()
        assert pymsg.name == 'Message'
        assert pymsg.is_exception is False
        assert pymsg.base.name == 'Base'
        assert pymsg.discriminator_value.name == 'Type.MESSAGE'
        assert pymsg.subtypes == []
        assert len(pymsg.declared_fields) == 1
        assert len(pymsg.inherited_fields) == 1
        assert len(pymsg.fields) == 2

    def test_render(self):
        translator = PythonTranslator('/dev/null')
        pymsg = self.create()
        code = pymsg.render(translator)
        assert code


class TestPythonInterface(unittest.TestCase):
    def create(self):
        base = Interface('Base')
        base.create_method('method0', NativeTypes.INT32, ('arg', NativeTypes.INT32))
        exc = Message('Exception', is_exception=True)

        iface = Interface('Interface', base=base, exc=exc)
        iface.create_method('method1', NativeTypes.STRING, ('name', NativeTypes.STRING))

        module = Module('test')
        module.add_definitions(base, exc, iface)
        module.link_imports()
        module.link()

        return pydef(iface, ref=lambda x: pyref(x, module))

    def test_constructor(self):
        pyiface = self.create()
        assert pyiface.name == 'Interface'
        assert pyiface.base.name == 'Base'
        assert pyiface.exc.name == 'Exception'
        assert len(pyiface.methods) == 2
        assert len(pyiface.declared_methods) == 1
        assert len(pyiface.inherited_methods) == 1

    def test_render(self):
        translator = PythonTranslator('/dev/null')
        pyiface = self.create()
        code = pyiface.render(translator)
        assert code


class TestPyImport(unittest.TestCase):
    def test(self):
        module = Module('my.test')
        import0 = Import('alias', module)
        s = pyimport(import0)
        assert s == 'my.test'

    def test_mapper(self):
        module = Module('my.test')
        import0 = Import('alias', module)

        mapper = NameMapper({'my.test': 'my_test'})
        s = pyimport(import0, mapper)
        assert s == 'my_test'


class TestPyRef(unittest.TestCase):
    def test_native(self):
        def0 = NativeTypes.INT32
        ref = pyref(def0)
        assert ref is NATIVE[Type.INT32]

    def test_list(self):
        def0 = List(NativeTypes.INT32)
        ref = pyref(def0)
        assert ref.name == 'list'
        assert ref.descriptor == 'descriptors.list0(descriptors.int32)'

    def test_set(self):
        def0 = Set(NativeTypes.INT32)
        ref = pyref(def0)
        assert ref.name == 'set'
        assert ref.descriptor == 'descriptors.set0(descriptors.int32)'

    def test_map(self):
        def0 = Map(NativeTypes.INT32, NativeTypes.INT64)
        ref = pyref(def0)
        assert ref.name == 'dict'
        assert ref.descriptor == 'descriptors.map0(descriptors.int32, descriptors.int64)'

    def test_enum_value(self):
        def0 = Enum('Number')
        def0.add_value('ONE')
        two = def0.add_value('TWO')

        module = Module('test')
        module.add_definition(def0)

        ref = pyref(two)
        assert ref.name == 'test.Number.TWO'
        assert ref.descriptor is None

    def test_enum(self):
        def0 = Enum('Number')
        module = Module('test')
        module.add_definition(def0)

        ref = pyref(def0)
        assert ref.name == 'test.Number'
        assert ref.descriptor == 'test.Number.__descriptor__'

    def test_message(self):
        def0 = Message('Message')
        module = Module('test')
        module.add_definition(def0)

        ref = pyref(def0)
        assert ref.name == 'test.Message'
        assert ref.descriptor == 'test.Message.__descriptor__'

    def test_interface(self):
        def0 = Interface('Interface')
        module = Module('test')
        module.add_definition(def0)

        ref = pyref(def0)
        assert ref.name == 'test.Interface'
        assert ref.descriptor == 'test.Interface.__descriptor__'

    def test_relative(self):
        def0 = Message('Message')
        module = Module('test')
        module.add_definition(def0)

        ref = pyref(def0, module)
        assert ref.name == 'Message'
        assert ref.descriptor == 'Message.__descriptor__'

    def test_mapper(self):
        def0 = Message('Message')
        module = Module('my.test.submodule')
        module.add_definition(def0)

        mapper = NameMapper({'my.test': 'my_test'})
        ref = pyref(def0, mapper=mapper)
        assert ref.name == 'my_test.submodule.Message'
        assert ref.descriptor == 'my_test.submodule.Message.__descriptor__'
