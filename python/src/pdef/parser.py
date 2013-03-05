# encoding: utf-8
import logging
import ply.lex as lex
import ply.yacc as yacc

from pdef import lang


class Tokens(object):
    # Simple reserved words.
    reserved = ('AS', 'ENUM', 'IMPORT', 'INHERITS', 'MESSAGE', 'ON', 'POLYMORPHIC', 'OPTIONS',
                'MODULE', 'NATIVE')

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
    def p_module(self, t):
        '''
        module : module_name imports definitions
        '''
        name = t[1]
        imports = t[2]
        definitions = t[3]
        t[0] = lang.Module(name, imports=imports, definitions=definitions)

    # Empty token to support optional values.
    def p_empty(self, t):
        '''
        empty :
        '''
        pass

    # The first line is a file.
    def p_module_name(self, t):
        '''
        module_name : MODULE IDENTIFIER SEMI
        '''
        t[0] = t[2]

    # Optional module imports.
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
        t[0] = lang.ImportRef(name, alias)

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
        generic_args = [] if len(t) == 2 else t[3]
        t[0] = lang.Ref(name, *generic_args)

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
        options = dict(t[5])
        print options
        t[0] = lang.Native(name, variables=variables, options=lang.NativeOptions(**options))

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
        t[0] = lang.EnumValue(t[1])

    # Message definition
    def p_message(self, t):
        '''
        message : MESSAGE IDENTIFIER message_header message_body
        '''
        name = t[2]
        variables, base, base_type, polymorphism = t[3]
        options, fields = t[4]

        t[0] = lang.Message(name, variables=variables, base=base, base_type=base_type,
                            polymorphism=polymorphism, declared_fields=fields)

    def p_message_header(self, t):
        '''
        message_header : variables base polymorphism
        '''
        base, base_type = t[2]
        t[0] = t[1], base, base_type, t[3]

    def p_message_body(self, t):
        '''
        message_body : LBRACE message_options fields RBRACE
        '''
        t[0] = (t[2], t[3])

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

    # Message inheritance
    def p_message_base(self, t):
        '''
        base : INHERITS type AS IDENTIFIER
             | empty
        '''
        if len(t) == 2:
            t[0] = None, None
        else:
            t[0] = t[2], lang.Ref(t[4])

    def p_message_polymorphism(self, t):
        '''
        polymorphism : POLYMORPHIC ON STRING AS IDENTIFIER
                     | empty
        '''
        if len(t) == 2:
            t[0] = None
        else:
            field = lang.Ref(t[3])
            default_type = lang.Ref(t[5])
            t[0] = lang.MessagePolymorphism(field, default_type)

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
