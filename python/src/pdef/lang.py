# encoding: utf-8
import os
import logging
from collections import OrderedDict

from pdef.compiler import ast, parser
from pdef.types import Type, PdefException
from pdef.compiler.preconditions import check_isinstance, check_state

EXT = 'pdef'


class Symbol(object):
    '''Abstract base symbol.'''
    name = None
    doc = None

    @property
    def fullname(self):
        return self.name

    def _check(self, expression, msg, *args):
        if expression:
            return

        msg = msg % args if msg else 'Error'
        raise PdefException(msg)

    def _debug(self, msg, *args):
        logging.debug('  ' + msg, *args)


class Package(Symbol):
    '''Protocol definition.'''
    def __init__(self):
        self.modules = SymbolTable(self, name='modules')

    def add_module(self, module):
        '''Add a module to this package.'''
        self.modules.add(module)
        self._debug('Added a module "%s"', module.name)

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

    def lookup_module(self, name):
        '''Return a module by its name, or raise an exception.'''
        module = self.modules.get(name)
        if not module:
            raise PdefException('Module "%s" is not found' % name)
        return module

    def lookup_module_lazy(self, name):
        '''Return a lambda which lookups a module by name.'''
        return lambda: self.lookup_module(name)

    def link(self):
        '''Link the package.'''
        for module in self.modules.values():
            module.link_imports()

        for module in self.modules.values():
            module.link_definitions()
        self._debug('Linked')


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
            module.parse_import(import0, module_lookup=package.lookup_module_lazy)

        for def0 in node.definitions:
            module.parse_definition(def0)

        return module

    def __init__(self, name, package=None):
        self.name = name
        self.package = package

        self.imports = SymbolTable(self, 'imports')
        self.definitions = SymbolTable(self, 'definitions')
        self.imports_linked = False
        self.definitions_linked = False

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.name)

    def __str__(self):
        return self.name

    @property
    def linked(self):
        return self.imports_linked and self.definitions_linked

    def add_import(self, import0):
        '''Add a module import to this module.'''
        check_isinstance(import0, Import)
        self.imports.add(import0)

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

    def add_definition(self, def0):
        '''Add a new definition to this module.'''
        check_isinstance(def0, Definition)
        self._check(def0.module is self or def0.module is None,
                    '%s: cannot add a definition from another module, "%s" in %s',
                    self, def0.name, def0.module)

        name = def0.name
        for imp_name in self.imports:
            self._check(imp_name != name and imp_name.startswith(name + '.'),
                        '%s: definition clashes with an import, "%s", "%s"', self, name, imp_name)

        self.definitions.add(def0)
        def0.module = self
        self._debug('%s: added a definition "%s"', self, def0)

    def add_definitions(self, *defs):
        '''Add definitions to this module.'''
        for def0 in defs:
            self.add_definition(def0)

    def parse_definition(self, node):
        '''Parse a definition and add it to this module.'''
        definition = Definition.parse_node(node, module=self, lookup=self.lookup_lazy)
        self.add_definition(definition)

    def get_definition(self, name):
        '''Return a definition or an enum value by its name, or raise an exception.'''
        def0 = None

        if '.' not in name:
            def0 = self.definitions.get(name)
        else:
            # It must be an enum value
            left, right = name.split('.', 1)

            enum = self.get_definition(left)
            if enum.is_enum:
                return enum.get_value(right)

        if not def0:
            raise PdefException('%s: type is not found, "%s"' % (self, name))

        return def0

    def lookup(self, ref):
        '''Look up a definition by an AST reference node and link it.'''
        check_isinstance(ref, ast.TypeRef)
        def0 = self._lookup(ref)
        def0.link()
        return def0

    def _lookup(self, ref):
        def0 = NativeTypes.get_by_type(ref.type)
        if def0:
            return def0  # It's a simple value.

        t = ref.type
        if t == Type.LIST:
            element = self.lookup(ref.element)
            return List(element, module=self)

        elif t == Type.SET:
            element = self.lookup(ref.element)
            return Set(element, module=self)

        elif t == Type.MAP:
            key = self.lookup(ref.key)
            value = self.lookup(ref.value)
            return Map(key, value, module=self)

        elif t == Type.ENUM_VALUE:
            enum = self.lookup(ref.enum)
            value = enum.get_value(ref.value)
            return value

        # It must be an import or a user defined type.
        name = ref.name
        if '.' not in name:
            if name not in self.definitions:
                raise PdefException('%s: type is not found, "%s"' % (self, ref))
            return self.definitions[name]

        # It can be an enum value or an imported type (i.e. import.module.Enum.Value).
        left = []
        right = name.split('.')
        while right:
            left.append(right.pop(0))
            lname = '.'.join(left)
            rname = '.'.join(right)

            if lname in self.imports:
                import0 = self.imports[lname]
                return import0.module.get_definition(rname)

            if lname in self.definitions:
                return self.get_definition(name)

        raise PdefException('%s: type is not found, "%s"' % (self, ref))


    def lookup_lazy(self, ref):
        '''Return a lambda for a lazy definition lookup.'''
        check_isinstance(ref, ast.TypeRef)
        return lambda: self.lookup(ref)

    def link_imports(self):
        '''Link this method imports, must be called before link_definitions().'''
        self._check(not self.imports_linked, '%s: imports are already linked', self)

        for import0 in self.imports.values():
            import0.link()

        self.imports_linked = True
        self._debug('%s: linked imports', self)

    def link_definitions(self):
        '''Link this module definitions, must be called after link_imports().'''
        self._check(self.imports_linked, '%s: imports must be linked', self)
        self._check(not self.definitions_linked, '%s: definitions are already linked', self)

        for def0 in self.definitions.values():
            def0.link()

        self.definitions_linked = True
        self._debug('%s: linked definitions', self)


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

    def link(self):
        if self.linked:
            return

        self.module = self.module() if callable(self.module) else self.module
        self._check(isinstance(self.module, Module), '%s: must be a module, %s',
                    self, self.module)


class Definition(Symbol):
    '''Base definition.'''
    @classmethod
    def parse_node(cls, node, module, lookup):
        '''Create a definition from an AST node.'''
        if node.type == Type.ENUM:
            return Enum.parse_node(node, module, lookup)

        elif node.type == Type.MESSAGE:
            return Message.parse_node(node, module, lookup)

        elif node.type == Type.INTERFACE:
            return Interface.parse_node(node, module, lookup)

        raise ValueError('Unsupported definition node %s' % node)

    is_exception = False # The flag is set in a message constructor.

    def __init__(self, type0, name, module=None, doc=None):
        self.type = type0
        self.name = name
        self.module = module
        self.doc = doc
        self.linked = False

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.fullname)

    def __str__(self):
        return self.fullname

    @property
    def fullname(self):
        return '%s.%s' % (self.module.name, self.name) if self.module else self.name

    @property
    def is_primitive(self):
        return self.type in Type.PRIMITIVES

    @property
    def is_datatype(self):
        return self.type in Type.DATATYPES

    @property
    def is_interface(self):
        return self.type == Type.INTERFACE

    @property
    def is_message(self):
        return self.type == Type.MESSAGE

    @property
    def is_enum(self):
        return self.type == Type.ENUM

    @property
    def is_enum_value(self):
        return self.type == Type.ENUM_VALUE

    def link(self):
        if self.linked:
            return

        self.linked = True
        self._link()

    def _link(self):
        pass


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
    DECIMAL = NativeType(Type.DECIMAL)
    DATE = NativeType(Type.DATE)
    DATETIME = NativeType(Type.DATETIME)
    STRING = NativeType(Type.STRING)
    UUID = NativeType(Type.UUID)

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
    def parse_node(cls, node, module, lookup):
        '''Creates an enum from an AST node.'''
        check_isinstance(node, ast.Enum)
        enum = Enum(node.name)
        for n in node.values:
            enum.add_value(n)
        return enum

    def __init__(self, name):
        super(Enum, self).__init__(Type.ENUM, name)
        self.values = SymbolTable(self, 'values')

    def add_value(self, name):
        '''Create a new enum value by its name, add it to this enum, and return it.'''
        value = EnumValue(self, name)
        self.values.add(value)
        return value

    def get_value(self, name):
        '''Get a value by its name or raise an exception.'''
        value = self.values.get(name)
        if not value:
            raise PdefException('%s: value is not found, "%s"' % (self, name))
        return value

    def __contains__(self, item):
        return item in self.values.values()


class EnumValue(Definition):
    '''Single enum value which has a name and a pointer to the declaring enum.'''
    def __init__(self, enum, name):
        super(EnumValue, self).__init__(Type.ENUM_VALUE, name)
        self.enum = enum
        self.name = name


class Message(Definition):
    '''User-defined message.'''
    @classmethod
    def parse_node(cls, node, module, lookup):
        '''Create a message from an AST node.'''
        check_isinstance(node, ast.Message)

        message = Message(node.name, is_exception=node.is_exception, module=module, doc=node.doc)
        message.base = lookup(node.base) if node.base else None
        message.base_type = lookup(node.base_type) if node.base_type else None

        for n in node.fields:
            message.parse_field(n, lookup)
        return message

    def __init__(self, name, is_exception=False, module=None, doc=None):
        super(Message, self).__init__(Type.MESSAGE, name, module=module, doc=doc)
        self.is_exception = is_exception

        self.base = None
        self.base_type = None
        self.subtypes = OrderedDict()
        self._discriminator = None

        self.declared_fields = SymbolTable(self, 'declared_fields')

    @property
    def fields(self):
        if not self.base:
            return self.declared_fields

        return self.declared_fields + self.base.fields

    @property
    def inherited_fields(self):
        if not self.base:
            return SymbolTable('fields')

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
        self._debug('%s: set base to %s, base_type=%s', self, base, base_type)

    def add_field(self, field):
        '''Add a new field to this message and return the field.'''
        check_isinstance(field, Field)
        self._check(field.message is self or field.message is None,
                    '%s: cannot add a field from another message, "%s" from %s',
                    self, field.name, field.message)

        self.declared_fields.add(field)
        field.message = self

        if field.is_discriminator:
            self._check(not self._discriminator, '%s: duplicate discriminator', self)
            self._discriminator = field

        self._debug('%s: added a field "%s"', self, field.name)
        return field

    def create_field(self, name, definition, is_discriminator=False):
        '''Create a new field, add it to this message and return the field.'''
        field = Field(name, definition, self, is_discriminator)
        return self.add_field(field)

    def parse_field(self, node, lookup):
        '''Parse an AST node, add a new field to this message, and return the field.'''
        field = Field.parse_node(node, message=self, lookup=lookup)
        return self.add_field(field)

    def _add_subtype(self, subtype):
        '''Add a new subtype to this message, check its base_type.'''
        check_isinstance(subtype, Message)
        self._check(self.discriminator, '%s: is not polymorphic, no discriminator field', self)
        self._check(subtype.base_type in self.discriminator.type,
                    '%s: wrong polymorphic enum value', self)
        self._check(subtype.base_type not in self.subtypes, '%s: duplicate subtype %s',
                    self, subtype.base_type)

        self.subtypes[subtype.base_type] = subtype
        if self.base and self.base.discriminator == self.discriminator:
            self.base._add_subtype(subtype)

    def _link(self):
        '''Link the base and the fields.'''
        self._link_base()
        self._link_fields()
        self._debug('%s: linked', self)

    def _link_base(self):
        self.base = self.base() if callable(self.base) else self.base
        self.base_type = self.base_type() if callable(self.base_type) else self.base_type

        base = self.base
        base_type = self.base_type
        if base:
            self._check(base.is_message, '%s: base must be a message, %s', self, base)
            self._check(self != base, '%s: cannot inherit itself', self)
            self._check(self.is_exception == base.is_exception, '%s: cannot inherit %s',
                        self, base)
            self._check_circular_inheritance()
            base.link()

        if base_type:
            self._check(base_type.is_enum_value, '%s: base type must be an enum value, %s',
                        self, base_type)
            self._check(base.discriminator, '%s: base does not have a discriminator, base=%s',
                        self, base)
            self.base._add_subtype(self)
        else:
            self._check(not base or not base.discriminator,
                        '%s: no enum value for a base discriminator, base=%s', self, base)

    def _link_fields(self):
        for field in self.declared_fields.values():
            field.link()

        inherited_fields = self.inherited_fields
        for field in self.declared_fields.values():
            self._check(field.name not in inherited_fields, '%s: duplicate field %s', self, field)

    def _check_circular_inheritance(self):
        b = self.base
        while b:
            self._check(b is not self, '%s: circular inheritance with %s', self, b)
            b = b.base


class Field(Symbol):
    '''Single message field.'''
    @classmethod
    def parse_node(cls, node, message, lookup):
        '''Create a field from an AST node.'''
        check_isinstance(node, ast.Field)
        type0 = lookup(node.type)
        return Field(node.name, type0, message=message, is_discriminator=node.is_discriminator)

    def __init__(self, name, type0, message, is_discriminator=False):
        self.name = name
        self.type = type0
        self.message = message
        self.is_discriminator = is_discriminator

    def __repr__(self):
        return '%s %s' % (self.name, self.type)

    @property
    def fullname(self):
        return '%s.%s=%s' % (self.message.fullname, self.name, self.type)

    def link(self):
        self.type = self.type() if callable(self.type) else self.type
        self._check(self.type.is_datatype, '%s: field must be a data type, %s', self, self.type)

        if self.is_discriminator:
            self._check(self.type.is_enum, '%s: discriminator must be an enum', self)


class Interface(Definition):
    '''User-defined interface.'''
    @classmethod
    def parse_node(cls, node, module, lookup):
        check_isinstance(node, ast.Interface)
        iface = Interface(node.name, module=module, doc=node.doc)
        iface.base = lookup(node.base) if node.base else None
        iface.exc = lookup(node.exc) if node.exc else None

        for mnode in node.methods:
            iface.parse_method(mnode, lookup)

        return iface

    def __init__(self, name, base=None, exc=None, module=None, doc=None):
        super(Interface, self).__init__(Type.INTERFACE, name, module=module, doc=doc)
        self.base = base
        self.exc = exc
        self.declared_methods = SymbolTable(self, 'declared_methods')

    @property
    def methods(self):
        return self.inherited_methods + self.declared_methods

    @property
    def inherited_methods(self):
        if not self.base:
            return SymbolTable(self, 'methods')
        return self.base.methods

    def set_base(self, base):
        '''Set the base of this interface.'''
        self.base = base
        self._debug('%s: set a base "%s"', self, base)

    def add_method(self, method):
        '''Add a method to this interface.'''
        self.declared_methods.add(method)
        self._debug('%s: added a method "%s"', self, method.name)

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
        self._link_base()
        self._link_exc()
        self._link_methods()
        self._debug("%s: linked", self)

    def _link_base(self):
        self.base = self.base() if callable(self.base) else self.base
        if not self.base:
            return

        base = self.base
        base.link()
        self._check(base.is_interface, '%s: base must be an interface, %s', self, base)
        self._check(base is not self, '%s: self inheritance', self)
        self._check_circular_inheritance()

    def _link_exc(self):
        if not self.exc:
            return

        self.exc = self.exc() if callable(self.exc) else self.exc
        self._check(self.exc.is_exception, '%s: tries to throw a non-exception, %s', self, self.exc)

    def _link_methods(self):
        for method in self.declared_methods.values():
            method.link()

        if not self.base:
            return

        base_methods = self.base.methods
        for m in self.declared_methods.values():
            self._check(m.name not in base_methods, '%s: duplicate base method %s', self, m)

    def _check_circular_inheritance(self):
        b = self.base
        while b:
            self._check(b is not self, '%s: circular inheritance with %s', self, b)
            b = b.base


class Method(Symbol):
    '''Interface method.'''
    @classmethod
    def parse_from(cls, node, interface, lookup):
        check_isinstance(node, ast.Method)
        method = Method(node.name, result=lookup(node.result), interface=interface, doc=node.doc)
        for n in node.args:
            method.parse_arg(n, lookup)
        return method

    def __init__(self, name, result, interface, doc=None):
        self.name = name
        self.result = result
        self.interface = interface
        self.doc = doc
        self.args = SymbolTable(self, 'args')

    def __str__(self):
        return self.fullname

    @property
    def fullname(self):
        return '%s.%s' % (self.interface.fullname, self.name)

    def add_arg(self, arg):
        '''Append an argument to this method.'''
        self.args.add(arg)

    def create_arg(self, name, definition):
        '''Create a new arg and add it to this method.'''
        arg = MethodArg(name, definition)
        self.add_arg(arg)
        return arg

    def parse_arg(self, node, lookup):
        '''Create a new argument and add it to this method.'''
        arg = MethodArg.parse_from(node, lookup)
        return self.add_arg(arg)

    def link(self):
        self.result = self.result() if callable(self.result) else self.result
        for arg in self.args.values():
            arg.link()
        self._debug('%s: linked', self)


class MethodArg(Symbol):
    '''Single method argument.'''
    @classmethod
    def parse_from(cls, node, lookup):
        return MethodArg(node.name, lookup(node.type))

    def __init__(self, name, definition):
        self.name = name
        self.type = definition

    def __repr__(self):
        return self.name

    def link(self):
        self.type = self.type() if callable(self.type) else self.type
        self._check(self.type.is_datatype, '%s: must be a data type', self)


class List(Definition):
    '''List definition.'''
    def __init__(self, element, module=None):
        super(List, self).__init__(Type.LIST, 'list')
        self.element = element
        self.module = module

    def _link(self):
        self.element = self.element() if callable(self.element) else self.element
        self._check(self.element.is_datatype, '%s: element must be a data type, %s',
                    self, self.element)


class Set(Definition):
    '''Set definition.'''
    def __init__(self, element, module=None):
        super(Set, self).__init__(Type.SET, 'set')
        self.element = element
        self.module = module

    def _link(self):
        self.element = self.element() if callable(self.element) else self.element
        self._check(self.element.is_datatype, '%s: element must be a data type, %s',
                    self, self.element)


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

        self._check(self.key.is_primitive, '%s: key must be a primitive, %s', self, self.key)
        self._check(self.value.is_datatype, '%s: value must be a data type, %s', self, self.value)


class SymbolTable(OrderedDict):
    '''SymbolTable is an ordered dict which supports adding items using item.name as a key,
    and prevents duplicate items.'''
    def __init__(self, parent=None, name='items', *args, **kwds):
        super(SymbolTable, self).__init__(*args, **kwds)
        self.parent = parent
        self.name = name

    def add(self, item):
        '''Add an item with item.name as a key.'''
        self[item.name] = item

    def __setitem__(self, key, value, PREV=0, NEXT=1, dict_setitem=dict.__setitem__):
        check_state(key not in self, '%s.%s: duplicate %s', self.parent, self.name, key)
        super(SymbolTable, self).__setitem__(key, value)

    def __add__(self, other):
        table = SymbolTable()
        map(table.add, self.values())
        map(table.add, other.values())
        return table
