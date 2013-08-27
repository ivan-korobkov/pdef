# encoding: utf-8
import os
import logging
from collections import OrderedDict

from pdef import Type
from pdef.compiler import ast, parser
from pdef.compiler.exc import PdefCompilerException
from pdef.compiler.preconditions import check_isinstance

EXT = 'pdef'


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

    def link(self):
        '''Links this symbol.'''
        if self.linked:
            return

        self.linked = True
        self._link()
        self._debug('Linked %s', self)

    def validate(self):
        '''Validates this symbol.'''
        if self.validated:
            return

        self._check(self.linked, 'Symbol must be linked before validation, %s', self)
        self.validated = True
        self._validate()
        self._debug('Validated %s', self)

    def _link(self):
        pass

    def _validate(self):
        pass

    def _check(self, expression, msg, *args):
        if expression:
            return
        self._error(msg, *args)

    def _error(self, msg, *args):
        msg = msg % args if msg else 'Error'
        raise PdefCompilerException(msg)

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
        self.modules.append(module)
        self._debug('Added a module %s', module)

    def parse_module(self, node):
        '''Parse a module from an AST node, add it to this package, and return the module.'''
        module = Module.parse_node(node, self)
        self.add_module(module)
        return module

    def parse_file(self, path):
        '''Parse a module from a file, add it to this package, and return the module.'''
        logging.info('Parsing %s', path)

        module = Module.parse_file(path, self)
        return self.add_module(module)

    def parse_directory(self, path):
        '''Recursively parse modules from a directory, and return a list of modules.'''
        logging.info('Walking %s' % path)

        modules = []
        for root, dirs, files in os.walk(path):
            for file0 in files:
                ext = os.path.splitext(file0)[1]
                if ext.lower() != '.' + EXT:
                    continue

                filepath = os.path.join(root, file0)
                module = self.parse_file(filepath)
                modules.append(module)

        return modules

    def parse_path(self, path):
        '''Parse modules from a file or a directory.'''
        if os.path.isdir(path):
            return self.parse_directory(path)
        return self.parse_file(path)

    def find_module_or_raise(self, name):
        '''Return a module by its name, or raise an exception.'''
        for module in self.modules:
            if module.name == name:
                return module

        self._error('Module "%s" is not found', name)

    def find_module_or_raise_lazy(self, name):
        '''Return a lambda which lookups a module by name.'''
        return lambda: self.find_module_or_raise(name)

    def _link(self):
        '''Link the package.'''
        for module in self.modules:
            module.link_imports()

        for module in self.modules:
            module.link()

    def _validate(self):
        '''Validate the package.'''
        for module in self.modules:
            module.validate()


class Module(Symbol):
    '''Module in a pdef package, usually, a module is parsed from one file.'''
    @classmethod
    def parse_file(cls, path, package):
        '''Parses a module from a file.'''
        node = parser.parse_file(path)
        return cls.parse_node(node, package)

    @classmethod
    def parse_node(cls, node, package):
        '''Parse a module from an AST node.'''
        module = Module(node.name, package)

        for import0 in node.imports:
            module.parse_import(import0, module_lookup=package.find_module_or_raise_lazy)

        for def0 in node.definitions:
            module.parse_definition(def0)

        return module

    def __init__(self, name, package=None):
        self.name = name
        self.package = package

        self.modules = []
        self.imports = []
        self.definitions = []

        self.imports_linked = False

    def add_import(self, import0):
        '''Add a module import to this module.'''
        check_isinstance(import0, Import)
        self.imports.append(import0)

    def parse_import(self, node, module_lookup):
        '''Parse an import and add it to this module.'''
        imports = Import.parse_list_from_node(node, module_lookup=module_lookup)
        for import0 in imports:
            self.add_import(import0)

    def create_import(self, name, module):
        '''Create an import and add it to this module.'''
        import0 = Import(name, module)
        self.add_import(import0)
        return import0

    def find_import(self, name):
        '''Find an import by its name.'''
        for import0 in self.imports:
            if import0.name == name:
                return import0

    def add_definition(self, def0):
        '''Add a new definition to this module.'''
        check_isinstance(def0, Definition)
        self._check(def0.module is None, 'Definition is already in a module, def=%s,', def0)

        def0.module = self
        self.definitions.append(def0)

        self._debug('Added a definition, module=%s, def=%s', self, def0.name)

    def add_definitions(self, *defs):
        '''Add definitions to this module.'''
        for def0 in defs:
            self.add_definition(def0)

    def parse_definition(self, node):
        '''Parse a definition and add it to this module.'''
        definition = Definition.parse_node(node, lookup=self.find_ref_or_raise_lazy)
        self.add_definition(definition)

    def find_definition(self, name):
        '''Return a definition or an enum value by its name, or None.'''
        def0 = None

        if '.' not in name:
            for d in self.definitions:
                if d.name == name:
                    def0 = d
                    break
        else:
            # It must be an enum value
            left, right = name.split('.', 1)

            enum = self.find_definition(left)
            if enum and enum.is_enum:
                return enum.find_value(right)

        return def0

    def find_ref_or_raise(self, ref):
        '''Look up a definition by an AST reference node and link it.'''
        check_isinstance(ref, ast.TypeRef)
        def0 = self._find_ref(ref)
        if not def0:
            self._error('Type is not found, module=%s, ref=%s', self, ref)

        def0.link()
        return def0

    def _find_ref(self, ref):
        def0 = NativeTypes.get_by_type(ref.type)
        if def0:
            return def0  # It's a simple value.

        t = ref.type
        if t == Type.LIST:
            element = self.find_ref_or_raise(ref.element)
            return List(element, module=self)

        elif t == Type.SET:
            element = self.find_ref_or_raise(ref.element)
            return Set(element, module=self)

        elif t == Type.MAP:
            key = self.find_ref_or_raise(ref.key)
            value = self.find_ref_or_raise(ref.value)
            return Map(key, value, module=self)

        elif t == Type.ENUM_VALUE:
            enum = self.find_ref_or_raise(ref.enum)
            value = enum.find_value(ref.value)
            return value

        # It must be an import or a user defined type.
        name = ref.name
        if '.' not in name:
            return self.find_definition(name)

        # It can be an enum value or an imported type (i.e. import.module.Enum.Value).
        left = []
        right = name.split('.')
        while right:
            left.append(right.pop(0))
            lname = '.'.join(left)
            rname = '.'.join(right)

            import0 = self.find_import(lname)
            if import0:
                return import0.module.find_definition(rname)

            def0 = self.find_definition(name)
            if def0:
                return def0

        return None

    def find_ref_or_raise_lazy(self, ref):
        '''Return a lambda for a lazy definition lookup.'''
        check_isinstance(ref, ast.TypeRef)
        return lambda: self.find_ref_or_raise(ref)

    def link_imports(self):
        '''Link imports, must be called before module.link().'''
        for import0 in self.imports:
            import0.link()

        self.imports_linked = True

    def _link(self):
        '''Link imports and definitions.'''
        self._check(self.imports_linked, 'Imports must be linked before the module, module%s', self)

        for def0 in self.definitions:
            def0.link()

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


class Import(Symbol):
    @classmethod
    def parse_list_from_node(cls, node, module_lookup):
        '''Parse a node and return a list of imports.'''
        check_isinstance(node, ast.Import)
        if isinstance(node, ast.AbsoluteImport):
            return [Import(node.name, module_lookup(node.name))]

        elif isinstance(node, ast.RelativeImport):
            prefix = node.prefix
            return [Import(name, module_lookup(prefix + '.' + name)) for name in node.names]

        else:
            raise ValueError('Unsupported import node %s' % node)

    def __init__(self, name, module):
        self.name = name
        self.module = module
        self.linked = False

    def _link(self):
        self.module = self.module() if callable(self.module) else self.module
        self._check(isinstance(self.module, Module), 'Import must be a module, import=%s', self)


class Definition(Symbol):
    '''Base definition.'''
    @classmethod
    def parse_node(cls, node, lookup):
        '''Create a definition from an AST node.'''
        if node.type == Type.ENUM:
            return Enum.parse_node(node, lookup)

        elif node.type == Type.MESSAGE:
            return Message.parse_node(node, lookup)

        elif node.type == Type.INTERFACE:
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

    def find_value(self, name):
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
        message.base_type = lookup(node.base_type) if node.base_type else None

        for n in node.fields:
            message.parse_field(n, lookup)
        return message

    def __init__(self, name, is_exception=False, doc=None, is_form=False):
        super(Message, self).__init__(Type.MESSAGE, name, doc=doc)
        self.is_exception = is_exception

        self.base = None
        self.base_type = None
        self.subtypes = OrderedDict()
        self.is_form = is_form
        self._discriminator = None

        self.declared_fields = []

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

    @property
    def discriminator(self):
        '''Return this message discriminator field, base discriminator field, or None.'''
        if self._discriminator:
            return self._discriminator

        return self.base.discriminator if self.base else None

    def set_base(self, base, base_type=None):
        '''Set this message base and polymorphic base type.'''
        self.base = base
        self.base_type = base_type
        self._debug('Set base, message=%s, base=%s, base_type=%s', self, base, base_type)

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
        '''Add a new subtype to this message, check its base_type.'''
        check_isinstance(subtype, Message)
        if subtype.base_type in self.subtypes:
            return

        self.subtypes[subtype.base_type] = subtype
        if self.base:
            self.base._add_subtype(subtype)

    def _link(self):
        '''Link the base and the fields.'''
        self.base = self.base() if callable(self.base) else self.base
        self.base_type = self.base_type() if callable(self.base_type) else self.base_type
        if self.base:
            self.base.link()

        if self.base_type:
            self.base._add_subtype(self)

        for field in self.declared_fields:
            field.link()

    def _validate(self):
        self._validate_base()
        self._validate_base_type()
        self._validate_subtypes()
        self._validate_fields()

    def _validate_base(self):
        if not self.base:
            return

        base = self.base
        self._check(base.is_message, 'Base must be a message, message=%s', self)
        self._check(self.is_exception == base.is_exception,
                    'Wrong base type (message/exc), message=%s', self)

        while base:
            self._check(base is not self, 'Circular inheritance, message=%s', self)
            base = base.base

    def _validate_base_type(self):
        base = self.base
        btype = self.base_type

        if not btype:
            is_polymorphic = base and base.discriminator
            self._check(not is_polymorphic, 'Polymorphic type required, message=%s', self)
            return

        self._check(btype.is_enum_value,
                    'Polymorphic type must be an enum value, message=%s', self)

        self._check(base.discriminator,
                    'Cannot set a polymorphic type, '
                    'the base does not have a discriminator, message=%s', self)

    def _validate_subtypes(self):
        if not self.subtypes:
            return

        base_types = set()
        for subtype in self.subtypes.values():
            self._check(subtype.base_type in self.discriminator.type,
                        'Wrong polymorphic type, message=%s, subtype=%s', self, subtype)

            self._check(subtype.base_type not in base_types,
                        'Duplicate subtype, message=%s, subtype=%s', self, subtype.base_type)
            base_types.add(subtype.base_type)

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

    def _link(self):
        self.type = self.type() if callable(self.type) else self.type

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
        self.declared_methods.append(method)
        self._debug('Added a method, interface=%s, method=%s', self, method)

    def create_method(self, name, result=NativeTypes.VOID, *args_tuples):
        '''Add a new method to this interface and return the method.'''
        method = Method(name, result, self)
        for arg_tuple in args_tuples:
            method.create_arg(*arg_tuple)

        self.add_method(method)
        return method

    def parse_method(self, node, lookup):
        '''Create a new method and add it to this interface.'''
        method = Method.parse_from(node, self, lookup)
        self.add_method(method)

    def _link(self):
        '''Link the base, the exception and the methods.'''
        self.base = self.base() if callable(self.base) else self.base
        if self.base:
            self.base.link()

        self.exc = self.exc() if callable(self.exc) else self.exc

        for method in self.declared_methods:
            method.link()

    def _validate(self):
        self._validate_base()
        self._validate_exc()
        self._validate_methods()

    def _validate_base(self):
        if not self.base:
            return

        base = self.base
        self._check(base.is_interface, 'Base must be an interface, interface=%s', self)

        while base:
            self._check(base is not self, 'Circular inheritance, interface=%s', self)
            base = base.base

    def _validate_exc(self):
        if not self.exc:
            return
        self._check(self.exc.is_exception, 'Wrong exception, interface=%s', self)

    def _validate_methods(self):
        for method in self.methods:
            method.validate()

        names = set()
        for method in self.methods:
            self._check(method.name not in names,
                        'Duplicate method, interface=%s, method=%s', self, method.name)
            names.add(method.name)


class Method(Symbol):
    '''Interface method.'''
    @classmethod
    def parse_from(cls, node, interface, lookup):
        check_isinstance(node, ast.Method)
        method = Method(node.name, result=lookup(node.result), interface=interface, doc=node.doc,
                        is_index=node.is_index, is_post=node.is_post)
        for n in node.args:
            method.parse_arg(n, lookup)
        return method

    def __init__(self, name, result, interface, doc=None, is_index=False, is_post=False):
        self.name = name
        self.result = result
        self.interface = interface
        self.doc = doc
        self.is_index = is_index
        self.is_post = is_post

        self.args = []

    def __str__(self):
        return self.fullname

    @property
    def fullname(self):
        return '%s.%s' % (self.interface.fullname, self.name)

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

    def _link(self):
        self.result = self.result() if callable(self.result) else self.result
        for arg in self.args:
            arg.link()

    def _validate(self):
        for arg in self.args:
            arg.validate()


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

    def _link(self):
        self.type = self.type() if callable(self.type) else self.type

    def _validate(self):
        self._check(self.type.is_datatype, 'Argument must be a data type, arg=%s', self)


class List(Definition):
    '''List definition.'''
    def __init__(self, element, module=None):
        super(List, self).__init__(Type.LIST, 'list')
        self.element = element
        self.module = module

    def _link(self):
        self.element = self.element() if callable(self.element) else self.element

    def _validate(self):
        self._check(self.element.is_datatype, 'List elements must be data types, %s', self)


class Set(Definition):
    '''Set definition.'''
    def __init__(self, element, module=None):
        super(Set, self).__init__(Type.SET, 'set')
        self.element = element
        self.module = module

    def _link(self):
        self.element = self.element() if callable(self.element) else self.element

    def _validate(self):
        self._check(self.element.is_datatype, 'Set elements must be data types, %s', self)


class Map(Definition):
    '''Map definition.'''
    def __init__(self, key, value, module=None):
        super(Map, self).__init__(Type.MAP, 'map')
        self.key = key
        self.value = value
        self.module = module

    def _link(self):
        self.key = self.key() if callable(self.key) else self.key
        self.value = self.value() if callable(self.value) else self.value

    def _validate(self):
        self._check(self.key.is_primitive, 'Map keys must be primitives, %s', self)
        self._check(self.value.is_datatype, 'Map values must be data types, %s', self)
