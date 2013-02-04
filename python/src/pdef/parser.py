# encoding: utf-8
import logging
import ply.lex as lex
import ply.yacc as yacc

from pdef import lang


class Tokens(object):

    # Simple reserved words.
    reserved = ('AS', 'EXTENDS', 'ENUM', 'IMPORT', 'MESSAGE', 'OPTIONS', 'PACKAGE', 'NATIVE')

    # All tokens.
    tokens = reserved + (
        'COLON', 'COMMA', 'SEMI',
        'LESS', 'GREATER',
        'LBRACE', 'RBRACE', 'LBRACKET', 'RBRACKET',
        'IDENTIFIER', 'STRING')

    # Regular expressions for simple rules
    t_COLON = r'\:'
    t_COMMA = r'\,'
    t_SEMI = r'\;'
    t_LESS = r'\<'
    t_GREATER = r'\>'
    t_LBRACE  = r'\{'
    t_RBRACE  = r'\}'
    t_LBRACKET = r'\['
    t_RBRACKET = r'\]'

    # Ignored characters
    t_ignore = " \t"

    # Reserved words map {lowercase: UPPERCASE}.
    # Used as a type in IDENTIFIER.
    reserved_map = {}
    for r in reserved:
        reserved_map[r.lower()] = r

    def t_STRING(self, t):
        r'\".+?\"'
        t.value = t.value.strip('"')
        return t

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
    def p_package(self, t):
        '''
        package : package_name imports definitions
        '''
        name = t[1]
        imports = t[2]
        definitions = t[3]
        t[0] = lang.Package(name, imports=imports, definitions=definitions)

    # Empty token to support optional values.
    def p_empty(self, t):
        '''
        empty :
        '''
        pass

    # The first line is a file.
    def p_package_name(self, t):
        '''
        package_name : PACKAGE IDENTIFIER SEMI
        '''
        t[0] = t[2]

    # Optional package imports.
    def p_imports(self, t):
        '''
        imports : imports import
                | import
                | empty
        '''
        self._list(t)

    # Package import.
    def p_import(self, t):
        '''
        import : IMPORT IDENTIFIER AS IDENTIFIER SEMI
               | IMPORT IDENTIFIER SEMI
        '''
        name = t[2]
        alias = name if len(t) == 4 else t[4]
        t[0] = lang.ModuleReference(name, alias)

    # Data dict fields: k: v, k1: v2
    def p_data_fields(self, t):
        '''
        data_fields : data_fields COMMA data_field
                    | data_field
                    | empty
        '''
        self._list(t, separated=True)

    # Data field: "name: value"
    def p_data_field(self, t):
        '''
        data_field : IDENTIFIER COLON STRING
        '''
        t[0] = (t[1], t[3])

    # Type reference with optional generic arguments.
    def p_type(self, t):
        '''
        type : IDENTIFIER LESS types GREATER
             | IDENTIFIER
        '''
        name = t[1]
        args = [] if len(t) == 2 else t[3]
        t[0] = lang.Reference(name, args=args)

    # List of generic arguments.
    def p_types(self, t):
        '''
        types : types COMMA type
              | type
        '''
        self._list(t, separated=1)

    # List of type definitions.
    def p_definitions(self, t):
        '''
        definitions : definitions definition
                    | definition
        '''
        self._list(t)

    # Single type definition.
    def p_definition(self, t):
        '''
        definition : native
                   | enum
                   | message
        '''
        t[0] = t[1]

    # Native type definition.
    def p_native(self, t):
        '''
        native : NATIVE IDENTIFIER variables LBRACE data_fields RBRACE
        '''
        name = t[2]
        variables = t[3]
        options = t[5]
        t[0] = lang.Native(name, variables=variables, options=options)

    # Enum definition.
    def p_enum(self, t):
        '''
        enum : ENUM IDENTIFIER LBRACE enum_values SEMI RBRACE
        '''
        t[0] = lang.Enum(t[2], values=t[4])

    # List of enum values.
    def p_enum_values(self, t):
        '''
        enum_values : enum_values COMMA enum_value
                    | enum_value
        '''
        self._list(t, separated=1)

    # Single enum value
    def p_enum_value(self, t):
        '''
        enum_value : IDENTIFIER
        '''
        t[0] = t[1]

    # Message definition
    def p_message(self, t):
        '''
        message : MESSAGE IDENTIFIER variables base LBRACE message_options fields RBRACE
        '''
        name = t[2]
        variables = t[3]
        base = t[4]
        fields = t[7]
        # TODO: Message options
        options = t[6]
        t[0] = lang.Message(name, variables=variables, base=base, declared_fields=fields)

    # Message options: options [];
    def p_message_options(self, t):
        '''
        message_options : OPTIONS options SEMI
                        | empty
        '''
        if len(t) == 2:
            t[0] = ()
        else:
            t[0] = t[2]

    # Options dict: [name: value, name2: value2]
    def p_options(self, t):
        '''
        options : LBRACKET data_fields RBRACKET
                | empty
        '''
        if len(t) == 2:
            t[0] = ()
        else:
            t[0] = t[2]

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
        field : IDENTIFIER type field_options SEMI
        '''
        # TODO: Field options
        t[0] = lang.Field(t[1], type=t[2])

    # Optional field options: {}
    def p_field_options(self, t):
        '''
        field_options : options
                      | empty
        '''
        t[0] = t[1]

    # Generic variables in a definition name.
    def p_variables(self, t):
        '''
        variables : LESS variable_list GREATER
                  | empty
        '''
        if len(t) == 2:
            t[0] = []
        else:
            t[0] = t[2]

    # List of variable names in variables.
    def p_variable_list(self, t):
        '''
        variable_list : variable_list COMMA variable
                      | variable
        '''
        self._list(t, separated=1)

    # Generic variable in a definition name.
    def p_variable(self, t):
        '''
        variable : IDENTIFIER
        '''
        t[0] = lang.Variable(t[1])

    # Base message
    def p_base(self, t):
        '''
        base : EXTENDS type
             | empty
        '''
        if len(t) == 2:
            t[0] = None
        else:
            t[0] = t[2]

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
        self.parser = yacc.yacc(module=self, debug=debug, tabmodule='pdef.parsetab')
        self.errors = []

    def parse(self, s, **kwargs):
        return self.parser.parse(s, debug=self.debug, **kwargs)

    def _error(self, msg, *args):
        logging.error(msg, *args)
        self.errors.append(msg % args)
