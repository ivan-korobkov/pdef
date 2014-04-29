# encoding: utf-8
from __future__ import unicode_literals
import functools
import logging
import io

import re
import ply.lex as lex
import ply.yacc as yacc

from pdefc import lang
from pdefc.reserved import RESERVED


class Parser(object):
    '''Pdef parser. It is reusable but not thread-safe.'''

    def __init__(self):
        self.grammar = _Grammar()

        # Some docs on options:
        # - optimize=False and write_tables=False force a parser to generate tabfile each time
        # the parser is created.
        # - file=self.grammar sets the grammar for a lexer and a parser.
        # - start sets the start grammar rule.

        self.lexer = lex.lex(module=self.grammar, optimize=False, debug=False, reflags=re.UNICODE)
        self.parser = yacc.yacc(module=self.grammar, optimize=False, write_tables=False,
                                start='file', debug=False)

    def parse(self, s, path=None):
        '''Parse a file from a string and return (file, errors).'''
        logging.info('Parsing %s', path or 'stream')

        self.grammar.path = path
        self.grammar.errors = lang.Errors()

        try:
            lexer = self.lexer.clone()
            file = self.parser.parse(s, tracking=True, lexer=lexer)
            errors = self.grammar.errors
            if errors:
                return None, errors
            
            file.path = path
            return file, errors
        finally:
            self.grammar.path = None
            self.grammar.errors = None


def _with_location(token_position):
    def decorator(func):
        def with_location(self, t):
            func(self, t)
            t[0].location = _location(self, t, token_position)

        functools.update_wrapper(with_location, func)
        return with_location

    return decorator


def _location(self, t, token_position):
    path = self.path
    lineno = t.lineno(token_position)
    return lang.Location(path, lineno)


class _Tokens(object):
    '''Lexer tokens.'''
    path = None
    errors = None

    # Simple reserved words.
    types = (
        'BOOL',
        'INT16',
        'INT32',
        'INT64',
        'FLOAT',
        'DOUBLE',
        'STRING',
        'DATETIME',
        'VOID',

        'LIST',
        'SET',
        'MAP',

        'ENUM',
        'STRUCT',
        'EXCEPTION',
        'INTERFACE')

    # Identifier types, see t_IDENTIFIER
    identifiers = types + ('PACKAGE', )
    identifiers_case_sensitive = ('POST', 'GET')
    identifier_map = {key: value for key, value in
                      [(s.lower(), s) for s in identifiers] +
                      [(s, s) for s in identifiers_case_sensitive]}

    reserved = set(RESERVED)
    tokens = identifiers + identifiers_case_sensitive + (
        'DOT',
        'COMMA',
        'SEMI',
        'LESS',
        'GREATER',
        'LBRACE',
        'RBRACE',
        'LPAREN',
        'RPAREN',
        'IDENTIFIER',
        'DOC')

    # Regexp for simple rules.
    t_DOT = r'.'
    t_COMMA = r'\,'
    t_SEMI = r'\;'
    t_LESS = r'\<'
    t_GREATER = r'\>'
    t_LBRACE = r'\{'
    t_RBRACE = r'\}'
    t_LPAREN = r'\('
    t_RPAREN = r'\)'

    # Ignored characters
    t_ignore = ' \t'

    def t_IDENTIFIER(self, t):
        r'[a-zA-Z_]{1}[a-zA-Z0-9_]*'
        t.type = self.identifier_map.get(t.value, 'IDENTIFIER')

        if t.value in self.reserved:
            lineno = t.lineno
            location = lang.Location(self.path, lineno)
            self.errors.add_error(location, '"%s" is a reserved word', t.value)

        return t

    def t_comment(self, t):
        r'//.*\n'
        t.lexer.lineno += 1
        t.lineno += 1

    # Skip a new line and increment the lexer lineno counter.
    def t_newline(self, t):
        r'\n+'
        t.lexer.lineno += t.value.count('\n')

    # Pdef docstring.
    def t_DOC(self, t):
        r'\/\*\*((.|\n)*?)\*\/'
        t.lexer.lineno += t.value.count('\n')
        t.value = cleanup_docstring(t.value)
        return t

    def t_error(self, t):
        lineno = t.lexer.lineno
        location = lang.Location(self.path, lineno)
        self.errors.add_error(location, 'Illegal character "%s"', t.value[0])
        t.lexer.skip(1)


class _GrammarRules(object):
    '''Parser grammar rules.'''
    path = None
    errors = None

    # Starting point.
    @_with_location(0)
    def p_file(self, p):
        '''
        file : doc package definitions
        '''
        path = self.path
        doc = p[1]
        package = p[2]
        types = p[3]

        file = lang.File(path, types=types)
        file.doc = doc
        p[0] = file

    def p_absolute_name(self, p):
        '''
        absolute_name : absolute_name_list
        '''
        p[0] = '.'.join(p[1])

    def p_absolute_name_list(self, p):
        '''
        absolute_name_list : absolute_name_list DOT IDENTIFIER
                           | IDENTIFIER
        '''
        self._list(p, separated=True)

    # Empty token to support optional values.
    def p_empty(self, p):
        '''
        empty :
        '''
        pass

    def p_doc(self, p):
        '''
        doc : DOC
            | empty
        '''
        if len(p) == 2:
            p[0] = p[1]
        else:
            p[0] = ''

    def p_package(self, p):
        '''
        package : PACKAGE absolute_name SEMI
        '''
        p[0] = p[2]

    def p_definitions(self, p):
        '''
        definitions : definitions definition
                    | definition
                    | empty
        '''
        self._list(p)

    def p_definition(self, p):
        '''
        definition : doc enum
                   | doc struct
                   | doc interface
        '''
        d = p[2]
        d.doc = p[1]
        p[0] = d

    @_with_location(2)
    def p_enum(self, p):
        '''
        enum : ENUM IDENTIFIER LBRACE enum_values RBRACE
        '''
        p[0] = lang.Enum(p[2], values=p[4])

    def p_enum_values(self, p):
        '''
        enum_values : enum_value_list SEMI
                    | enum_value_list
        '''
        p[0] = p[1]

    def p_enum_value_list(self, p):
        '''
        enum_value_list : enum_value_list COMMA enum_value
                        | enum_value
                        | empty
        '''
        self._list(p, separated=True)

    @_with_location(1)
    def p_enum_value(self, p):
        '''
        enum_value : IDENTIFIER
        '''
        p[0] = lang.EnumValue(p[1])

    # Struct definition
    @_with_location(2)
    def p_struct(self, p):
        '''
        struct : struct_or_exc IDENTIFIER LBRACE fields RBRACE
        '''
        is_exception = p[1].lower() == 'exception'
        name = p[2]
        fields = p[4]

        p[0] = lang.Struct(name, fields=fields, is_exception=is_exception)

    def p_struct_or_exception(self, p):
        '''
        struct_or_exc : STRUCT
                       | EXCEPTION
        '''
        p[0] = p[1]

    # List of struct fields
    def p_fields(self, p):
        '''
        fields : fields field
               | field
               | empty
        '''
        self._list(p)

    # Single struct field
    @_with_location(1)
    def p_field(self, p):
        '''
        field : IDENTIFIER type SEMI
        '''
        name = p[1]
        type0 = p[2]
        p[0] = lang.Field(name, type0)

    # Interface definition
    @_with_location(2)
    def p_interface(self, p):
        '''
        interface : INTERFACE IDENTIFIER LBRACE methods RBRACE
        '''
        name = p[2]
        methods = p[4]

        p[0] = lang.Interface(name, methods=methods)

    def p_methods(self, p):
        '''
        methods : methods method
                | method
                | empty
        '''
        self._list(p)

    @_with_location(3)
    def p_method(self, p):
        '''
        method : doc method_type IDENTIFIER LPAREN method_args RPAREN type SEMI
        '''
        doc = p[1]
        type = p[2]
        name = p[3]
        args = p[5]
        result = p[7]

        method = lang.Method(name, type=type, result=result, args=args)
        method.doc = doc
        p[0] = method

    def p_method_type(self, p):
        '''
        method_type : GET
                    | POST
        '''
        p[0] = p[1]

    def p_method_args(self, p):
        '''
        method_args : method_args COMMA method_arg
                    | method_arg
                    | empty
        '''
        self._list(p, separated=True)

    @_with_location(2)
    def p_method_arg(self, p):
        '''
        method_arg : doc IDENTIFIER type
        '''
        p[0] = lang.Argument(p[2], p[3])

    @_with_location(1)
    def p_type(self, p):
        '''
        type : value
             | list
             | set
             | map
             | ref
        '''
        p[0] = p[1]

    def p_value(self, p):
        '''
        value : BOOL
              | INT16
              | INT32
              | INT64
              | FLOAT
              | DOUBLE
              | STRING
              | DATETIME
              | VOID
        '''
        d = {
            'bool': lang.BOOL,
            'int16': lang.INT16,
            'int32': lang.INT32,
            'int64': lang.INT64,
            'float': lang.FLOAT,
            'double': lang.DOUBLE,
            'string': lang.STRING,
            'datetime': lang.DATETIME,
            'void': lang.VOID
        }

        name = p[1].lower()
        p[0] = d[name]

    def p_list(self, p):
        '''
        list : LIST LESS type GREATER
        '''
        p[0] = lang.List(p[3])

    def p_set(self, p):
        '''
        set : SET LESS type GREATER
        '''
        p[0] = lang.Set(p[3])

    def p_map(self, p):
        '''
        map : MAP LESS type COMMA type GREATER
        '''
        p[0] = lang.Map(p[3], p[5])

    def p_ref(self, p):
        '''
        ref : absolute_name
        '''
        p[0] = lang.Reference(p[1])

    def p_error(self, p):
        if p is None:
            msg = 'Unexpected end of file'
        else:
            msg = 'Syntax error at "%s"' % p.value

        lineno = p.lexer.lineno if p else 0
        location = lang.Location(self.path, lineno)
        self.errors.add_error(location, msg)

    def _list(self, p, separated=False):
        '''List builder, supports separated and empty lists.

        Supported grammar:
        list : list [optional separator] item
             | item
             | empty
        '''
        if len(p) == 1:
            p[0] = []
        elif len(p) == 2:
            if p[1] is None:
                p[0] = []
            else:
                p[0] = [p[1]]
        else:
            p[0] = p[1]
            if not separated:
                p[0].append(p[2])
            else:
                p[0].append(p[3])


class _Grammar(_GrammarRules, _Tokens):
    '''Grammar combines grammar rules and lexer tokens. It can be passed to the ply.yacc
    and pla.lex functions as the module argument.'''

    def __init__(self, path=None, errors=None):
        self.path = path
        self.errors = errors


_docstring_start_pattern = re.compile('^\s*/\*\*\s*')  # /**
_docstring_line_pattern = re.compile('^\s*\*\s?')  # *
_docstring_end_pattern = re.compile('\s*\*/\s*$')  # */


def cleanup_docstring(s):
    '''Clean up docstrings from start/end and line stars (*).'''
    lines = s.splitlines()
    result = []
    count = 0

    for line in lines:
        count += 1
        first = count == 1
        last = len(lines) == count

        # Strip the docstrings start pattern /**
        if first:
            line = _docstring_start_pattern.sub('', line)

        # String the docstrings end pattern */
        if last:
            line = _docstring_end_pattern.sub('', line)

        # Strip ap optional line start star *.
        if not first:
            line = _docstring_line_pattern.sub('', line)

        # Skip empty first and last lines.
        if not line and (first or last):
            continue
        result.append(line)

    return '\n'.join(result)
