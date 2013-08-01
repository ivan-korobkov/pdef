# encoding: utf-8
import mock
import unittest
from pdef import ast
from pdef.common import Type
from pdef.lang import *


class TestPdef(unittest.TestCase):
    def test(self):
        '''Should create a module from an AST node, link its imports and definitions.'''
        enum = Enum('Type', 'MESSAGE')
        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)
        module0 = Module('module0')
        module0.add_definition(enum)
        module0.add_definition(base)

        import_node = ast.Import('module0', 'Type', 'Base')
        def_node = ast.Message('Message',
                               base=ast.DefinitionRef('Base'),
                               base_type=ast.EnumValueRef(ast.DefinitionRef('Type'), 'MESSAGE'),
                               fields=[ast.Field('field', ast.Ref(Type.INT32))])
        file_node = ast.File('module1', imports=[import_node], definitions=[def_node])
        module1 = Module.from_ast(file_node)

        pdef = Package()
        pdef.add_module(module0)
        pdef.add_module(module1)

        module1.link_imports()
        module1.link_definitions()

        message = module1.definitions['Message']
        assert message.name == 'Message'
        assert message.base is base
        assert message.base_type is enum.values['MESSAGE']


class TestModule(unittest.TestCase):
    def test_add_definition(self):
        '''Should add a new definition to a module.'''
        def0 = Definition(Type.DEFINITION, 'Test')
        module = Module('test')
        module.add_definition(def0)

        assert 'Test' in module.definitions

    def test_add_definition_duplicate(self):
        '''Should prevent adding a duplicate definition to a module.'''
        def0 = Definition(Type.DEFINITION, 'Test')
        def1 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        try:
            module.add_definition(def1)
            self.fail()
        except PdefException, e:
            assert 'duplicate' in e.message

    def test_add_definition_import_clash(self):
        '''Should prevent adding a definition to a module when its name clashes with an import.'''
        def0 = Definition(Type.DEFINITION, 'Test')
        def1 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_import(def0)

        try:
            module.add_definition(def1)
            self.fail()
        except PdefException, e:
            assert 'clashes with an import' in e.message

    def test_link_imports(self):
        '''Should link all imports from an AST node in a module.'''
        def0 = Definition(Type.DEFINITION, 'def0')
        def1 = Definition(Type.DEFINITION, 'def1')
        module0 = Module('module0')
        module0.add_definition(def0)
        module0.add_definition(def1)

        node = ast.File('module1', imports=(ast.Import('module0', 'def0', 'def1'), ))
        module1 = Module.from_ast(node)

        pdef = Package()
        pdef.add_module(module0)
        pdef.add_module(module1)

        module1.link_imports()
        assert module1.imported_definitions == {'def0': def0, 'def1': def1}

    def test_lookup_value(self):
        '''Should lookup a value by its ref.'''
        module = Module('test')
        int64 = module.lookup(ast.Ref(Type.INT64))
        assert int64 is NativeTypes.INT64

    def test_lookup_list(self):
        '''Should create and link a list by its ref.'''
        module = Module('test')
        list0 = module.lookup(ast.ListRef(ast.Ref(Type.STRING)))
        assert isinstance(list0, List)
        assert list0.element is NativeTypes.STRING

    def test_lookup_set(self):
        '''Should create and link a set by its ref.'''
        module = Module('test')
        set0 = module.lookup(ast.SetRef(ast.Ref(Type.FLOAT)))
        assert isinstance(set0, Set)
        assert set0.element is NativeTypes.FLOAT

    def test_lookup_map(self):
        '''Should create and link a map by its ref.'''
        module = Module('test')
        map0 = module.lookup(ast.MapRef(ast.Ref(Type.STRING), ast.Ref(Type.INT32)))
        assert isinstance(map0, Map)
        assert map0.key is NativeTypes.STRING
        assert map0.value is NativeTypes.INT32

    def test_lookup_enum_value(self):
        '''Should look up an enum value.'''
        enum = Enum('Number')
        enum.add_value('ONE')

        module = Module('test')
        module.add_definition(enum)
        one = module.lookup(ast.EnumValueRef(ast.DefinitionRef('Number'), 'ONE'))
        assert one is enum.values['ONE']

    def test_lookup_enum_value_not_present(self):
        '''Should raise an error when an enum does not have a specified value.'''
        enum = Enum('Number')
        module = Module('test')
        module.add_definition(enum)

        try:
            module.lookup(ast.EnumValueRef(ast.DefinitionRef('Number'), 'ONE'))
            self.fail()
        except PdefException, e:
            assert 'not found' in e.message

    def test_lookup_user_defined(self):
        '''Should look up a user-defined definition by its reference.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefinitionRef('Test')
        result = module.lookup(ref)
        assert def0 is result

    def test_lookup_import(self):
        '''Should look up an imported definition.'''
        def0 = Definition(Type.DEFINITION, 'def0')
        module0 = Module('module0')
        module0.add_import(def0)

        ref = ast.DefinitionRef('def0')
        result = module0.lookup(ref)
        assert def0 is result

    def test_lookup_and_link(self):
        '''Should look up a definition by its reference and link the definition.'''
        def0 = Definition(Type.DEFINITION, 'Test')

        module = Module('test')
        module.add_definition(def0)

        ref = ast.DefinitionRef('Test')
        result = module.lookup(ref)
        assert result._linked


class TestEnum(unittest.TestCase):
    def test_parse_node(self):
        '''Should create an enum from an AST node.'''
        node = ast.Enum('Number', values=('ONE', 'TWO', 'THREE'))
        module = mock.Mock()
        lookup = mock.Mock()

        enum = Enum.parse_node(node, module, lookup)

        assert len(enum.values) == 3
        assert 'ONE' in enum.values
        assert 'TWO' in enum.values
        assert 'THREE' in enum.values

    def test_add_value(self):
        '''Should add to enum a new value by its name.'''
        enum = Enum('Number')
        one = enum.add_value('ONE')

        assert one.is_enum_value
        assert one.name == 'ONE'
        assert one.enum is enum


class TestMessage(unittest.TestCase):
    def test_parse_node(self):
        '''Should create a message from an AST node.'''
        node = ast.Message('Msg', base=ast.DefinitionRef('Base'),
                           fields=[ast.Field('field', ast.Ref(Type.INT32))])
        module = mock.Mock()
        lookup = mock.Mock()

        msg = Message.parse_node(node, module, lookup)
        assert msg.name == node.name
        assert msg.base
        assert 'field' in msg.declared_fields

    def test_link(self):
        '''Should init and link message base and fields.'''
        base = Message('Base')

        msg = Message('Msg')
        msg.set_base(lambda: base)
        msg.create_field('field', lambda: NativeTypes.STRING)
        msg.link()

        assert msg.base is base
        assert msg.fields['field'].type is NativeTypes.STRING

    def test_link__base_with_type(self):
        '''Should link message and add to it to its base subtypes.'''
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg')
        msg.set_base(lambda: base, lambda: subtype)
        msg.link()

        assert subtype in base.subtypes
        assert msg in base.subtypes.values()

    def test_link_base__self_inheritance(self):
        '''Should prevent self-inheritance.'''
        msg = Message('Msg')
        msg.set_base(msg)
        try:
            msg.link()
            self.fail()
        except PdefException, e:
            assert 'cannot inherit itself' in e.message

    def test_link_base__circular_inheritance(self):
        '''Should prevent circular inheritance.'''
        msg0 = Message('Msg0')
        msg1 = Message('Msg1')
        msg1.set_base(msg0)
        msg0.set_base(msg1)

        try:
            msg0.link()
            self.fail()
        except PdefException, e:
            assert 'circular inheritance' in e.message

    def test_link_base__message_exception_clash(self):
        '''Should prevent message<->exception inheritance.'''
        msg = Message('Msg')
        exc = Message('Exc', is_exception=True)
        exc.set_base(msg)

        try:
            exc.link()
            self.fail()
        except PdefException, e:
            assert 'cannot inherit' in e.message

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
        assert msg.base_type is subtype
        assert subtype in base.subtypes
        assert base.subtypes[subtype] is msg

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

        assert msg0.subtypes == {type1: msg1}
        assert base.subtypes == {type0: msg0, type1: msg1}

    def test_link_base__multiple_tree_discriminators(self):
        '''Should set a message base when another discriminator is present.'''
        enum0 = Enum('Type0')
        enum1 = Enum('Type1')

        sub0 = enum0.add_value('SUB0')
        sub1 = enum1.add_value('SUB1')

        msg0 = Message('Msg0')
        msg0.create_field('type0', enum0, is_discriminator=True)

        msg1 = Message('Msg1')
        msg1.create_field('type1', enum1, is_discriminator=True)
        msg1.set_base(msg0, sub0)

        msg2 = Message('Msg2')
        msg2.set_base(msg1, sub1)
        msg2.link()
        assert msg2.discriminator is msg1.discriminator
        assert msg1.subtypes == {sub1: msg2}
        assert msg0.subtypes == {sub0: msg1}

    def test_link_base__nonpolymorphic_polymorphic(self):
        '''Should prevent inheriting a polymorphic base by a non-polymorphic message.'''
        enum = Enum('Type')
        enum.add_value('Subtype')

        base = Message('Base')
        base.create_field('type', enum, is_discriminator=True)

        msg = Message('Msg')
        msg.set_base(base)
        try:
            msg.link()
        except PdefException, e:
            assert 'non-polymorphic inheritance of a polymorphic base' in e.message

    def test_link_base__polymorphic_nonpolymorphic(self):
        '''Should prevent inheriting a non-polymorphic base by a polymorphic message.'''
        base = Message('Base')
        enum = Enum('Type')
        subtype = enum.add_value('SUBTYPE')

        msg = Message('Msg')
        msg.set_base(base, subtype)
        try:
            msg.link()
        except PdefException, e:
            assert 'polymorphic inheritance of a non-polymorphic base' in e.message

    def test_create_field(self):
        '''Should create and add a field to a message.'''
        msg = Message('Msg')
        msg.create_field('field', NativeTypes.INT32)

        field = msg.declared_fields['field']
        assert field.name == 'field'
        assert field.type == NativeTypes.INT32

    def test_create_field__duplicate(self):
        '''Should prevent duplicate message fields.'''
        msg = Message('Msg')
        msg.create_field('field', NativeTypes.INT32)
        try:
            msg.create_field('field', NativeTypes.INT32)
        except PdefException, e:
            assert 'duplicate' in e.message

    def test_create_field__set_discriminator(self):
        '''Should set a message discriminator when a field is a discriminator.'''
        enum = Enum('Type')
        msg = Message('Msg')

        field = msg.create_field('type', enum, is_discriminator=True)
        assert field.is_discriminator
        assert msg.discriminator is field

    def test_create_field__duplicate_discriminator(self):
        '''Should prevent multiple discriminators in a message'''
        enum = Enum('Type')
        msg = Message('Msg')
        msg.create_field('type0', enum, is_discriminator=True)
        try:
            msg.create_field('type1', enum, is_discriminator=True)
            self.fail()
        except PdefException, e:
            assert 'duplicate discriminator' in e.message

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

        assert msg1.fields == {'type': type_field, 'field0': field0, 'field1': field1}
        assert msg1.inherited_fields == {'type': type_field, 'field0': field0}
        assert msg0.fields == {'type': type_field, 'field0': field0}
        assert msg0.inherited_fields == {'type': type_field}

    def test_link_fields__duplicate_inherited_field(self):
        '''Should prevent duplicate fields with inherited fields.'''
        msg0 = Message('Msg0')
        msg0.create_field('field', NativeTypes.STRING)

        msg1 = Message('Msg1')
        msg1.set_base(msg0)
        msg1.create_field('field', NativeTypes.STRING)

        try:
            msg1.link()
            self.fail()
        except PdefException, e:
            assert 'duplicate field' in e.message


class TestField(unittest.TestCase):
    def test_parse_node(self):
        node = ast.Field('field', type=ast.Ref(ast.Type.STRING), is_discriminator=True)
        message = mock.Mock()
        lookup = mock.Mock()

        field = Field.parse_node(node, message, lookup)
        assert field.name == 'field'
        assert field.is_discriminator
        lookup.assert_called_with(node.type)

    def test_link(self):
        message = mock.Mock()
        field = Field('field', lambda: NativeTypes.INT32, message)
        field.link()

        assert field.type is NativeTypes.INT32

    def test_link__must_be_datatype(self):
        '''Should prevent fields which are not data types.'''
        iface = Interface('Interface')
        message = mock.Mock()
        field = Field('field', iface, message)
        try:
            field.link()
            self.fail()
        except PdefException, e:
            assert 'field must be a data type' in e.message

    def test_link__discriminator_must_be_enum(self):
        '''Should ensure discriminator field type is an enum.'''
        enum = Enum('Enum')
        message = mock.Mock()

        field0 = Field('field0', enum, message, is_discriminator=True)
        field1 = Field('field1', NativeTypes.INT32, message, is_discriminator=True)

        field0.link()
        try:
            field1.link()
            self.fail()
        except PdefException, e:
            assert 'discriminator must be an enum' in e.message

    def test_fullname(self):
        message = Message('Message')
        field = Field('field', NativeTypes.STRING, message)

        assert field.fullname == 'Message.field=string'


class TestInterface(unittest.TestCase):
    def test_parse_node(self):
        '''Should create an interface from an AST node.'''
        base_ref = ast.DefinitionRef('Base')
        node = ast.Interface('Iface', base=base_ref,
                             methods=[ast.Method('echo',
                                                 args=[ast.MethodArg('text', ast.Ref(Type.STRING))],
                             result=ast.Ref(Type.STRING))])

        module = mock.Mock()
        lookup = mock.Mock()

        iface = Interface.parse_node(node, module, lookup)
        assert iface.name == node.name
        assert 'echo' in iface.declared_methods

    def test_link(self):
        '''Should init and link interface base and declared methods.'''
        base = Interface('Base')

        iface = Interface('Iface')
        iface.set_base(lambda : base)
        iface.create_method('method', result=lambda : NativeTypes.INT32)
        iface.link()

        assert iface.base is base
        assert iface.declared_methods['method'].result is NativeTypes.INT32

    def test_link__base_self_inheritance(self):
        '''Should prevent interface self-inheritance.'''
        iface = Interface('Iface')
        iface.set_base(iface)

        try:
            iface.link()
            self.fail()
        except PdefException, e:
            assert 'self inheritance' in e.message

    def test_link__base_circular_inheritance(self):
        '''Should prevent circular interface inheritance.'''
        iface0 = Interface('Iface0')
        iface1 = Interface('Iface1')
        iface2 = Interface('Iface2')

        iface0.set_base(iface2)
        iface1.set_base(iface0)
        iface2.set_base(iface1)

        try:
            iface2.link()
            self.fail()
        except PdefException, e:
            assert 'circular' in e.message

    def test_link__base_must_be_interface(self):
        '''Should prevent interface bases which are not interfaces.'''
        iface = Interface('Iface0')
        iface.set_base(NativeTypes.INT32)
        try:
            iface.link()
            self.fail()
        except PdefException, e:
            assert 'base must be an interface' in e.message

    def test_methods(self):
        '''Should combine the inherited and declared methods.'''
        iface0 = Interface('Iface0')
        iface1 = Interface('Iface1')
        iface1.set_base(iface0)

        method0 = iface0.create_method('method0')
        method1 = iface1.create_method('method1')

        assert iface1.inherited_methods == {'method0': method0}
        assert iface1.methods == {'method0': method0, 'method1': method1}

    def test_create_method(self):
        '''Should create a new method to this interface.'''
        iface = Interface('Calc')
        method = iface.create_method('sum', NativeTypes.INT32,
                                     ('i0', NativeTypes.INT32), ('i1', NativeTypes.INT32))

        assert 'sum' in iface.declared_methods
        assert method.name == 'sum'
        assert method.result is NativeTypes.INT32
        assert 'i0' in method.args
        assert 'i1' in method.args

    def test_create_method__prevent_duplicates(self):
        '''Should prevent duplicate methods in an interface.'''
        iface = Interface('Iface')
        iface.create_method('doNothing')
        try:
            iface.create_method('doNothing')
            self.fail()
        except PdefException, e:
            assert 'duplicate' in e.message

    def test_link_methods__prevent_base_method_duplicates(self):
        iface0 = Interface('Interface0')
        iface0.create_method('method')

        iface1 = Interface('Interface1')
        iface1.set_base(iface0)
        iface1.create_method('method')

        try:
            iface1.link()
        except PdefException, e:
            assert 'duplicate base method' in e.message


class TestMethod(unittest.TestCase):
    def test_parse(self):
        node = ast.Method('name', args=[ast.MethodArg('arg0', ast.DefinitionRef('int32'))],
                          result=ast.DefinitionRef('int32'))
        iface = mock.Mock()
        lookup = mock.Mock()

        method = Method.parse_from(node, iface, lookup)
        assert method.name == 'name'
        assert method.result
        assert 'arg0' in method.args

    def test_link(self):
        iface = mock.Mock()
        method = Method('name', lambda : NativeTypes.INT32, iface)
        method.create_arg('arg', lambda : NativeTypes.INT64)
        method.link()

        assert method.result is NativeTypes.INT32
        assert method.args['arg'].type is NativeTypes.INT64

    def test_fullname(self):
        iface = Interface('Interface')

        method = Method('method', NativeTypes.INT32, iface)
        method.create_arg('i0', NativeTypes.INT32)
        method.create_arg('i1', NativeTypes.INT32)

        assert method.fullname == 'Interface.method(i0 int32, i1 int32)=>int32'


class TestMethodArg(unittest.TestCase):
    def parse_from(self):
        ref = ast.DefinitionRef('int32')
        node = ast.MethodArg('arg', ref)
        lookup = mock.Mock()

        arg = MethodArg.parse_from(node, lookup)
        assert arg.name == 'arg'
        lookup.assert_called_with(ref)

    def test_link(self):
        ref = lambda : NativeTypes.INT32
        arg = MethodArg('name', ref)
        arg.link()

        assert arg.name == 'name'
        assert arg.type is NativeTypes.INT32


class TestList(unittest.TestCase):
    def test_element_datatype(self):
        '''Should prevent list elements which are not data types.'''
        iface = Interface('Interface')
        try:
            List(iface)
        except PdefException, e:
            assert 'element must be a data type' in e.message


class TestSet(unittest.TestCase):
    def test_element_datatype(self):
        '''Should prevent set elements which are not data types.'''
        iface = Interface('Interface')
        try:
            Set(iface)
        except PdefException, e:
            assert 'element must be a data type' in e.message


class TestMap(unittest.TestCase):
    def test_key_primitive(self):
        msg = Message('Message')
        try:
            Map(msg, msg)
        except PdefException, e:
            assert 'key must be a primitive' in e.message

    def test_value_datatype(self):
        iface = Interface('Interface')
        try:
            Map(NativeTypes.STRING, iface)
        except PdefException, e:
            assert 'value must be a data type' in e.message
