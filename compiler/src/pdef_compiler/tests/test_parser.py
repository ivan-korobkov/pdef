# encoding: utf-8
import os.path
import unittest

from pdef_compiler.parser import create_parser
from pdef_lang import AbsoluteImport, RelativeImport, Location
from pdef_lang.references import ListReference, SetReference, MapReference


class TestParser(unittest.TestCase):
    def setUp(self):
        self.parser = create_parser()

    def test_parse_path(self):
        dirpath = self._fixture_path()
        modules, errors = self.parser.parse_path(dirpath)
        assert len(modules) == 3
        assert not errors

    # Test parsing fixtures.

    def test_parse_fixtures__polymorphic_messages(self):
        filepath = self._fixture_path('inheritance.pdef')
        modules, errors = self.parser.parse_path(filepath)

        assert not errors
        assert len(modules) == 1
        assert modules[0].name == 'pdef.test.inheritance'
        assert modules[0].path.endswith('inheritance.pdef')

    def test_parse_fixtures__messages(self):
        filepath = self._fixture_path('messages.pdef')
        modules, errors = self.parser.parse_path(filepath)

        assert not errors
        assert len(modules) == 1
        assert modules[0].name == 'pdef.test.messages'
        assert modules[0].path.endswith('messages.pdef')

    def test_parse_fixtures__interfaces(self):
        filepath = self._fixture_path('interfaces.pdef')
        modules, errors = self.parser.parse_path(filepath)

        assert not errors
        assert len(modules) == 1
        assert modules[0].name == 'pdef.test.interfaces'
        assert modules[0].path.endswith('interfaces.pdef')

    def _fixture_path(self, filename=None):
        dirpath = os.path.join(os.path.dirname(__file__), 'fixtures')
        if not filename:
            return dirpath

        return os.path.join(dirpath, filename)

    def test_parse__reuse(self):
        s0 = '''
            module syntax_errors;

            message interface enum {
                hello();
            }
        '''

        s1 = '''
            module correct;

            message Message {}
        '''

        module, errors = self.parser.parse_string(s0)
        assert module is None
        assert errors

        module, errors = self.parser.parse_string(s1)
        assert module.name == 'correct'
        assert len(module.definitions) == 1
        assert not errors

    # Test lexer.

    def test_illegal_character(self):
        s = u'''module hello.world;

        // Привет
        message ЭMessage {}

        enum ЙEnum {}
        '''
        module, errors = self.parser.parse_string(s)
        assert not module
        assert errors[0].errors[0] == u"Illegal character 'Э', line 3"
        assert errors[0].errors[1] == u"Illegal character 'Й', line 5"

    # Test syntax parser.

    def test_syntax_error(self):
        s = '''module hello.world;

        message Message {
            wrong field definition;
        }
        '''
        module, errors = self.parser.parse_string(s)
        assert not module
        assert "Syntax error at 'definition', line 4" in errors[0].errors

    def test_syntax_error__end_of_file(self):
        s = '''module hello.world;

        message Message {
        '''
        module, errors = self.parser.parse_string(s)
        assert not module
        assert 'Unexpected end of file' in errors[0].errors

    def test_module(self):
        s = '''
            module hello.world;
            import another_module;

            enum Enum {}
            message Message {}
            interface Interface {}
        '''

        module, _ = self.parser.parse_string(s)

        assert module.name == 'hello.world'
        assert len(module.imports) == 1
        assert len(module.definitions) == 3

    def test_imports(self):
        s = '''
            module hello.world;

            import module0;
            import package0.module1;
            from package1 import module2, module3;
            from package1.subpackage import module4;
        '''

        module, _ = self.parser.parse_string(s)
        imports = module.imports

        assert len(imports) == 4

        import0 = imports[0]
        assert isinstance(import0, AbsoluteImport)
        assert import0.name == 'module0'
        assert import0.location == Location(4)

        import1 = imports[1]
        assert isinstance(import1, AbsoluteImport)
        assert import1.name == 'package0.module1'
        assert import1.location == Location(5)

        import2 = imports[2]
        assert isinstance(import2, RelativeImport)
        assert import2.prefix == 'package1'
        assert import2.relative_names == ('module2', 'module3')
        assert import2.location == Location(6)

        import3 = imports[3]
        assert isinstance(import3, RelativeImport)
        assert import3.prefix == 'package1.subpackage'
        assert import3.relative_names == ('module4', )
        assert import3.location == Location(7)

    def test_enum(self):
        s = '''
            module hello.world;

            enum Enum {
                ONE, TWO, THREE;
            }
        '''

        module, _ = self.parser.parse_string(s)
        enum = module.definitions[0]
        values = enum.values

        assert enum.name == 'Enum'
        assert enum.location == Location(4)
        assert len(values) == 3
        assert [v.name for v in values] == ['ONE', 'TWO', 'THREE']
        assert all(v.location == Location(5) for v in values)

    def test_message(self):
        s = '''
            module hello.world;

            @form
            message Message :
                Base(
                    Type.MESSAGE) {}
        '''

        module, _ = self.parser.parse_string(s)
        message = module.definitions[0]

        assert message.name == 'Message'
        assert message.is_form
        assert len(message.declared_fields) == 0
        assert message.location == Location(5)

        assert message._base.location == Location(6)
        assert message._base.name == 'Base'
        assert message._discriminator_value.name == 'Type.MESSAGE'
        assert message._discriminator_value.location == Location(7)

    def test_message_exception(self):
        s = '''
            module hello.world;

            exception Exception {}
        '''

        module, _ = self.parser.parse_string(s)
        message = module.definitions[0]

        assert message.name == 'Exception'
        assert message.is_exception
        assert message.location == Location(4)

    def test_fields(self):
        s = '''
            module hello.world;

            message Message {
                field0
                    Type @discriminator;

                field1 AnotherMessage;
            }
        '''

        module, _ = self.parser.parse_string(s)
        message = module.definitions[0]
        fields = message.declared_fields

        assert len(fields) == 2
        assert message.discriminator is fields[0]

        field0 = fields[0]
        assert field0.name == 'field0'
        assert field0._type.name == 'Type'
        assert field0.is_discriminator
        assert field0.location == Location(5)

        field1 = fields[1]
        assert field1.name == 'field1'
        assert field1._type.name == 'AnotherMessage'
        assert field1.is_discriminator is False
        assert field1.location == Location(8)

    def test_interface(self):
        s = '''
            module hello.world;

            @throws(Exception)
            interface Interface {}
        '''

        module, _ = self.parser.parse_string(s)
        interface = module.definitions[0]

        assert interface.name == 'Interface'
        assert interface.location == Location(5)
        assert interface._exc.name == 'Exception'
        assert interface._exc.location == Location(4)

    def test_methods(self):
        s = '''
            module hello.world;

            interface Interface {
                @index
                @post
                method0() void;

                method1(
                    arg0 type0,
                    arg1 type1) result;
            }
        '''

        module, _ = self.parser.parse_string(s)
        interface = module.definitions[0]
        methods = interface.methods

        assert len(methods) == 2

        method0 = methods[0]
        assert method0.name == 'method0'
        assert method0.is_post
        assert method0.is_index
        assert method0.location == Location(7)
        assert method0._result.name == 'void'
        assert method0._result.location == Location(7)
        assert method0.args == []

        method1 = methods[1]
        assert method1.name == 'method1'
        assert method1.is_post is False
        assert method1.is_index is False
        assert method1.location == Location(9)
        assert method1._result.name == 'result'
        assert method1._result.location == Location(11)

        assert len(method1.args) == 2
        assert method1.args[0].name == 'arg0'
        assert method1.args[0]._type.name == 'type0'
        assert method1.args[0]._type.location == Location(10)
        assert method1.args[1].name == 'arg1'
        assert method1.args[1]._type.name == 'type1'
        assert method1.args[1]._type.location == Location(11)

    def test_collections(self):
        s = '''
            module hello.world;

            message Message {
                field0  list<
                    list<
                        Element>>;
                field1  set<set<Element>>;
                field2  map<list<Key>, set<Value>>;
            }
        '''

        module, _ = self.parser.parse_string(s)
        fields = module.definitions[0].fields

        # List.
        list0 = fields[0]._type
        assert isinstance(list0, ListReference)
        assert isinstance(list0.element, ListReference)
        assert list0.element.element.name == 'Element'

        assert list0.location == Location(5)
        assert list0.element.location == Location(6)
        assert list0.element.element.location == Location(7)

        # Set.
        set0 = fields[1]._type
        assert isinstance(set0, SetReference)
        assert isinstance(set0.element, SetReference)
        assert set0.element.element.name == 'Element'

        assert set0.location == Location(8)
        assert set0.element.location == Location(8)
        assert set0.element.element.location == Location(8)

        # Map.
        map0 = fields[2]._type
        assert isinstance(map0, MapReference)
        assert isinstance(map0.key, ListReference)
        assert isinstance(map0.value, SetReference)
        assert map0.key.element.name == 'Key'
        assert map0.value.element.name == 'Value'

        assert map0.location == Location(9)
        assert map0.key.location == Location(9)
        assert map0.value.location == Location(9)
