# encoding: utf-8
import logging
from collections import deque

import pdef_compiler
from pdef_compiler import ast
from pdef_compiler.preconditions import check_isinstance


class Type(object):
    '''Pdef type enum.'''

    # Base value types.
    BOOL = 'bool'
    INT16 = 'int16'
    INT32 = 'int32'
    INT64 = 'int64'
    FLOAT = 'float'
    DOUBLE = 'double'
    STRING = 'string'

    # Collection types.
    LIST = 'list'
    MAP = 'map'
    SET = 'set'

    # Special data type.
    OBJECT = 'object'

    # User defined data types.
    DEFINITION = 'definition'  # Abstract definition type, used in references.
    ENUM = 'enum'
    ENUM_VALUE = 'enum_value'
    MESSAGE = 'message'
    EXCEPTION = 'exception'

    # Interface and void.
    INTERFACE = 'interface'
    VOID = 'void'

    PRIMITIVES = (BOOL, INT16, INT32, INT64, FLOAT, DOUBLE, STRING)
    DATA_TYPES = PRIMITIVES + (OBJECT, LIST, MAP, SET, DEFINITION, ENUM, MESSAGE, EXCEPTION)


class Reference(object):
    def __init__(self, node, module):
        self.node = node
        self.module = module


class Symbol(object):
    '''Abstract base symbol.'''
    name = None
    doc = None

    linked = False
    validated = False

    def __str__(self):
        return self.fullname

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.name)

    @property
    def fullname(self):
        return self.name

    def validate(self):
        '''Validates this symbol.'''
        if self.validated:
            return

        self._check(self.linked, 'Symbol must be linked before validation, %s', self)
        self.validated = True
        self._validate()
        self._debug('Validated %s', self)

    def _validate(self):
        pass

    def _check(self, expression, msg, *args):
        if expression:
            return
        self._raise(msg, *args)

    def _raise(self, msg, *args):
        msg = msg % args if msg else 'Error'
        raise pdef_compiler.CompilerException(msg)

    def _debug(self, msg, *args):
        logging.debug('  ' + msg, *args)


class Package(Symbol):
    '''Protocol definition.'''
    def __init__(self):
        self.modules = []

    def __str__(self):
        return 'package'

    def add_module(self, module):
        '''Add a module to this package.'''
        if module.package:
            raise ValueError('Module is already in a package, %s' % module)

        self.modules.append(module)
        module.package = self
        self._debug('Added a module %s', module)

    def get_module(self, name):
        '''Return a module by its name.'''
        for module in self.modules:
            if module.name == name:
                return module

    def _validate(self):
        '''Validate the package.'''
        for module in self.modules:
            module.validate()


class Module(Symbol):
    '''Module in a pdef package, usually, a module is parsed from one file.'''
    def __init__(self, name, package=None):
        self.name = name
        self.package = package

        self.imports = []
        self.imported_modules = []
        self.definitions = []

        self.imports_linked = False

    def add_import(self, import0):
        '''Add a module import to this module.'''
        if import0.module:
            raise ValueError('Import is already in a module, %s' % import0)

        self.imports.append(import0)
        import0.module = self

    def add_imported_module(self, alias, module):
        '''Add an imported module to this module.'''
        imported = ImportedModule(alias, module)
        self.imported_modules.append(imported)
        return imported

    def get_imported_module(self, alias):
        '''Find a module by its import alias.'''
        for imported_module in self.imported_modules:
            if imported_module.alias == alias:
                return imported_module.module

    def add_definition(self, def0):
        '''Add a new definition to this module.'''
        if def0.module:
            raise ValueError('Definition is already in a module, def=%s,' % def0)

        self.definitions.append(def0)
        def0.module = self

        self._debug('Added a definition, module=%s, def=%s', self, def0.name)

    def add_definitions(self, *defs):
        '''Add definitions to this module.'''
        for def0 in defs:
            self.add_definition(def0)

    def get_definition(self, name):
        '''Find a definition in this module by a name.'''
        for d in self.definitions:
            if d.name == name:
                return d

    def _validate(self):
        '''Validate imports and definitions.'''
        # Check definition duplicates
        names = set()
        for def0 in self.definitions:
            name = def0.name
            self._check(name not in names, 'Duplicate definition, module=%s, def=%s', self, def0)
            names.add(name)

        # Check clashes with imports.
        for def0 in self.definitions:
            name = def0.name
            for import0 in self.imports:
                iname = import0.name
                self._check(iname != name and not iname.startswith(name + '.'),
                            'Definition clashes with an import, module=%s, def=%s', self, def0)

        for def0 in self.definitions:
            def0.validate()

    def _has_import_circle(self, another):
        '''Return true if this module has an import circle with another module.'''
        if another is self:
            return False

        q = deque(imp.module for imp in self.imports)
        while q:
            module = q.pop()
            if module is self:
                return True

            for imp in module.imports:
                q.append(imp.module)

        return False


class AbstractImport(object):
    def __init__(self):
        self.names = None
        self.module = None


class AbsoluteImport(AbstractImport):
    def __init__(self, name):
        super(AbsoluteImport, self).__init__()

        self.name = name
        self.names = [name]


class RelativeImport(AbstractImport):
    def __init__(self, prefix, *relative_names):
        super(RelativeImport, self).__init__()

        self.prefix = prefix
        self.relative_names = relative_names
        self.names = tuple(prefix + '.' + name for name in relative_names)


class ImportedModule(object):
    def __init__(self, alias, module):
        self.alias = alias
        self.module = module


class Definition(Symbol):
    '''Base definition.'''
    @classmethod
    def parse_node(cls, node, lookup):
        '''Create a definition from an AST node.'''
        if isinstance(node, ast.Enum):
            return Enum.parse_node(node, lookup)

        elif isinstance(node, ast.Message):
            return Message.parse_node(node, lookup)

        elif isinstance(node, ast.Interface):
            return Interface.parse_node(node, lookup)

        raise ValueError('Unsupported definition node %s' % node)

    is_exception = False  # The flag is set in a message constructor.

    def __init__(self, type0, name, doc=None):
        self.type = type0
        self.name = name
        self.doc = doc

        self.module = None
        self.linked = False

        self.is_primitive = self.type in Type.PRIMITIVES
        self.is_datatype = self.type in Type.DATA_TYPES
        self.is_interface = self.type == Type.INTERFACE
        self.is_message = self.type == Type.MESSAGE

        self.is_enum = self.type == Type.ENUM
        self.is_enum_value = self.type == Type.ENUM_VALUE

        self.is_list = self.type == Type.LIST
        self.is_set = self.type == Type.SET
        self.is_map = self.type == Type.MAP

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    @property
    def fullname(self):
        return '%s.%s' % (self.module.name, self.name) if self.module else self.name

    def _must_be_referenced_before(self, another):
        '''Validate that this definition is reference before another one.'''
        if not self.module or not another.module:
            return True

        if another.module is self.module:
            # They are in the same module.

            for def0 in self.module.definitions:
                if def0 is self:
                    return True
                if def0 is another:
                    self._raise('%s must be referenced before %s. Move it above in the file.',
                                self, another)

            raise AssertionError('Wrong module state')

        if self.module._has_import_circle(another.module):
            self._raise('%s must be referenced before %s, but their modules circularly import '
                        'each other. Move %s into another module.',
                        self, another, self)

        return True

    def _must_be_referenced_after(self, another):
        '''Validate that this definition is referenced after another one.'''
        return another._must_be_referenced_before(self)


class NativeType(Definition):
    '''Native type definition, i.e. it defines a native language type such as string, int, etc.'''
    def __init__(self, type0):
        super(NativeType, self).__init__(type0, type0)
        self.type = type0


class NativeTypes(object):
    '''Native types.'''
    BOOL = NativeType(Type.BOOL)
    INT16 = NativeType(Type.INT16)
    INT32 = NativeType(Type.INT32)
    INT64 = NativeType(Type.INT64)
    FLOAT = NativeType(Type.FLOAT)
    DOUBLE = NativeType(Type.DOUBLE)
    STRING = NativeType(Type.STRING)

    OBJECT = NativeType(Type.OBJECT)
    VOID = NativeType(Type.VOID)

    _BY_TYPE = None

    @classmethod
    def get_by_type(cls, t):
        '''Returns a value by its type or none.'''
        if cls._BY_TYPE is None:
            cls._BY_TYPE = {}
            for k, v in cls.__dict__.items():
                if not isinstance(v, NativeType):
                    continue
                cls._BY_TYPE[v.type] = v

        return cls._BY_TYPE.get(t)


class Enum(Definition):
    '''Enum definition.'''
    @classmethod
    def parse_node(cls, node, lookup):
        '''Creates an enum from an AST node.'''
        check_isinstance(node, ast.Enum)
        enum = Enum(node.name)
        for n in node.values:
            enum.add_value(n)
        return enum

    def __init__(self, name):
        super(Enum, self).__init__(Type.ENUM, name)
        self.values = []

    def add_value(self, name):
        '''Create a new enum value by its name, add it to this enum, and return it.'''
        value = EnumValue(self, name)
        self.values.append(value)
        return value

    def get_value(self, name):
        '''Get a value by its name or raise an exception.'''
        for value in self.values:
            if value.name == name:
                return value

    def __contains__(self, enum_value):
        return enum_value in self.values

    def _validate(self):
        names = set()
        for value in self.values:
            self._check(value.name not in names, 'Duplicate value, enum=%s, value=%s', self, value)
            names.add(value.name)


class EnumValue(Definition):
    '''Single enum value which has a name and a pointer to the declaring enum.'''
    def __init__(self, enum, name):
        super(EnumValue, self).__init__(Type.ENUM_VALUE, name)
        self.enum = enum
        self.name = name


class Message(Definition):
    '''User-defined message.'''
    @classmethod
    def parse_node(cls, node, lookup):
        '''Create a message from an AST node.'''
        check_isinstance(node, ast.Message)

        message = Message(node.name, is_exception=node.is_exception, doc=node.doc,
                          is_form=node.is_form)
        message.base = lookup(node.base) if node.base else None
        message.discriminator_value = lookup(node.discriminator_value) \
            if node.discriminator_value else None

        for n in node.fields:
            message.parse_field(n, lookup)
        return message

    def __init__(self, name, is_exception=False, doc=None, is_form=False):
        super(Message, self).__init__(Type.MESSAGE, name, doc=doc)
        self.is_exception = is_exception

        self.base = None
        self._discriminator = None       # Discriminator field, self.discriminator is a property.
        self.discriminator_value = None  # Enum value.
        self.subtypes = []

        self.is_form = is_form
        self.declared_fields = []

    @property
    def discriminator(self):
        '''Return this message discriminator field, base discriminator field, or None.'''
        if self._discriminator:
            return self._discriminator

        return self.base.discriminator if self.base else None

    @property
    def fields(self):
        if not self.base:
            return self.declared_fields

        return self.base.fields + self.declared_fields

    @property
    def inherited_fields(self):
        if not self.base:
            return []

        return self.base.fields

    def set_base(self, base, discriminator_value=None):
        '''Set this message base and polymorphic base type.'''
        self.base = base
        self.discriminator_value = discriminator_value
        self._debug('Set base, message=%s, base=%s, discriminator_value=%s',
                    self, base, discriminator_value)

    def add_field(self, field):
        '''Add a new field to this message and return the field.'''
        check_isinstance(field, Field)
        self._check(field.message is None, 'Field is already in a message, field=%s', field)

        self.declared_fields.append(field)
        field.message = self

        if field.is_discriminator:
            self._discriminator = field

        self._debug('Added a field, message=%s, field=%s', self, field.name)
        return field

    def create_field(self, name, definition, is_discriminator=False):
        '''Create a new field, add it to this message and return the field.'''
        field = Field(name, definition, is_discriminator=is_discriminator)
        return self.add_field(field)

    def parse_field(self, node, lookup):
        '''Parse an AST node, add a new field to this message, and return the field.'''
        field = Field.parse_node(node, lookup=lookup)
        return self.add_field(field)

    def _add_subtype(self, subtype):
        '''Add a new subtype to this message.'''
        check_isinstance(subtype, Message)
        if subtype is self:
            return

        self.subtypes.append(subtype)
        if self.base:
            self.base._add_subtype(subtype)

    def _validate(self):
        self._validate_base()
        self._validate_discriminator_value()
        self._validate_subtypes()
        self._validate_fields()

    def _validate_base(self):
        if not self.base:
            return

        base = self.base
        self._check(base.is_message, 'Base must be a message, message=%s', self)
        self._check(self.is_exception == base.is_exception,
                    'Wrong base type (message/exc), message=%s', self)
        self._must_be_referenced_after(base)

        # Check circular inheritance.
        while base:
            self._check(base is not self, 'Circular inheritance, message=%s', self)
            base = base.base

    def _validate_discriminator_value(self):
        base = self.base
        dvalue = self.discriminator_value

        if not dvalue:
            is_polymorphic = base and base.discriminator
            self._check(not is_polymorphic, 'Discriminator value required for %s base', self)
            return

        self._check(dvalue.is_enum_value,
                    'Discriminator value must be an enum value, message=%s', self)

        self._check(base.discriminator,
                    'Cannot set a discriminator value, '
                    'the base does not have a discriminator, message=%s', self)

        self._must_be_referenced_after(dvalue)

    def _validate_subtypes(self):
        if not self.subtypes:
            return

        dvalues = set()
        for subtype in self.subtypes:
            dvalue = subtype.discriminator_value

            # Check that the value is a discriminator enum instance.
            self._check(dvalue in self.discriminator.type,
                        'Wrong discriminator value type, message=%s, subtype=%s', self, subtype)

            self._check(dvalue not in dvalues,
                        'Duplicate discriminator value, message=%s, subtype=%s', self, dvalue)
            dvalues.add(dvalue)

    def _validate_fields(self):
        names = set()
        for field in self.fields:
            self._check(not field.name in names, 'Duplicate field, message=%s, field%s', self,
                        field.name)
            names.add(field.name)

        discriminator = None
        for field in self.fields:
            if field.is_discriminator:
                self._check(not discriminator, 'Multiple discriminator fields, message=%s', self)
                discriminator = field


class Field(Symbol):
    '''Single message field.'''
    @classmethod
    def parse_node(cls, node, lookup):
        '''Create a field from an AST node.'''
        check_isinstance(node, ast.Field)
        type0 = lookup(node.type)
        return Field(node.name, type0, is_discriminator=node.is_discriminator,
                     is_query=node.is_query)

    def __init__(self, name, type0, message=None, is_discriminator=False, is_query=False):
        self.name = name
        self.type = type0
        self.is_discriminator = is_discriminator
        self.is_query = is_query
        self.message = message

    @property
    def fullname(self):
        if not self.message:
            return self.name

        return '%s.%s' % (self.message.fullname, self.name)

    def _validate(self):
        self._check(self.type.is_datatype, 'Field must be a data type, field=%s', self)

        if self.is_discriminator:
            self._check(self.type.is_enum, 'Discriminator field must be an enum, field=%s', self)


class Interface(Definition):
    '''User-defined interface.'''
    @classmethod
    def parse_node(cls, node, lookup):
        check_isinstance(node, ast.Interface)
        iface = Interface(node.name, doc=node.doc)
        iface.base = lookup(node.base) if node.base else None
        iface.exc = lookup(node.exc) if node.exc else None

        for mnode in node.methods:
            iface.parse_method(mnode, lookup)

        return iface

    def __init__(self, name, base=None, exc=None, doc=None):
        super(Interface, self).__init__(Type.INTERFACE, name, doc=doc)
        self.base = base
        self.exc = exc
        self.declared_methods = []

    @property
    def methods(self):
        return self.inherited_methods + self.declared_methods

    @property
    def inherited_methods(self):
        if not self.base:
            return []
        return self.base.methods

    def set_base(self, base):
        '''Set the base of this interface.'''
        self.base = base
        self._debug('Set a base, interface=%s, base=%s', self, base)

    def add_method(self, method):
        '''Add a method to this interface.'''
        self._check(method.interface is None, 'Method is already in an interface, %s', method)

        method.interface = self
        self.declared_methods.append(method)
        self._debug('Added a method, interface=%s, method=%s', self, method)

    def create_method(self, name, result=NativeTypes.VOID, *args_tuples):
        '''Add a new method to this interface and return the method.'''
        method = Method(name, result)
        for arg_tuple in args_tuples:
            method.create_arg(*arg_tuple)

        self.add_method(method)
        return method

    def parse_method(self, node, lookup):
        '''Create a new method and add it to this interface.'''
        method = Method.parse_from(node, lookup)
        self.add_method(method)

    def _validate(self):
        self._validate_base()
        self._validate_exc()
        self._validate_methods()

    def _validate_base(self):
        if not self.base:
            return

        base = self.base
        self._check(base.is_interface, 'Base must be an interface, interface=%s', self)
        self._must_be_referenced_after(base)

        # Check circular inheritance.
        while base:
            self._check(base is not self, 'Circular inheritance, interface=%s', self)
            base = base.base

    def _validate_exc(self):
        if not self.exc:
            return
        self._check(self.exc.is_exception, 'Wrong exception, interface=%s', self)

    def _validate_methods(self):
        names = set()
        for method in self.methods:
            self._check(method.name not in names,
                        'Duplicate method, interface=%s, method=%s', self, method.name)
            names.add(method.name)

        for method in self.methods:
            method.validate()


class Method(Symbol):
    '''Interface method.'''
    @classmethod
    def parse_from(cls, node, lookup):
        check_isinstance(node, ast.Method)
        method = Method(node.name, result=lookup(node.result), doc=node.doc,
                        is_index=node.is_index, is_post=node.is_post)
        for n in node.args:
            method.parse_arg(n, lookup)
        return method

    def __init__(self, name, result, doc=None, is_index=False, is_post=False):
        self.name = name
        self.result = result
        self.interface = None

        self.doc = doc
        self.is_index = is_index
        self.is_post = is_post

        self.args = []

    def __str__(self):
        return self.fullname

    @property
    def fullname(self):
        if not self.interface:
            return self.name

        return '%s.%s' % (self.interface.fullname, self.name)

    @property
    def is_remote(self):
        return not self.result.is_interface

    def add_arg(self, arg):
        '''Append an argument to this method.'''
        arg.method = self
        self.args.append(arg)

    def create_arg(self, name, definition):
        '''Create a new arg and add it to this method.'''
        arg = MethodArg(name, definition)
        self.add_arg(arg)
        return arg

    def parse_arg(self, node, lookup):
        '''Create a new argument and add it to this method.'''
        arg = MethodArg.parse_from(node, lookup)
        return self.add_arg(arg)

    def _validate(self):
        for arg in self.args:
            arg.validate()

        if self.is_post:
            self._check(self.is_remote, 'Only remote methods can be @post, method=%s', self)

        # Check that all form args fields do not clash with method arguments.
        names = {arg.name for arg in self.args}
        for arg in self.args:
            type0 = arg.type
            if not type0.is_message or not type0.is_form:
                continue

            for field in type0.fields:
                self._check(field.name not in names,
                            'Form fields clash with method args, method=%s, form_arg=%s, field=%s',
                            self, arg, field.name)


class MethodArg(Symbol):
    '''Single method argument.'''
    @classmethod
    def parse_from(cls, node, lookup):
        return MethodArg(node.name, lookup(node.type))

    def __init__(self, name, definition):
        self.name = name
        self.type = definition
        self.is_query = False
        self.method = None

    @property
    def fullname(self):
        return '%s.%s' % (self.method, self.name)

    def _validate(self):
        self._check(self.type.is_datatype, 'Argument must be a data type, arg=%s', self)


class List(Definition):
    '''List definition.'''
    def __init__(self, element, module=None):
        super(List, self).__init__(Type.LIST, 'list')
        self.element = element
        self.module = module

    def _validate(self):
        self._check(self.element.is_datatype, 'List elements must be data types, %s', self)


class Set(Definition):
    '''Set definition.'''
    def __init__(self, element, module=None):
        super(Set, self).__init__(Type.SET, 'set')
        self.element = element
        self.module = module

    def _validate(self):
        self._check(self.element.is_datatype, 'Set elements must be data types, %s', self)


class Map(Definition):
    '''Map definition.'''
    def __init__(self, key, value, module=None):
        super(Map, self).__init__(Type.MAP, 'map')
        self.key = key
        self.value = value
        self.module = module

    def _validate(self):
        self._check(self.key.is_primitive, 'Map keys must be primitives, %s', self)
        self._check(self.value.is_datatype, 'Map values must be data types, %s', self)
