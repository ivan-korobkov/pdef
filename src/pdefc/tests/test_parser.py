# encoding: utf-8
import io
import os.path
import unittest

from pdefc.parser import create_parser
from pdefc.lang import AbsoluteImport, RelativeImport, Location
from pdefc.lang.references import ListReference, SetReference, MapReference


class TestParser(unittest.TestCase):
    def setUp(self):
        self.parser = create_parser()

    # Test fixtures.

    def test_parse_fixtures__polymorphic_messages(self):
        source = self._load_fixture('inheritance.pdef')
        module, errors = self.parser.parse(source, 'inheritance.pdef')

        assert not errors
        assert module.name == 'pdef.test.inheritance'
        assert module.filename == 'inheritance.pdef'

    def test_parse_fixtures__messages(self):
        source = self._load_fixture('messages.pdef')
        module, errors = self.parser.parse(source, 'messages.pdef')

        assert not errors
        assert module.name == 'pdef.test.messages'
        assert module.filename == 'messages.pdef'

    def test_parse_fixtures__interfaces(self):
        source = self._load_fixture('interfaces.pdef')
        module, errors = self.parser.parse(source, 'interfaces.pdef')

        assert not errors
        assert module.name == 'pdef.test.interfaces'
        assert module.filename == 'interfaces.pdef'

    def _load_fixture(self, filename=None):
        dirpath = os.path.join(os.path.dirname(__file__), 'fixtures')
        if not filename:
            return dirpath

        path = os.path.join(dirpath, filename)
        with io.open(path, 'r', encoding='utf-8') as f:
            return f.read()

    # Test grammar.

    def test_parse__reuse(self):
        s0 = '''module wrong;
            message interface enum {
                hello();
            }
        '''

        s1 = '''
            module correct;
            message Message {}
        '''

        module, errors = self.parser.parse(s0)
        assert module is None
        assert errors

        module, errors = self.parser.parse(s1)
        assert module.name == 'correct'
        assert len(module.definitions) == 1
        assert not errors

    # Test syntax parser.

    def test_syntax_error(self):
        s = '''module test;

        /**
         * Multi-line doc.
         */
        message Message {
            // Comment
            wrong field definition;
        }
        '''
        module, errors = self.parser.parse(s)
        assert not module
        assert 'Line 8, syntax error at "definition"' in errors[0]

    def test_syntax_error__reuse_parser(self):
        s = '''module test;

        /** Doc. */
        message Message {
            // Comment
            wrong field definition;
        }
        '''
        _, errors0 = self.parser.parse(s)
        _, errors1 = self.parser.parse(s)

        assert 'Line 6, syntax error at "definition"' in errors0[0]
        assert 'Line 6, syntax error at "definition"' in errors1[0]

    def test_syntax_error__end_of_file(self):
        s = '''/** Description. */
        module test;

        message Message {
        '''
        module, errors = self.parser.parse(s)
        assert not module
        assert 'Unexpected end of file' in errors[0]

    def test_syntax_error__reserved_word(self):
        s = '''
        module test;

        message Message {
            final string;
        }
        '''

        _, errors = self.parser.parse(s)
        assert 'Line 5, "final" is a reserved word' in errors[0]

    def test_doc(self):
        s = '''
            /** This is
            a multi-line
            doc string. */
            module test;
        '''
        module, _ = self.parser.parse(s)

        assert module.doc == 'This is\na multi-line\ndoc string.'

    def test_module(self):
        s = '''
            /** Module doc. */
            module test;
            import another_module;

            enum Enum {}
            message Message {}
            interface Interface {}
        '''
        module, _ = self.parser.parse(s)

        assert module.name == 'test'
        assert module.doc == 'Module doc.'
        assert len(module.imports) == 1
        assert len(module.definitions) == 3

    def test_imports(self):
        s = '''module test;
            import module0;
            import package0.module1;
            from package1 import module2, module3;
            from package1.subpackage import module4;
        '''

        module, _ = self.parser.parse(s)
        imports = module.imports

        assert len(imports) == 4

        import0 = imports[0]
        assert isinstance(import0, AbsoluteImport)
        assert import0.name == 'module0'
        assert import0.location == Location(2)

        import1 = imports[1]
        assert isinstance(import1, AbsoluteImport)
        assert import1.name == 'package0.module1'
        assert import1.location == Location(3)

        import2 = imports[2]
        assert isinstance(import2, RelativeImport)
        assert import2.prefix == 'package1'
        assert import2.relative_names == ('module2', 'module3')
        assert import2.location == Location(4)

        import3 = imports[3]
        assert isinstance(import3, RelativeImport)
        assert import3.prefix == 'package1.subpackage'
        assert import3.relative_names == ('module4', )
        assert import3.location == Location(5)

    def test_enum(self):
        s = '''
            module test;

            /** Doc. */
            enum Enum {
                ONE,
                TWO,
                THREE;
            }
        '''

        module, _ = self.parser.parse(s)
        enum = module.definitions[0]
        values = enum.values

        assert enum.name == 'Enum'
        assert enum.doc == 'Doc.'
        assert enum.location == Location(5)
        assert len(values) == 3
        assert [v.name for v in values] == ['ONE', 'TWO', 'THREE']
        assert [v.location.lineno for v in values] == [6, 7, 8]

    def test_message(self):
        s = '''
            module test;

            /** Message doc. */
            message Message :
                Base(
                    Type.MESSAGE) {}
        '''

        module, _ = self.parser.parse(s, 'module')
        message = module.definitions[0]

        assert message.name == 'Message'
        assert message.doc == 'Message doc.'
        assert len(message.declared_fields) == 0
        assert message.location == Location(5)

        assert message._base.location == Location(6)
        assert message._base.name == 'Base'
        assert message._discriminator_value.name == 'Type.MESSAGE'
        assert message._discriminator_value.location == Location(7)

    def test_message_exception(self):
        s = '''
            module test;

            /** Exception doc. */
            exception Exception {}
        '''

        module, _ = self.parser.parse(s)
        message = module.definitions[0]

        assert message.name == 'Exception'
        assert message.doc == 'Exception doc.'
        assert message.is_exception
        assert message.location == Location(5)

    def test_message_inheritance(self):
        s = '''
            module test;
            message Message : Base {}
        '''

        module, _ = self.parser.parse(s)
        message = module.definitions[0]

        assert message.name == 'Message'
        assert message._base.name == 'Base'
        assert message.discriminator_value is None

    def test_message_polymorphic_inheritance(self):
        s = '''
            module test;
            message Message : Base(Type.SUBTYPE) {}
        '''

        module, _ = self.parser.parse(s)
        message = module.definitions[0]

        assert message.name == 'Message'
        assert message._base.name == 'Base'
        assert message._discriminator_value.name == 'Type.SUBTYPE'

    def test_fields(self):
        s = '''
            module test;

            message Message {
                field0 Type @discriminator;
                field1 AnotherMessage;
            }
        '''

        module, _ = self.parser.parse(s)
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
        assert field1.location == Location(6)

    def test_interface(self):
        s = '''
            module test;

            /** Interface doc. */
            @throws(Exception)
            interface Interface {}
        '''

        module, _ = self.parser.parse(s)
        interface = module.definitions[0]

        assert interface.name == 'Interface'
        assert interface.doc == 'Interface doc.'
        assert interface.location == Location(6)
        assert interface._exc.name == 'Exception'
        assert interface._exc.location == Location(5)

    def test_interface_inheritance(self):
        s = '''
            module test;
            interface Interface : SuperInterface {}
        '''

        module, _ = self.parser.parse(s)
        interface = module.definitions[0]

        assert interface.name == 'Interface'
        assert interface._base.name == 'SuperInterface'

    def test_methods(self):
        s = '''
            module test;

            interface Interface {
                /** Method zero. */
                method0() void;

                /** Method one. */
                @post
                method1(
                    arg0 type0 @query,
                    arg1 type1 @post) result;
            }
        '''

        module, _ = self.parser.parse(s)
        interface = module.definitions[0]
        methods = interface.methods

        assert len(methods) == 2

        method0 = methods[0]
        assert method0.name == 'method0'
        assert method0.doc == 'Method zero.'
        assert method0.is_post is False
        assert method0.location == Location(6)
        assert method0._result.name == 'void'
        assert method0._result.location == Location(6)
        assert method0.args == []

        method1 = methods[1]
        assert method1.name == 'method1'
        assert method1.doc == 'Method one.'
        assert method1.is_post
        assert method1.location == Location(10)
        assert method1._result.name == 'result'
        assert method1._result.location == Location(12)

        assert len(method1.args) == 2
        assert method1.args[0].name == 'arg0'
        assert method1.args[0]._type.name == 'type0'
        assert method1.args[0]._type.location == Location(11)
        assert method1.args[0].is_query
        assert method1.args[1].name == 'arg1'
        assert method1.args[1]._type.name == 'type1'
        assert method1.args[1]._type.location == Location(12)
        assert method1.args[1].is_post

    def test_collections(self):
        s = '''
            module test;

            message Message {
                field0  list<
                    list<
                        Element>>;
                field1  set<set<Element>>;
                field2  map<list<Key>, set<Value>>;
            }
        '''

        module, _ = self.parser.parse(s, 'module')
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
