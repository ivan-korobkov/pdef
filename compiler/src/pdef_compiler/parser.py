# encoding: utf-8
import functools
import logging
import re
import os.path
import ply.lex as lex
import ply.yacc as yacc
from io import open

import pdef_lang

EXTENSION = '.pdef'
ENCODING = 'utf8'


def create_parser():
    '''Create a new parser, the parser is reusable but not thread-safe.'''
    return Parser()


class FileErrors(object):
    '''FileErrors class combines a path and its error messages into a single error.
    This error supports pretty printing.'''
    def __init__(self, path, errors):
        self.path = path
        self.errors = list(errors)

    def __unicode__(self):
        s = [self.path]
        for e in self.errors:
            s.append('  ' + unicode(e))

        return '\n'.join(s)

    def __str__(self):
        return unicode(self).encode('utf8')


class Parser(object):
    '''Pdef parser. It is reusable but not thread-safe.'''

    def __init__(self, extension=EXTENSION, encoding=ENCODING):
        self.extension = (extension if extension.startswith('.') else '.' + extension).lower()
        self.encoding = encoding
        self.grammar = _Grammar(self._error)

        # Some docs on options:
        # * optimize=False and write_tables=False force to generate tabmodule each time
        #   parser is created. It is required for production.
        # * module=self.grammar sets the grammar for a lexer and a parser.
        # * start='file' sets the start grammar rule.

        self.lexer = lex.lex(module=self.grammar, optimize=False, debug=False, reflags=re.UNICODE)
        self.parser = yacc.yacc(module=self.grammar, optimize=False, write_tables=False,
                                start='module', debug=False)

        # These are cleaned on each parse invocation.
        self._errors = []
        self._path = None

    def parse_path(self, path):
        '''Parse a directory or a file and return a list of modules and a list of errors.'''
        if not os.path.exists(path):
            return None, ['Path does not exist %r' % path]

        if os.path.isdir(path):
            return self.parse_directory(path)

        module, errors = self.parse_file(path)
        return [module] if module else [], errors

    def parse_directory(self, path):
        '''Recursively parse a directory, return a list of modules and a list of errors.'''
        if not os.path.exists(path):
            return None, ['Directory does not exist %r' % path]

        if not os.path.isdir(path):
            return None, ['Not a directory %r' % path]

        logging.info('Walking %s' % path)

        modules = []
        errors = []

        for root, dirs, files in os.walk(path):
            for file0 in files:
                ext = os.path.splitext(file0)[1]
                if ext.lower() != self.extension:
                    continue

                filepath = os.path.join(root, file0)
                module, errors0 = self.parse_file(filepath)
                if module:
                    modules.append(module)
                errors += errors0

        return modules, errors

    def parse_file(self, path):
        '''Parse a file into a module, return a module and a list of errors.'''
        if not os.path.exists(path):
            return None, ['File does not exist %r' % path]

        if not os.path.isfile(path):
            return None, ['Not a file %r' % path]

        with open(path, 'r', encoding=self.encoding) as f:
            s = f.read()

        return self.parse_string(s, path)

    def parse_string(self, s, path=None):
        '''Parses a string into a module, return a module and a list of errors.'''
        logging.info('Parsing %s', path)

        # Clear the variables.
        self._errors = []
        self._path = path

        try:
            module = self.parser.parse(s, tracking=True, lexer=self.lexer)

            errors = list(self._errors)
            module = None if errors else module
            if module:
                module.path = path

            return module, [FileErrors(path, self._errors)] if errors else []
        finally:
            self._errors = None
            self._path = None

    def _error(self, msg):
        self._errors.append(msg)


def _location(t, token_position):
    lineno = t.lineno(token_position)
    return pdef_lang.Location(lineno)


def _with_location(token_position):
    def decorator(func):
        def set_location(self, t):
            func(self, t)
            t[0].location = _location(t, token_position)

        functools.update_wrapper(set_location, func)
        return set_location

    return decorator


class _Tokens(object):
    '''Lexer tokens.'''

    # Simple reserved words.
    types = (
        'BOOL', 'INT16', 'INT32', 'INT64', 'FLOAT', 'DOUBLE',
        'STRING', 'OBJECT', 'VOID', 'LIST', 'SET', 'MAP', 'ENUM',
        'MESSAGE', 'EXCEPTION', 'INTERFACE')

    reserved = types + ('MODULE', 'FROM', 'IMPORT')

    tokens = reserved + \
        ('DOT', 'COLON', 'COMMA', 'SEMI',
         'LESS', 'GREATER', 'LBRACE', 'RBRACE',
         'LPAREN', 'RPAREN',
         'IDENTIFIER', 'DOC') \
        + ('DISCRIMINATOR', 'FORM', 'INDEX', 'POST', 'THROWS')

    # Regexp for simple rules.
    t_DOT = r'.'
    t_COLON = r'\:'
    t_COMMA = r'\,'
    t_SEMI = r'\;'
    t_LESS = r'\<'
    t_GREATER = r'\>'
    t_LBRACE = r'\{'
    t_RBRACE = r'\}'
    t_LPAREN = r'\('
    t_RPAREN = r'\)'

    # Regexp for options.
    t_DISCRIMINATOR = r'@discriminator'
    t_FORM = r'@form'
    t_INDEX = r'@index'
    t_POST = r'@post'
    t_THROWS = r'@throws'

    # Ignored characters
    t_ignore = " \t"

    # Reserved words map {lowercase: UPPERCASE}.
    # Used as a type in IDENTIFIER.
    reserved_map = {}
    for r in reserved:
        reserved_map[r.lower()] = r

    def t_IDENTIFIER(self, t):
        r'[a-zA-Z_]{1}[a-zA-Z0-9_]*'
        t.type = self.reserved_map.get(t.value, 'IDENTIFIER')
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

    def t_error(self, t):
        self._error(u"Illegal character '%s', line %s" % (t.value[0], t.lexer.lineno))
        t.lexer.skip(1)

    def _error(self, msg):
        raise NotImplementedError()


class _GrammarRules(object):
    '''Parser grammar rules.'''
    def _error(self, msg):
        raise NotImplementedError()

    # Starting point.
    @_with_location(0)
    def p_module(self, t):
        '''
        module : doc MODULE module_name SEMI imports definitions
        '''
        doc = t[1]
        name = t[3]
        imports = t[5]
        definitions = t[6]
        t[0] = pdef_lang.Module(name, imports=imports, definitions=definitions, doc=doc)

    # Any absolute name, returns a list.
    def p_absolute_name(self, t):
        '''
        absolute_name : absolute_name DOT IDENTIFIER
                      | IDENTIFIER
        '''
        self._list(t, separated=True)

    def p_type_name(self, t):
        '''
        type_name : absolute_name
        '''
        t[0] = '.'.join(t[1])

    def p_module_name(self, t):
        '''
        module_name : absolute_name
        '''
        t[0] = '.'.join(t[1])

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

    @_with_location(1)
    def p_import(self, t):
        '''
        import : absolute_import
               | relative_import
        '''
        t[0] = t[1]

    def p_absolute_import(self, t):
        '''
        absolute_import : IMPORT module_name SEMI
        '''
        t[0] = pdef_lang.AbsoluteImport(t[2])

    def p_relative_import(self, t):
        '''
        relative_import : FROM module_name IMPORT relative_import_names SEMI
        '''
        t[0] = pdef_lang.RelativeImport(t[2], t[4])

    def p_relative_import_names(self, t):
        '''
        relative_import_names : relative_import_names COMMA module_name
                              | module_name
        '''
        self._list(t, separated=True)

    def p_definitions(self, t):
        '''
        definitions : definitions definition
                    | definition
                    | empty
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
        t[0] = d

    @_with_location(2)
    def p_enum(self, t):
        '''
        enum : ENUM IDENTIFIER LBRACE enum_values RBRACE
        '''
        t[0] = pdef_lang.Enum(t[2], values=t[4])

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
                        | empty
        '''
        self._list(t, separated=1)

    @_with_location(2)
    def p_enum_value(self, t):
        '''
        enum_value : doc IDENTIFIER
        '''
        t[0] = pdef_lang.EnumValue(t[2], doc=t[1])

    # Message definition
    @_with_location(3)
    def p_message(self, t):
        '''
        message : message_form message_or_exc IDENTIFIER message_base LBRACE fields RBRACE
        '''
        is_form = t[1]
        is_exception = t[2].lower() == 'exception'
        name = t[3]
        base, discriminator_value = t[4]
        fields = t[6]

        t[0] = pdef_lang.Message(name, base=base, discriminator_value=discriminator_value,
                                 declared_fields=fields, is_exception=is_exception,
                                 is_form=is_form)

    def p_message_form(self, t):
        '''
        message_form : FORM
                     | empty
        '''
        t[0] = bool(t[1])

    def p_message_or_exception(self, t):
        '''
        message_or_exc : MESSAGE
                       | EXCEPTION
        '''
        t[0] = t[1]

    def p_message_base(self, t):
        '''
        message_base : COLON type LPAREN type RPAREN
                     | COLON type
                     | empty
        '''
        base, discriminator = None, None

        if len(t) == 3:
            base = t[2]
        elif len(t) == 6:
            base = t[2]
            discriminator = t[4]

        if base:
            base.location = _location(t, 2)

        t[0] = base, discriminator

    # List of message fields
    def p_fields(self, t):
        '''
        fields : fields field
               | field
               | empty
        '''
        self._list(t)

    # Single message field
    @_with_location(2)
    def p_field(self, t):
        '''
        field : doc IDENTIFIER type field_discriminator SEMI
        '''
        doc = t[1]
        name = t[2]
        type0 = t[3]
        is_discriminator = t[4]
        t[0] = pdef_lang.Field(name, type0, is_discriminator=is_discriminator, doc=doc)

    def p_field_discriminator(self, t):
        '''
        field_discriminator : DISCRIMINATOR
                            | empty
        '''
        t[0] = bool(t[1])

    # Interface definition
    @_with_location(3)
    def p_interface(self, t):
        '''
        interface : interface_exc INTERFACE IDENTIFIER LBRACE methods RBRACE
        '''
        exc = t[1]
        name = t[3]
        methods = t[5]

        t[0] = pdef_lang.Interface(name, exc=exc, declared_methods=methods)

    def p_interface_exc(self, t):
        '''
        interface_exc : THROWS LPAREN type RPAREN
                      | empty
        '''
        if len(t) == 5:
            t[0] = t[3]
        else:
            t[0] = None

    def p_methods(self, t):
        '''
        methods : methods method
                | method
                | empty
        '''
        self._list(t)

    @_with_location(3)
    def p_method(self, t):
        '''
        method : doc method_attrs IDENTIFIER LPAREN method_args RPAREN type SEMI
        '''
        doc = t[1]
        attrs = t[2]
        name = t[3]
        args = t[5]
        result = t[7]
        is_index = '@index' in attrs
        is_post = '@post' in attrs
        t[0] = pdef_lang.Method(name, result=result, args=args, is_index=is_index, is_post=is_post,
                                doc=doc)

    def p_method_args(self, t):
        '''
        method_args : method_args COMMA method_arg
                    | method_arg
                    | empty
        '''
        self._list(t, separated=True)

    @_with_location(2)
    def p_method_arg(self, t):
        '''
        method_arg : doc IDENTIFIER type
        '''
        t[0] = pdef_lang.MethodArg(t[2], t[3])

    def p_method_attrs(self, t):
        '''
        method_attrs : method_attrs method_attr
                     | method_attr
                     | empty
        '''
        self._list(t)

    def p_method_attr(self, t):
        '''
        method_attr : INDEX
                    | POST
        '''
        t[0] = t[1]

    @_with_location(1)
    def p_type(self, t):
        '''
        type : value_ref
             | list_ref
             | set_ref
             | map_ref
             | def_ref
        '''
        t[0] = t[1]

    def p_value_ref(self, t):
        '''
        value_ref : BOOL
                  | INT16
                  | INT32
                  | INT64
                  | FLOAT
                  | DOUBLE
                  | STRING
                  | OBJECT
                  | VOID
        '''
        t[0] = pdef_lang.reference(t[1].lower())

    def p_list_ref(self, t):
        '''
        list_ref : LIST LESS type GREATER
        '''
        t[0] = pdef_lang.ListReference(t[3])

    def p_set_ref(self, t):
        '''
        set_ref : SET LESS type GREATER
        '''
        t[0] = pdef_lang.SetReference(t[3])

    def p_map_ref(self, t):
        '''
        map_ref : MAP LESS type COMMA type GREATER
        '''
        t[0] = pdef_lang.MapReference(t[3], t[5])

    def p_def_ref(self, t):
        '''
        def_ref : type_name
        '''
        t[0] = pdef_lang.reference(t[1])

    def p_error(self, t):
        if t is None:
            msg = u'Unexpected end of file'
        else:
            msg = u"Syntax error at '%s', line %s" % (t.value, t.lexer.lineno)
        self._error(msg)

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


class _Grammar(_GrammarRules, _Tokens):
    '''Grammar combines grammar rules and lexer tokens. It can be passed to the ply.yacc
    and pla.lex functions as the module argument.'''

    def __init__(self, error_func):
        self._error = error_func
