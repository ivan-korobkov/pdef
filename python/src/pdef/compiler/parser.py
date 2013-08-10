# encoding: utf-8
import logging
import re
import ply.lex as lex
import ply.yacc as yacc

from pdef.compiler import ast
from pdef.types import GeneratedException

__all__ = ('parse', 'parse_string')


def parse_file(path):
    '''Parses a module file.'''
    parser = _Parser(path)
    return parser.parse_file(path)


def parse_string(s):
    '''Parses a module string.'''
    parser = _Parser('stream')
    return parser.parse_string(s)


class _Tokens(object):
    # Simple reserved words.
    types = (
        'BOOL', 'INT16', 'INT32', 'INT64', 'FLOAT', 'DOUBLE',
        'STRING', 'OBJECT', 'VOID', 'LIST', 'SET', 'MAP', 'ENUM',
        'MESSAGE', 'EXCEPTION', 'INTERFACE')

    reserved = types + ('MODULE', 'FROM', 'IMPORT', 'DISCRIMINATOR', 'THROWS')

    tokens = reserved + (
        'COLON', 'COMMA', 'SEMI',
        'LESS', 'GREATER',
        'LBRACE', 'RBRACE',
        'LPAREN', 'RPAREN',
        'IDENTIFIER', 'DOC')

    # Regular expressions for simple rules
    t_COLON = r'\:'
    t_COMMA = r'\,'
    t_SEMI = r'\;'
    t_LESS = r'\<'
    t_GREATER = r'\>'
    t_LBRACE  = r'\{'
    t_RBRACE  = r'\}'
    t_LPAREN = r'\('
    t_RPAREN = r'\)'

    # Ignored characters
    t_ignore = " \t"

    # Reserved words map {lowercase: UPPERCASE}.
    # Used as a type in IDENTIFIER.
    reserved_map = {}
    for r in reserved:
        reserved_map[r.lower()] = r

    def t_IDENTIFIER(self, t):
        r'~?[a-zA-Z_]{1}[a-zA-Z0-9_]*(\.[a-zA-Z_]{1}[a-zA-Z0-9_]*)*'
        if t.value.startswith('~'): # Allows to use reserved words.
            t.value = t.value[1:]
        else:
            t.type = self.reserved_map.get(t.value, "IDENTIFIER")

        return t

    def t_comment(self, t):
        r'//.*\n'
        t.lineno += 1

    # Skip the new line and increment the lineno counter.
    def t_newline(self, t):
        r'\n+'
        t.lexer.lineno += t.value.count("\n")

    doc_start = re.compile('^\s*\**\s*', re.MULTILINE)
    doc_end = re.compile('\s\*$', re.MULTILINE)

    # Pdef docstring.
    def t_DOC(self, t):
        r'\/\*\*((.|\n)*?)\*\/'
        t.lineno += t.value.count('\n')
        value = t.value.strip('/')
        value = self.doc_start.sub('', value)
        value = self.doc_end.sub('', value)
        t.value = value
        return t

    # Print the error message
    def t_error(self, t):
        self._error("Illegal character %s", t.value[0])
        t.lexer.skip(1)

    def _error(self, msg, *args):
        raise NotImplementedError()


class _GrammarRules(object):
    # Starting point.
    def p_file(self, t):
        '''
        file : MODULE IDENTIFIER SEMI imports definitions
        '''
        name = t[2]
        imports = t[4]
        definitions = t[5]
        t[0] = ast.File(name, imports=imports, definitions=definitions)
        t[0].location = self._location(t)

    # Empty token to support optional values.
    def p_empty(self, t):
        '''
        empty :
        '''
        pass

    def p_doc(self, t):
        '''
        doc : DOC
            | empty
        '''
        if len(t) == 2:
            t[0] = t[1]
        else:
            t[0] = ''

    def p_imports(self, t):
        '''
        imports : imports import
                | import
                | empty
        '''
        self._list(t)

    def p_import(self, t):
        '''
        import : absolute_import
               | relative_import
        '''
        t[0] = t[1]

    def p_absolute_import(self, t):
        '''
        absolute_import : IMPORT IDENTIFIER SEMI
        '''
        t[0] = ast.AbsoluteImport(t[2])

    def p_relative_import(self, t):
        '''
        relative_import : FROM IDENTIFIER IMPORT relative_import_names SEMI
        '''
        t[0] = ast.RelativeImport(t[2], *t[4])

    def p_relative_import_names(self, t):
        '''
        relative_import_names : relative_import_names COMMA IDENTIFIER
                              | IDENTIFIER
        '''
        self._list(t, separated=True)

    def p_definitions(self, t):
        '''
        definitions : definitions definition
                    | definition
        '''
        self._list(t)

    def p_definition(self, t):
        '''
        definition : doc enum
                   | doc message
                   | doc interface
        '''
        d = t[2]
        d.doc = t[1]
        d.location = self._location(t)
        t[0] = d

    def p_enum(self, t):
        '''
        enum : ENUM IDENTIFIER LBRACE enum_values RBRACE
        '''
        t[0] = ast.Enum(t[2], values=t[4])

    def p_enum_values(self, t):
        '''
        enum_values : enum_value_list SEMI
                    | enum_value_list
        '''
        t[0] = t[1]

    def p_enum_value_list(self, t):
        '''
        enum_value_list : enum_value_list COMMA enum_value
                        | enum_value
        '''
        self._list(t, separated=1)

    def p_enum_value(self, t):
        '''
        enum_value : doc IDENTIFIER
        '''
        t[0] = t[2]

    # Message definition
    def p_message(self, t):
        '''
        message : message_or_exc IDENTIFIER message_base LBRACE field_list RBRACE
        '''
        name = t[2]
        base, base_type = t[3]
        fields = t[5]
        is_exception = t[1].lower() == 'exception'

        t[0] = ast.Message(name, base=base, base_type=base_type, fields=fields,
                           is_exception=is_exception)

    def p_message_or_exception(self, t):
        '''
        message_or_exc : MESSAGE
                       | EXCEPTION
        '''
        t[0] = t[1]

    def p_message_base(self, t):
        '''
        message_base : COLON type LPAREN def_type RPAREN
                     | COLON type
                     | empty
        '''
        if len(t) == 2:
            t[0] = None, None
        elif len(t) == 3:
            t[0] = t[2], None
        elif len(t) == 6:
            t[0] = t[2], t[4]
        else:
            raise SyntaxError

    def p_field_list(self, t):
        '''
        field_list : fields SEMI
                   | fields
        '''
        t[0] = t[1]

    # List of message fields
    def p_fields(self, t):
        '''
        fields : fields SEMI field
               | field
               | empty
        '''
        self._list(t, separated=True)

    # Single message field
    def p_field(self, t):
        '''
        field : doc IDENTIFIER type field_options
        '''
        options = t[4]
        is_discriminator = 'discriminator' in options
        t[0] = ast.Field(t[2], t[3], is_discriminator=is_discriminator)

    def p_field_options(self, t):
        '''
        field_options : COMMA DISCRIMINATOR
                      | empty
        '''
        if len(t) == 3:
            t[0] = [t[2]]
        else:
            t[0] = []

    # Interface definition
    def p_interface(self, t):
        '''
        interface : INTERFACE IDENTIFIER interface_options LBRACE methods RBRACE
        '''
        name = t[2]
        options = t[3]
        methods = t[5]

        base = options.base if options else None
        exc = options.exc if options else None

        t[0] = ast.Interface(name, base=base, exc=exc, methods=methods)

    def p_interface_options(self, t):
        '''
        interface_options : COLON type COMMA THROWS type
                          | COLON THROWS type
                          | COLON type
                          | empty
        '''
        base = None
        exc = None
        if len(t) == 6:
            base = t[2]
            exc = t[5]
        elif len(t) == 4:
            exc = t[3]
        elif len(t) == 3:
            base = t[2]

        t[0] = ast.InterfaceOptions(base=base, exc=exc)

    def p_methods(self, t):
        '''
        methods : methods method
                | method
                | empty
        '''
        self._list(t)

    def p_method(self, t):
        '''
        method : doc IDENTIFIER LPAREN fields RPAREN type method_options SEMI
        '''
        doc = t[1]
        name = t[2]
        args = t[4]
        result = t[6]
        t[0] = ast.Method(name, args=args, result=result, doc=doc)

    def p_method_options(self, t):
        '''
        method_options : empty
        '''
        t[0] = []

    def p_type(self, t):
        '''
        type : value_type
            | list_type
            | set_type
            | map_type
            | def_type
        '''
        t[0] = t[1]

    def p_value_type(self, t):
        '''
        value_type : BOOL
                   | INT16
                   | INT32
                   | INT64
                   | FLOAT
                   | DOUBLE
                   | STRING
                   | OBJECT
                   | VOID
        '''
        t[0] = ast.TypeRef(t[1].lower())

    def p_list_type(self, t):
        '''
        list_type : LIST LESS type GREATER
        '''
        t[0] = ast.ListRef(t[3])

    def p_set_type(self, t):
        '''
        set_type : SET LESS type GREATER
        '''
        t[0] = ast.SetRef(t[3])

    def p_map_type(self, t):
        '''
        map_type : MAP LESS type COMMA type GREATER
        '''
        t[0] = ast.MapRef(t[3], t[5])

    def p_def_type(self, t):
        '''
        def_type : IDENTIFIER
        '''
        t[0] = ast.DefRef(t[1])

    def p_error(self, t):
        self._error("Syntax error at '%s', line %s", t.value, t.lexer.lineno)

    def _list(self, t, separated=False):
        '''List builder, supports separated and empty lists.

        Supported grammar:
        list : list [optional separator] item
             | item
             | empty
        '''
        if len(t) == 1:
            t[0] = []
        elif len(t) == 2:
            if t[1] is None:
                t[0] = []
            else:
                t[0] = [t[1]]
        else:
            t[0] = t[1]
            if not separated:
                t[0].append(t[2])
            else:
                t[0].append(t[3])

    def _error(self, msg, *args):
        raise NotImplementedError()

    def _location(self, t):
        raise NotImplementedError()


class _Parser(_GrammarRules, _Tokens):
    def __init__(self, path, debug=False):
        super(_Parser, self).__init__()
        self.debug = debug
        self.lexer = lex.lex(module=self, debug=debug)
        self.parser = yacc.yacc(module=self, debug=debug, tabmodule='pdef.compiler.parsetab',
                                start='file')
        self.errors = []
        self.path = path

    def parse_file(self, path):
        with open(path, 'r') as f:
            s = f.read()

        return self.parse_string(s)

    def parse_string(self, s):
        result = self.parser.parse(s, debug=self.debug, tracking=True, lexer=self.lexer)
        if self.errors:
            raise GeneratedException('Syntax errors')
        return result

    def _error(self, msg, *args):
        msg = '%s: %s' % (self.path, msg)
        logging.error(msg, *args)
        self.errors.append(msg)

    def _location(self, t):
        return ast.Location(self.path, t.lineno(1))
