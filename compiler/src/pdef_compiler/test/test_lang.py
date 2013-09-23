# encoding: utf-8
import mock
import unittest

from pdef_compiler import ast, CompilerException
from pdef_compiler.lang import *


class TestPackage(unittest.TestCase):
    def test_parse_module__node(self):
        '''Should parse a module from an AST node and add it to this package.'''
        module_node = ast.File('module', definitions=[
            ast.Enum('Enum', values=['One', 'Two'])
        ])

        package = Package()
        package.parse_module(module_node)

        assert package.find_module_or_raise('module')

    def test_link(self):
        '''Should link modules in a package.'''
        package = Package()
        package.add_module(Module('module', package))
        package.link()

        assert package.find_module_or_raise('module').linked


class TestModule(unittest.TestCase):
    def test_add_import(self):
        '''Should add a new import to a module.'''
        import0 = Import('imported', Module('imported'))
        module = Module('module')
        module.add_import(import0)

        assert module.find_import('imported')

    def test_add_definition(self):
        '''Should add a new definition to a module.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)
        assert module.find_definition('Test')

    def test_find_definition(self):
        '''Should return a definition by its name.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        assert def0 is module.find_definition('Test')

    def test_find_definition__enum_value(self):
        '''Should return an enum value by its name.'''
        enum = Enum('Number')
        one = enum.add_value('One')

        module = Module('test')
        module.add_definition(enum)

        def0 = module.find_definition('Number.One')
        assert def0 is one

    def test_find_ref__native(self):
        '''Should lookup a native type by its ref.'''
        module = Module('test')
        int64 = module.find_ref_or_raise(ast.ValueRef(Type.INT64))
        assert int64 is NativeTypes.INT64

    def test_find_ref__list(self):
        '''Should create and link a list by its ref.'''
        module = Module('test')
        list0 = module.find_ref_or_raise(ast.ListRef(ast.ValueRef(Type.STRING)))
        assert isinstance(list0, List)
        assert list0.element is NativeTypes.STRING

    def test_find_ref__set(self):
        '''Should create and link a set by its ref.'''
        module = Module('test')
        set0 = module.find_ref_or_raise(ast.SetRef(ast.ValueRef(Type.FLOAT)))
        assert isinstance(set0, Set)
        assert set0.element is NativeTypes.FLOAT

    def test_find_ref__map(self):
        '''Should create and link a map by its ref.'''
        module = Module('test')
        map0 = module.find_ref_or_raise(ast.MapRef(ast.ValueRef(Type.STRING), ast.ValueRef(Type.INT32)))
        assert isinstance(map0, Map)
        assert map0.key is NativeTypes.STRING
        assert map0.value is NativeTypes.INT32

    def test_find_ref__enum_value(self):
        '''Should look up an enum value.'''
        enum = Enum('Number')
        enum.add_value('One')

        module = Module('test')
        module.add_definition(enum)
        one = module.find_ref_or_raise(ast.EnumValueRef(ast.DefRef('Number'), 'One'))
        assert one is enum.find_value('One')

    def test_find_ref__enum_value_not_present(self):
        '''Should raise an error when an enum does not have a specified value.'''
        enum = Enum('Number')
        module = Module('test')
        module.add_definition(enum)

        try:
            module.find_ref_or_raise(ast.EnumValueRef(ast.DefRef('Number'), 'One'))
            self.fail()
        except CompilerException, e:
            assert 'not found' in e.message

    def test_find_ref__user_defined(self):
        '''Should look up a user-defined definition by its reference.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefRef('Test')
        result = module.find_ref_or_raise(ref)
        assert def0 is result

    def test_find_ref___link(self):
        '''Should look up a definition by its reference and link the definition.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefRef('Test')
        result = module.find_ref_or_raise(ref)
        assert result.linked

    def test_find_ref__imported_type(self):
        '''Should lookup an imported definition.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module0 = Module('test.module0')
        module0.add_definition(def0)

        module1 = Module('module1')
        module1.create_import('test.module0', module0)

        ref = ast.DefRef('test.module0.Test')
        result = module1.find_ref_or_raise(ref)
        assert result is def0

    def test_find_ref__imported_enum_value(self):
        '''Should lookup an imported enum value.'''
        enum = Enum('Number')
        one = enum.add_value('One')

        module0 = Module('test.module0')
        module0.add_definition(enum)

        module1 = Module('module1')
        module1.create_import('module0', module0)

        ref = ast.DefRef('module0.Number.One')
        result = module1.find_ref_or_raise(ref)
        assert result is one

    def test_link_imports(self):
        '''Should link module imports.'''
        module0 = Module('module0')

        module1 = Module('module1')
        module1.add_import(Import('module0', lambda: module0))
        module1.link_imports()

        assert module1.imports_linked
        assert module1.find_import('module0').module is module0

    def test_valid__duplicate_definition(self):
        '''Should prevent adding a duplicate definition to a module.'''
        def0 = Definition(Type.DEFINITION, 'Test')
        def1 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)
        module.add_definition(def1)
        module.link_imports()
        module.link()

        try:
            module.validate()
            self.fail()
        except CompilerException, e:
            assert 'Duplicate definition' in e.message

    def test_add_definition__import_clash(self):
        '''Should prevent adding a definition to a module when its name clashes with an import.'''
        module = Module('test')
        module.create_import('clash.name', Module('imported'))

        def0 = Definition(Type.DEFINITION, 'clash')
        module.add_definition(def0)
        module.link_imports()
        module.link()

        try:
            module.validate()
        except CompilerException, e:
            assert 'Definition clashes with an import' in e.message

    def test_has_import_circle__true(self):
        # 0 -> 1 -> 2 -> 0
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.create_import('module1', module1)
        module1.create_import('module2', module2)
        module2.create_import('module0', module0)

        assert module0._has_import_circle(module2) is True

    def test_has_import_circle__false(self):
        # 0 -> 1 -> 2
        module0 = Module('module0')
        module1 = Module('module1')
        module2 = Module('module2')

        module0.create_import('module0', module1)
        module1.create_import('module0', module2)

        assert module0._has_import_circle(module2) is False


class TestImport(unittest.TestCase):
    def test_parse_list_from_node__absolute(self):
        node = ast.AbsoluteImport('absolute.module')
        lookup = mock.Mock()
        imports = Import.parse_list_from_node(node, lookup)

        assert len(imports) == 1
        assert imports[0].name == 'absolute.module'
        lookup.assert_called_with('absolute.module')

    def test_parse_list_from_node__relative(self):
        node = ast.RelativeImport('absolute', 'module0', 'module1')
        lookup = mock.Mock()
        imports = Import.parse_list_from_node(node, lookup)

        assert len(imports) == 2
        assert imports[0].name == 'module0'
        assert imports[1].name == 'module1'
        assert lookup.call_args_list[0] == (('absolute.module0', ), {})
        assert lookup.call_args_list[1] == (('absolute.module1', ), {})


class TestDefinition(unittest.TestCase):
    def test_must_be_referenced_before__ok(self):
        def0 = Definition(Type.MESSAGE, 'def0')
        def1 = Definition(Type.MESSAGE, 'def1')

        module = Module('module')
        module.add_definitions(def0, def1)
        module.link_imports()
        module.link()

        assert def0._must_be_referenced_before(def1)

    def test_must_be_referenced_before__but_is_not(self):
        def0 = Definition(Type.MESSAGE, 'def0')
        def1 = Definition(Type.MESSAGE, 'def1')

        module = Module('module')
        module.add_definitions(def0, def1)
        module.link_imports()
        module.link()

        try:
            def1._must_be_referenced_before(def0)
            self.fail()
        except CompilerException, e:
            assert 'must be referenced before' in e.message

    def test_must_be_referenced_before__circular_import(self):
        def0 = Definition(Type.MESSAGE, 'def0')
        def1 = Definition(Type.MESSAGE, 'def1')

        module0 = Module('module0')
        module1 = Module('module1')

        module0.add_definition(def0)
        module1.add_definition(def1)

        module0.create_import('module1', module1)
        module1.create_import('module0', module0)

        module0.link_imports()
        module1.link_imports()
        module0.link()
        module1.link()

        try:
            def0._must_be_referenced_before(def1)
            self.fail()
        except CompilerException, e:
            assert 'modules circularly import each other' in e.message


class TestEnum(unittest.TestCase):
    def test_parse_node(self):
        '''Should create an enum from an AST node.'''
        node = ast.Enum('Number', values=('ONE', 'TWO', 'THREE'))
        lookup = mock.Mock()

        enum = Enum.parse_node(node, lookup)
        assert len(enum.values) == 3
        assert enum.find_value('ONE')
        assert enum.find_value('TWO')
        assert enum.find_value('THREE')

    def test_add_value(self):
        '''Should add to enum a new value by its name.'''
        enum = Enum('Number')
        one = enum.add_value('ONE')

        assert one.is_enum_value
        assert one.name == 'ONE'
        assert one.enum is enum

    def test_validate__duplicate_values(self):
        enum = Enum('Number')
        enum.add_value('ONE')
        enum.add_value('TWO')
        enum.link()

        try:
            enum.validate()
        except CompilerException, e:
            assert 'Duplicate value' in e.message


class TestMessage(unittest.TestCase):
    def test_parse_node(self):
        '''Should create a message from an AST node.'''
        node = ast.Message('Msg', base=ast.DefRef('Base'),
                           fields=[ast.Field('field', ast.ValueRef(Type.INT32))])
        lookup = mock.Mock()

        msg = Message.parse_node(node, lookup)
        assert msg.name == node.name
        assert msg.base
        assert len(msg.declared_fields) == 1
        assert msg.declared_fields[0].name == 'field'

    def test_create_field(self):
        '''Should create and add a field to a message.'''
        msg = Message('Msg')
        field = msg.create_field('field', NativeTypes.INT32)

        assert [field] == msg.declared_fields
        assert field.name == 'field'
        assert field.type == NativeTypes.INT32

    def test_create_field__set_discriminator(self):
        '''Should set a message discriminator when a field is a discriminator.'''
        enum = Enum('Type')
        msg = Message('Msg')

        field = msg.create_field('type', enum, is_discriminator=True)
        msg.link()
        assert field.is_discriminator
        assert msg.discriminator is field

    def test_inherited_fields(self):
        '''Should correctly compute message inherited fields.'''
        enum = Enum('Type')
        type0 = enum.add_value('Type0')
        type1 = enum.add_value('Type1')

        base = Message('Base')
        type_field = base.create_field('type', enum, is_discriminator=True)
        msg0 = Message('Msg0')
        field0 = msg0.create_field('field0', NativeTypes.INT32)
        msg0.set_base(base, type0)

        msg1 = Message('Msg1')
        field1 = msg1.create_field('field1', NativeTypes.STRING)
        msg1.set_base(msg0, type1)

        assert msg1.fields == [type_field, field0, field1]
        assert msg1.inherited_fields == [type_field, field0]
        assert msg0.fields == [type_field, field0]
        assert msg0.inherited_fields == [type_field]

    def test_link(self):
        '''Should init and link message base and fields.'''
        base = Message('Base')

        msg = Message('Msg')
        msg.set_base(lambda: base)
        field = msg.create_field('field', lambda: NativeTypes.STRING)
        msg.link()

        assert msg.base is base
        assert field.type is NativeTypes.STRING

    def test_link__base_with_type(self):
        '''Should link message and add to it to its base subtypes.'''
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg')
        msg.set_base(lambda: base, lambda: subtype)
        msg.link()

        assert msg in base.subtypes

    def test_link_base__add_subtypes(self):
        '''Should set a message base with a base type and add the message to the base subtypes.'''
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg')
        msg.set_base(base, subtype)
        msg.link()

        assert msg.base is base
        assert msg.discriminator_value is subtype
        assert msg in base.subtypes

    def test_link_base__subtype_tree(self):
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
        msg1.link()

        assert msg0.subtypes == [msg1]
        assert base.subtypes == [msg0, msg1]

    def test_validate_base__self_inheritance(self):
        '''Should prevent self-inheritance.'''
        msg = Message('Msg')
        msg.set_base(msg)
        msg.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'Circular inheritance' in e.message

    def test_validate_base__circular_inheritance(self):
        '''Should prevent circular inheritance.'''
        msg0 = Message('Msg0')
        msg1 = Message('Msg1')
        msg1.set_base(msg0)
        msg0.set_base(msg1)
        msg0.link()

        try:
            msg0.validate()
            self.fail()
        except CompilerException, e:
            assert 'Circular inheritance' in e.message

    def test_validate_base__message_exception_clash(self):
        '''Should prevent message<->exception inheritance.'''
        msg = Message('Msg')
        exc = Message('Exc', is_exception=True)
        exc.set_base(msg)
        exc.link()

        try:
            exc.validate()
            self.fail()
        except CompilerException, e:
            assert 'Wrong base type (message/exc)' in e.message

    def test_validate_base__no_enum_value_for_discriminator(self):
        '''Should prevent inheriting a polymorphic base by a non-polymorphic message.'''
        enum = Enum('Type')
        enum.add_value('Subtype')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg')
        msg.set_base(base)
        msg.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'Discriminator value required' in e.message

    def test_validate_base__base_does_not_have_discriminator(self):
        '''Should prevent inheriting a non-polymorphic base by a polymorphic message.'''
        base = Message('Base')
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        msg = Message('Msg')
        msg.set_base(base, subtype)
        msg.link()
        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'Cannot set a discriminator value, the base does not have a discriminator' \
                in e.message

    def test_validate_base__base_must_be_referenced_before(self):
        base = Message('Base')
        msg = Message('Message')
        msg.set_base(base)

        module = Module('module')
        module.add_definitions(msg, base)
        module.link_imports()
        module.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'must be referenced before' in e.message

    def test_validate_fields__duplicate(self):
        '''Should prevent duplicate message fields.'''
        msg = Message('Msg')
        msg.create_field('field', NativeTypes.INT32)
        msg.create_field('field', NativeTypes.INT32)
        msg.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'Duplicate field' in e.message

    def test_validate_fields__duplicate_inherited_field(self):
        '''Should prevent duplicate fields with inherited fields.'''
        msg0 = Message('Msg0')
        msg0.create_field('field', NativeTypes.STRING)

        msg1 = Message('Msg1')
        msg1.set_base(msg0)
        msg1.create_field('field', NativeTypes.STRING)
        msg1.link()

        try:
            msg1.validate()
            self.fail()
        except CompilerException, e:
            assert 'Duplicate field' in e.message

    def test_validate_fields__duplicate_discriminator(self):
        '''Should prevent multiple discriminators in a message'''
        enum = Enum('Type')
        msg = Message('Msg')
        msg.create_field('type0', enum, is_discriminator=True)
        msg.create_field('type1', enum, is_discriminator=True)
        msg.link()

        try:
            msg.validate()
            self.fail()
        except CompilerException, e:
            assert 'Multiple discriminator fields' in e.message

    def test_validate_fields__duplicate_base_discriminator(self):
        '''Should forbid multiple discriminators in messages.'''
        enum0 = Enum('Type0')
        enum1 = Enum('Type1')
        sub0 = enum0.add_value('SUB0')

        msg0 = Message('Msg0')
        msg0.create_field('type0', enum0, is_discriminator=True)

        msg1 = Message('Msg1')
        msg1.create_field('type1', enum1, is_discriminator=True)
        msg1.set_base(msg0, sub0)
        msg1.link()

        try:
            msg1.validate()
            self.fail()
        except CompilerException, e:
            assert 'Multiple discriminator fields' in e.message


class TestField(unittest.TestCase):
    def test_parse_node(self):
        node = ast.Field('field', ast.ValueRef(Type.STRING), is_discriminator=True)
        lookup = mock.Mock()

        field = Field.parse_node(node, lookup)
        assert field.name == 'field'
        assert field.is_discriminator
        lookup.assert_called_with(node.type)

    def test_link(self):
        message = mock.Mock()
        field = Field('field', lambda: NativeTypes.INT32, message)
        field.link()

        assert field.type is NativeTypes.INT32

    def test_validate__must_be_datatype(self):
        '''Should prevent fields which are not data types.'''
        iface = Interface('Interface')
        message = mock.Mock()
        field = Field('field', iface, message)
        field.link()

        try:
            field.validate()
            self.fail()
        except CompilerException, e:
            assert 'Field must be a data type' in e.message

    def test_validate__discriminator_must_be_enum(self):
        '''Should ensure discriminator field type is an enum.'''
        enum = Enum('Enum')
        message = mock.Mock()

        field0 = Field('field0', enum, message, is_discriminator=True)
        field1 = Field('field1', NativeTypes.INT32, message, is_discriminator=True)

        field0.link()
        field1.link()
        try:
            field0.validate()
            field1.validate()
            self.fail()
        except CompilerException, e:
            assert 'Discriminator field must be an enum' in e.message

    def test_fullname(self):
        message = Message('Message')
        field = Field('field', NativeTypes.STRING, message=message)

        assert field.fullname == 'Message.field'


class TestInterface(unittest.TestCase):
    def test_parse_node(self):
        '''Should create an interface from an AST node.'''
        base_ref = ast.DefRef('Base')
        exc_ref = ast.DefRef('Exc')

        node = ast.Interface('Iface', base=base_ref, exc=exc_ref,
                methods=[ast.Method('echo', args=[ast.Field('text', ast.ValueRef(Type.STRING))],
                result=ast.ValueRef(Type.STRING))])
        lookup = mock.Mock()

        iface = Interface.parse_node(node, lookup)
        assert iface.name == node.name
        assert iface.base
        assert iface.exc
        assert len(iface.declared_methods) == 1
        assert iface.declared_methods[0].name == 'echo'
        assert len(lookup.call_args_list) == 4

    def test_link(self):
        '''Should init and link interface base and declared methods.'''
        base = Interface('Base')
        iface = Interface('Iface')
        iface.set_base(lambda: base)
        method = iface.create_method('method', result=lambda : NativeTypes.INT32)
        iface.link()

        assert iface.base is base
        assert [method] == iface.declared_methods
        assert method.result is NativeTypes.INT32

    def test_link_exc(self):
        '''Should link interface exception.'''
        exc = Message('Exception', is_exception=True)
        iface = Interface('Interface')
        iface.exc = lambda: exc

        iface.link()
        assert iface.exc is exc

    def test_methods(self):
        '''Should combine the inherited and declared methods.'''
        iface0 = Interface('Iface0')
        iface1 = Interface('Iface1')
        iface1.set_base(iface0)

        method0 = iface0.create_method('method0')
        method1 = iface1.create_method('method1')

        assert iface1.inherited_methods == [method0]
        assert iface1.methods == [method0, method1]

    def test_create_method(self):
        '''Should create a new method to this interface.'''
        iface = Interface('Calc')
        method = iface.create_method('sum', NativeTypes.INT32,
                                     ('i0', NativeTypes.INT32), ('i1', NativeTypes.INT32))

        assert [method] == iface.declared_methods
        assert method.name == 'sum'
        assert method.result is NativeTypes.INT32
        assert method.args[0].name == 'i0'
        assert method.args[1].name == 'i1'

    def test_validate_base__self_inheritance(self):
        '''Should prevent interface self-inheritance.'''
        iface = Interface('Iface')
        iface.set_base(iface)
        iface.link()

        try:
            iface.validate()
            self.fail()
        except CompilerException, e:
            assert 'Circular inheritance' in e.message

    def test_validate_base__circular_inheritance(self):
        '''Should prevent circular interface inheritance.'''
        iface0 = Interface('Iface0')
        iface1 = Interface('Iface1')
        iface2 = Interface('Iface2')

        iface0.set_base(iface2)
        iface1.set_base(iface0)
        iface2.set_base(iface1)
        iface2.link()

        try:
            iface2.validate()
            self.fail()
        except CompilerException, e:
            assert 'Circular inheritance' in e.message

    def test_validate_base__must_be_interface(self):
        '''Should prevent interface bases which are not interfaces.'''
        iface = Interface('Iface0')
        iface.set_base(NativeTypes.INT32)
        iface.link()

        try:
            iface.validate()
            self.fail()
        except CompilerException, e:
            assert 'Base must be an interface' in e.message

    def test_validate_base__must_be_referenced_before(self):
        '''Base should be referenced before the interface.'''
        base = Interface('Base')
        iface = Interface('Interface')
        iface.set_base(base)

        module = Module('module')
        module.add_definitions(iface, base)
        module.link_imports()
        module.link()

        try:
            iface.validate()
            self.fail()
        except CompilerException, e:
            assert 'must be referenced before' in e.message

    def test_validate_exc__tries_to_throw_non_exception(self):
        '''Should prevent setting interface exception to a non-exception type.'''
        nonexc = Message('Message')
        iface = Interface('Interface')
        iface.exc = nonexc
        iface.link()

        try:
            iface.validate()
            self.fail()
        except CompilerException, e:
            assert 'Wrong exception' in e.message

    def test_validate_methods__duplicates(self):
        iface0 = Interface('Interface0')
        iface0.create_method('method')

        iface1 = Interface('Interface1')
        iface1.set_base(iface0)
        iface1.create_method('method')
        iface1.link()

        try:
            iface1.validate()
            self.fail()
        except CompilerException, e:
            assert 'Duplicate method' in e.message


class TestMethod(unittest.TestCase):
    def test_parse(self):
        node = ast.Method('name', args=[ast.Field('arg0', ast.DefRef('int32'))],
                          result=ast.DefRef('int32'))
        lookup = mock.Mock()

        method = Method.parse_from(node, lookup)
        assert method.name == 'name'
        assert method.result
        assert len(method.args) == 1
        assert method.args[0].name == 'arg0'

    def test_link(self):
        iface = mock.Mock()
        method = Method('name', lambda: NativeTypes.INT32, iface)
        arg = method.create_arg('arg', lambda: NativeTypes.INT64)
        method.link()

        assert method.result is NativeTypes.INT32
        assert [arg] == method.args
        assert arg.type is NativeTypes.INT64

    def test_fullname(self):
        method = Method('method', NativeTypes.INT32)
        method.create_arg('i0', NativeTypes.INT32)
        method.create_arg('i1', NativeTypes.INT32)

        iface = Interface('Interface')
        iface.add_method(method)

        assert method.fullname == 'Interface.method'

    def test_validate__post_remote(self):
        result = Interface('Interface')
        method = Method('method', result, is_post=True)
        method.link()

        try:
            method.validate()
            self.fail()
        except CompilerException, e:
            assert 'Only remote methods can be @post' in e.message

    def test_validate__form_field_clashes_with_arg(self):
        form = Message('Form', is_form=True)
        form.create_field('clash', NativeTypes.INT32)

        method = Method('method', NativeTypes.INT32)
        method.create_arg('clash', NativeTypes.INT32)
        method.create_arg('form', form)

        method.link()
        try:
            method.validate()
        except CompilerException, e:
            assert 'Form fields clash with method args' in e.message


class TestMethodArg(unittest.TestCase):
    def test_parse_from(self):
        ref = ast.DefRef('int32')
        node = ast.Field('arg', ref)
        lookup = mock.Mock()

        arg = MethodArg.parse_from(node, lookup)
        assert arg.name == 'arg'
        lookup.assert_called_with(ref)

    def test_link(self):
        ref = lambda: NativeTypes.INT32
        arg = MethodArg('name', ref)
        arg.link()

        assert arg.name == 'name'
        assert arg.type is NativeTypes.INT32


class TestList(unittest.TestCase):
    def test_element_datatype(self):
        '''Should prevent list elements which are not data types.'''
        iface = Interface('Interface')
        d = List(iface)
        d.link()

        try:
            d.validate()
        except CompilerException, e:
            assert 'List elements must be data types' in e.message


class TestSet(unittest.TestCase):
    def test_element_datatype(self):
        '''Should prevent set elements which are not data types.'''
        iface = Interface('Interface')
        d = Set(iface)
        d.link()

        try:
            d.validate()
            self.fail()
        except CompilerException, e:
            assert 'Set elements must be data types' in e.message


class TestMap(unittest.TestCase):
    def test_key_primitive(self):
        msg = Message('Message')
        d = Map(msg, msg)
        d.link()

        try:
            d.validate()
            self.fail()
        except CompilerException, e:
            assert 'Map keys must be primitives' in e.message

    def test_value_datatype(self):
        iface = Interface('Interface')
        d = Map(NativeTypes.STRING, iface)
        d.link()

        try:
            d.validate()
            self.fail()
        except CompilerException, e:
            assert 'Map values must be data types' in e.message
