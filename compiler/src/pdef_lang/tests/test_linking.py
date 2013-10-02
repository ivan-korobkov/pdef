# encoding: utf-8
import unittest


class TestLookup(unittest.TestCase):
    def setUp(self):
        self.lookup = Lookup()

    def test_find__value(self):
        '''Should find a native type by its ref.'''
        module = Module('test')

        int64, errors = self.lookup.find(ast.ValueRef(Type.INT64), module)
        assert int64 is NativeTypes.INT64
        assert not errors

    def test_find__list(self):
        '''Should create and link a list by its ref.'''
        module = Module('test')
        list0, errors = self.lookup.find(ast.ListRef(ast.ValueRef(Type.STRING)), module)

        assert isinstance(list0, List)
        assert list0.element is NativeTypes.STRING
        assert not errors

    def test_find__set(self):
        '''Should create and link a set by its ref.'''
        module = Module('test')
        set0, errors = self.lookup.find(ast.SetRef(ast.ValueRef(Type.FLOAT)), module)

        assert isinstance(set0, Set)
        assert set0.element is NativeTypes.FLOAT
        assert not errors

    def test_find__map(self):
        '''Should create and link a map by its ref.'''
        module = Module('test')
        map0, errors = self.lookup.find(ast.MapRef(ast.ValueRef(Type.STRING),
                                                   ast.ValueRef(Type.INT32)),module)

        assert isinstance(map0, Map)
        assert map0.key is NativeTypes.STRING
        assert map0.value is NativeTypes.INT32
        assert not errors

    def test_find__user_definition(self):
        '''Should find up a user-defined definition by its reference.'''
        def0 = Definition(Type.REFERENCE, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefRef('Test')
        result, errors = self.lookup.find(ref, module)
        assert def0 is result
        assert not errors

    def test_find__enum_value(self):
        '''Should find an enum value by its name.'''
        enum = Enum('Number')
        one = enum.add_value('One')

        module = Module('test')
        module.add_definition(enum)

        def0, errors = self.lookup.find(ast.DefRef('Number.One'), module)
        assert def0 is one
        assert not errors

    def test_find__imported_definition(self):
        '''Should find an imported definition.'''
        def0 = Definition(Type.REFERENCE, 'Test')

        module0 = Module('test.module0')
        module0.add_definition(def0)

        module1 = Module('module1')
        module1.add_imported_module('test.module0', module0)

        ref = ast.DefRef('test.module0.Test')
        result, errors = self.lookup.find(ref, module1)
        assert result is def0
        assert not errors

    def test_find__imported_enum_value(self):
        '''Should find an imported enum value.'''
        enum = Enum('Number')
        one = enum.add_value('One')

        module0 = Module('test.module0')
        module0.add_definition(enum)

        module1 = Module('module1')
        module1.add_imported_module('module0', module0)

        ref = ast.DefRef('module0.Number.One')
        result, errors = self.lookup.find(ref, module1)
        assert result is one
        assert not errors


class TestLinker(unittest.TestCase):
    def setUp(self):
        self.linker = Linker()

    def test_link_package(self):
        '''Should link modules in a package.'''
        package = Package()
        package.add_module(Module('module'))

        self.linker.link_package(package)
        assert package.linked
        assert package.get_module('module').linked

    def test_link_package__errors(self):
        module = Module('test')
        msg = Message('Message')
        msg.add_field(Field('field', Reference(ast.DefRef('test.Reference'), module)))
        module.add_definition(msg)

        package = Package()
        package.add_module(module)

        try:
            self.linker.link_package(package)
            self.fail()
        except LinkerException as e:
            assert e.errors == ['nofile: test.Reference']

    def test_link_imports(self):
        import0 = AbsoluteImport('imported')

        module0 = Module('test')
        module0.add_import(import0)
        module1 = Module('imported')

        package = Package()
        package.add_module(module0)
        package.add_module(module1)

        self.linker._link_module_imports(module0)
        assert module0.imports_linked
        assert module0.imported_modules[0].module is module1

    def test_link_import__absolute(self):
        import0 = AbsoluteImport('test.module')

        module = Module('test.module')
        module.add_import(import0)

        package = Package()
        package.add_module(module)

        imported_modules, errors = self.linker._link_import(import0)
        assert imported_modules[0].module is module
        assert errors == []

    def test_link_import__relative(self):
        import0 = RelativeImport('test', 'module0', 'module1')

        module0 = Module('test.module0')
        module1 = Module('test.module1')
        module2 = Module('test.module2')
        module2.add_import(import0)

        package = Package()
        package.add_module(module0)
        package.add_module(module1)
        package.add_module(module2)

        imported_modules, errors = self.linker._link_import(import0)
        assert imported_modules[0].module is module0
        assert imported_modules[1].module is module1
        assert errors == []

    def test_link_def__message(self):
        '''Should init and link message base and fields.'''
        base = Message('Base')

        module = Module('test')
        module.add_definition(base)

        msg = Message('Msg')
        msg.set_base(Reference(ast.DefRef('Base'), module))
        field = msg.create_field('field', Reference(ast.ValueRef('string'), module))

        self.linker._link_def(msg)
        assert msg.base is base
        assert field.type is NativeTypes.STRING

    def test_link_message__base_with_type(self):
        '''Should link message and add to it to its base subtypes.'''
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        module = Module('test')
        module.add_definitions(enum, base)

        msg = Message('Msg')
        msg.set_base(Reference(ast.DefRef('Base'), module),
                     Reference(ast.DefRef('Type.SUBTYPE'), module))

        self.linker._link_def(msg)
        assert msg.base is base
        assert msg.discriminator_value is subtype
        assert msg in base.subtypes

    def test_link_message__base_subtype_tree(self):
        '''Should set a message base with a base type and add the message to the subtype tree.'''
        enum = Enum('Type')
        type0 = enum.add_value('Type0')
        type1 = enum.add_value('Type1')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg0 = Message('Msg0')
        msg0.set_base(base, type0)

        msg1 = Message('Msg1')
        msg1.set_base(msg0, type1)

        module = Module('test')
        module.add_definitions(enum, base, msg0, msg1)

        self.linker._link_def(msg1)
        assert msg0.subtypes == [msg1]
        assert base.subtypes == [msg0, msg1]

    def test_link_field(self):
        module = Module('test')
        message = Message('Msg')
        field = Field('field', Reference(ast.ValueRef(Type.INT32), module), message)

        self.linker._link_field(field)
        assert field.type is NativeTypes.INT32

    def test_link_interface(self):
        '''Should init and link interface base and declared methods.'''
        base = Interface('Base')

        module = Module('test')
        module.add_definition(base)

        iface = Interface('Iface')
        iface.set_base(Reference(ast.DefRef('Base'), module))
        method = iface.create_method('method', Reference(ast.ValueRef(Type.INT32), module))

        self.linker._link_def(iface)
        assert iface.base is base
        assert [method] == iface.declared_methods
        assert method.result is NativeTypes.INT32

    def test_link_interface__exc(self):
        '''Should link interface exception.'''
        exc = Message('Exception', is_exception=True)

        module = Module('test')
        module.add_definition(exc)

        iface = Interface('Interface')
        iface.exc = Reference(ast.DefRef('Exception'), module)

        self.linker._link_def(iface)
        assert iface.exc is exc

    def test_link_method(self):
        module = Module('test')

        method = Method('name', Reference(ast.ValueRef(Type.INT64), module))
        arg = method.create_arg('arg', Reference(ast.ValueRef(Type.INT32), module))

        self.linker._link_method(method)
        assert method.result is NativeTypes.INT64
        assert method.args == [arg]
        assert arg.type is NativeTypes.INT32
