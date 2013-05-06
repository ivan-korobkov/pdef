# encoding: utf-8
import logging
import ply.lex as lex
import ply.yacc as yacc

from pdef import ast


class Tokens(object):
    # Simple reserved words.
    reserved = ('DISCRIMINATOR', 'MODULE',
                'BOOL', 'INT16', 'INT32', 'INT64', 'FLOAT', 'DOUBLE', 'DECIMAL',
                'DATE', 'DATETIME', 'STRING', 'UUID', 'OBJECT', 'VOID',
                'LIST', 'SET', 'MAP', 'ENUM', 'MESSAGE', 'EXCEPTION', 'INTERFACE')

    tokens = reserved + (
        'COLON', 'COMMA', 'SEMI',
        'LESS', 'GREATER',
        'LBRACE', 'RBRACE',
        'LPAREN', 'RPAREN',
        'IDENTIFIER')

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
        t.type = self.reserved_map.get(t.value, "IDENTIFIER")
        if t.value.startswith('~'): # Allows to use reserved words.
            t.value = t.value[1:]

        return t

    def t_comment(self, t):
        r'//.*\n'
        t.lineno += 1

    # Skip the new line and increment the lineno counter.
    def t_newline(self, t):
        r'\n+'
        t.lexer.lineno += t.value.count("\n")

    # Print the error message
    def t_error(self, t):
        self._error("Illegal character %s", t.value[0])
        t.lexer.skip(1)

    def _error(self, msg, *args):
        raise NotImplementedError()


class GrammarRules(object):
    # Starting point.
    def p_file(self, t):
        '''
        file : MODULE IDENTIFIER SEMI definitions
        '''
        name = t[2]
        definitions = t[4]
        t[0] = ast.File(name, definitions=definitions)

    def p_definitions(self, t):
        '''
        definitions : definitions definition
                    | definition
        '''
        self._list(t)

    def p_definition(self, t):
        '''
        definition : enum
                   | message
                   | interface
        '''
        t[0] = t[1]

    # Empty token to support optional values.
    def p_empty(self, t):
        '''
        empty :
        '''
        pass

    def p_ref(self, t):
        '''
        ref : value_ref
            | list_ref
            | set_ref
            | map_ref
            | def_ref
        '''
        t[0] = t[1]

    def p_refs(self, t):
        '''
        refs : refs COMMA ref
             | ref
        '''
        self._list(t, separated=1)

    def p_value_ref(self, t):
        '''
        value_ref : BOOL
                  | INT16
                  | INT32
                  | INT64
                  | FLOAT
                  | DOUBLE
                  | DECIMAL
                  | DATE
                  | DATETIME
                  | STRING
                  | UUID
                  | OBJECT
                  | VOID
        '''
        t[0] = t[1].lower()

    def p_list_ref(self, t):
        '''
        list_ref : LIST LESS ref GREATER
        '''
        t[0] = ast.ListRef(t[3])

    def p_set_ref(self, t):
        '''
        set_ref : SET LESS ref GREATER
        '''
        t[0] = ast.SetRef(t[3])

    def p_map_ref(self, t):
        '''
        map_ref : MAP LESS ref COMMA ref GREATER
        '''
        t[0] = ast.MapRef(t[3], t[5])

    def p_def_ref(self, t):
        '''
        def_ref : IDENTIFIER
        '''
        t[0] = ast.DefinitionRef(t[1])

    def p_enum(self, t):
        '''
        enum : ENUM IDENTIFIER LBRACE enum_values RBRACE
        '''
        t[0] = ast.Enum(t[2], values=t[4])

    def p_enum_values(self, t):
        '''
        enum_values : enum_values COMMA IDENTIFIER
                    | IDENTIFIER
        '''
        self._list(t, separated=1)

    # Message definition
    def p_message(self, t):
        '''
        message : message_or_exc IDENTIFIER message_base LBRACE fields RBRACE
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
        message_base : COLON ref LPAREN ref RPAREN
                     | COLON ref
                     | empty
        '''
        if len(t) == 2:
            t[0] = None, None
        elif len(t) == 6:
            t[0] = t[2], t[4]
        else:
            t[0] = t[2], None

    # List of message fields
    def p_fields(self, t):
        '''
        fields : fields field
               | field
               | empty
        '''
        self._list(t)

    # Single message field
    def p_field(self, t):
        '''
        field : IDENTIFIER ref DISCRIMINATOR SEMI
              | IDENTIFIER ref SEMI
        '''
        t[0] = ast.Field(t[1], type=t[2], is_discriminator=len(t) == 5)

    # Interface definition
    def p_interface(self, t):
        '''
        interface : INTERFACE IDENTIFIER interface_bases LBRACE methods RBRACE
        '''
        name = t[2]
        bases = t[3]
        methods = t[4]
        t[0] = ast.Interface(name, bases=bases, methods=methods)

    def p_interface_bases(self, t):
        '''
        interface_bases : COLON refs
                        | empty
        '''
        if len(t) == 2:
            t[0] = []
        else:
            t[0] = t[2]

    def p_methods(self, t):
        '''
        methods : methods method
                | method
                | empty
        '''
        self._list(t)

    def p_method(self, t):
        '''
        method : IDENTIFIER LPAREN method_args RPAREN ref SEMI
        '''
        name = t[1]
        args = t[3]
        result = t[5]
        t[0] = ast.Method(name, args=args, result=result)

    def p_method_args(self, t):
        '''
        method_args : method_args COMMA method_arg
                    | method_arg
                    | empty
        '''
        self._list(t, separated=1)

    def p_method_arg(self, t):
        '''
        method_arg : IDENTIFIER ref
        '''
        t[0] = ast.MethodArg(t[1], t[2])

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


class Parser(Tokens, GrammarRules):
    def __init__(self, debug=False):
        super(Parser, self).__init__()
        self.debug = debug
        self.lexer = lex.lex(module=self, debug=debug)
        self.parser = yacc.yacc(module=self, debug=debug, tabmodule='pdef.parsetab', start='file')
        self.errors = []

    def parse(self, s, **kwargs):
        return self.parser.parse(s, debug=self.debug, **kwargs)

    def _error(self, msg, *args):
        logging.error(msg, *args)
        self.errors.append(msg % args)
