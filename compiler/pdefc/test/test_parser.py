# encoding: utf-8
from __future__ import unicode_literals
import unittest

from pdefc import lang
from pdefc.parser import Parser, cleanup_docstring


class TestParser(unittest.TestCase):
    def setUp(self):
        self.parser = Parser()

    # Test grammar.

    def test_parse__reuse(self):
        s0 = '''
            package test;

            struct interface enum {
                hello();
            }
        '''

        s1 = '''
            package test;

            struct Struct {}
        '''

        file, errors = self.parser.parse(s0, 'errors')
        assert file is None
        assert errors

        file, errors = self.parser.parse(s1, 'correct')
        assert file.path == 'correct'
        assert len(file.types) == 1
        assert not errors

    # Test syntax parser.

    def test_syntax_error(self):
        s = '''/** File description. */
        package test;

        /**
         * Multi-line doc.
         */
        struct Struct {
            // Comment
            wrong field definition;
        }
        '''
        file, errors = self.parser.parse(s)
        assert not file
        assert 'Line 9: Syntax error at "definition"' in errors[0]

    def test_syntax_error__reuse_parser(self):
        s = '''/** Description. */
        package test;

        /** Doc. */
        struct Struct {
            // Comment
            wrong field definition;
        }
        '''
        _, errors0 = self.parser.parse(s, 'file')
        _, errors1 = self.parser.parse(s, 'file')

        assert 'file, line 7: Syntax error at "definition"' in errors0[0]
        assert 'file, line 7: Syntax error at "definition"' in errors1[0]

    def test_syntax_error__end_of_file(self):
        s = '''/** Description. */
        package test;

        struct Struct {
        '''
        file, errors = self.parser.parse(s, 'file')
        assert not file
        assert 'file: Unexpected end of file' in errors[0]

    def test_syntax_error__reserved_word(self):
        s = '''
        package test;

        struct Struct {
            final string;
        }
        '''

        _, errors = self.parser.parse(s, 'file')
        assert 'file, line 5: "final" is a reserved word' in errors[0]

    def test_no_syntax_error__reserved_words_should_be_case_sensitive(self):
        s = '''
        package test;
        
        struct Struct {
            FINAL string;
        }
        '''

        _, errors = self.parser.parse(s, 'file')
        assert not errors

    def test_doc(self):
        s = '''
            /** This is
             * a multi-line
             * doc string. */
            package test;
        '''
        file, _ = self.parser.parse(s, 'file')
        assert file.doc == 'This is\na multi-line\ndoc string.'

    def test_file(self):
        s = '''
            /** File doc. */
            package test;

            enum Enum {}
            struct Struct {}
            interface Interface {}
        '''
        path = 'file.pdef'
        file, _ = self.parser.parse(s, path)

        assert file.path == path
        assert file.doc == 'File doc.'
        assert len(file.types) == 3

    def test_enum(self):
        s = '''
            package test;

            /** Doc. */
            enum Enum {
                ONE,
                TWO,
                THREE;
            }
        '''

        file, _ = self.parser.parse(s, 'file')
        enum = file.types[0]
        values = enum.values

        assert enum.name == 'Enum'
        assert enum.doc == 'Doc.'
        assert enum.location.lineno == 5
        assert len(values) == 3
        assert [v.name for v in values] == ['ONE', 'TWO', 'THREE']
        assert [v.location.lineno for v in values] == [6, 7, 8]

    def test_struct(self):
        s = '''
            package test;

            /** Struct doc. */
            struct Struct {}
        '''

        file, _ = self.parser.parse(s, 'file')
        struct = file.types[0]

        assert struct.name == 'Struct'
        assert struct.doc == 'Struct doc.'
        assert len(struct.fields) == 0
        assert struct.location.lineno == 5

    def test_struct_exception(self):
        s = '''
            package test;

            /** Exception doc. */
            exception Exception {}
        '''

        file, _ = self.parser.parse(s, 'file')
        struct = file.types[0]

        assert struct.name == 'Exception'
        assert struct.doc == 'Exception doc.'
        assert struct.is_exception
        assert struct.location.lineno == 5

    def test_fields(self):
        s = '''
            package test;

            struct Struct {
                field0 Type;
                field1 AnotherStruct;
            }
        '''

        file, _ = self.parser.parse(s, 'file')
        struct = file.types[0]
        fields = struct.fields

        assert len(fields) == 2

        field0 = fields[0]
        assert field0.name == 'field0'
        assert field0._type.name == 'Type'
        assert field0.location.lineno == 5

        field1 = fields[1]
        assert field1.name == 'field1'
        assert field1._type.name == 'AnotherStruct'
        assert field1.location.lineno == 6

    def test_interface(self):
        s = '''
            package test;

            /** Interface doc. */
            interface Interface {}
        '''

        file, _ = self.parser.parse(s, 'file')
        interface = file.types[0]

        assert interface.name == 'Interface'
        assert interface.doc == 'Interface doc.'
        assert interface.location.lineno == 5

    def test_methods(self):
        s = '''
            package test;

            interface Interface {
                /** Method zero. */
                GET method0() void;

                /** Method one. */
                POST method1(
                    arg0 type0,
                    arg1 type1) result;
                
                /** Request method. */
                POST method2(Request) Response;
            }
        '''

        file, _ = self.parser.parse(s, 'file.pdef')
        interface = file.types[0]
        methods = interface.methods

        assert len(methods) == 3

        method0 = methods[0]
        assert method0.name == 'method0'
        assert method0.doc == 'Method zero.'
        assert method0.is_get
        assert method0.location.lineno == 6
        assert method0.result is lang.VOID
        assert method0.args == []

        method1 = methods[1]
        assert method1.name == 'method1'
        assert method1.doc == 'Method one.'
        assert method1.is_post
        assert method1.location.lineno == 9
        assert method1._result.name == 'result'
        assert method1._result.location.lineno == 11

        assert len(method1.args) == 2
        assert method1.args[0].name == 'arg0'
        assert method1.args[0]._type.name == 'type0'
        assert method1.args[0]._type.location.lineno == 10
        assert method1.args[1].name == 'arg1'
        assert method1.args[1]._type.name == 'type1'
        assert method1.args[1]._type.location.lineno == 11
        
        method2 = methods[2]
        assert method2.name == 'method2'
        assert method2.is_post
        assert method2.args == []
        assert method2._request.name == 'Request'
        assert method2._result.name == 'Response'

    def test_collections(self):
        s = '''
            package test;

            struct Struct {
                field0  list<
                    list<
                        Element>>;
                field1  set<set<Element>>;
                field2  map<list<Key>, set<Value>>;
            }
        '''

        file, _ = self.parser.parse(s, 'file')
        fields = file.types[0].fields

        # List.
        list0 = fields[0].type
        assert isinstance(list0, lang.List)
        assert isinstance(list0.element, lang.List)
        assert list0.element._element.name == 'Element'
        assert list0.location.lineno == 5
        assert list0.element.location.lineno == 6
        assert list0.element._element.location.lineno == 7

        # Set.
        set0 = fields[1].type
        assert isinstance(set0, lang.Set)
        assert isinstance(set0.element, lang.Set)
        assert set0.element._element.name == 'Element'

        assert set0.location.lineno == 8
        assert set0.element.location.lineno == 8
        assert set0.element._element.location.lineno == 8

        # Map.
        map0 = fields[2].type
        assert isinstance(map0, lang.Map)
        assert isinstance(map0.key, lang.List)
        assert isinstance(map0.value, lang.Set)
        assert map0.key._element.name == 'Key'
        assert map0.value._element.name == 'Value'

        assert map0.location.lineno == 9
        assert map0.key.location.lineno == 9
        assert map0.value.location.lineno == 9


class TestCleanupDocstrings(unittest.TestCase):
    def test_oneline(self):
        s = cleanup_docstring('hello, world')
        assert s == 'hello, world'

        s = cleanup_docstring('/** hello, world */')
        assert s == 'hello, world'

        s = cleanup_docstring(' /**   hello, world    */  ')
        assert s == 'hello, world'

    def test_multiline(self):
        s = cleanup_docstring('''/**
         * hello,
         * world
         */ ''')
        assert s == 'hello,\nworld'

    def test_multiline_indented_text(self):
        s = cleanup_docstring('''/**
         * yaml:
         *   key0: value0
         *   key1: value1
         */''')

        assert s == 'yaml:\n  key0: value0\n  key1: value1'
