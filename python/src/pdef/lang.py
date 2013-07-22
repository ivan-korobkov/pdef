# encoding: utf-8
import logging
from collections import OrderedDict
from pdef import ast
from pdef.common import Type, PdefException
from pdef.preconditions import check_isinstance, check_state


class Symbol(object):
    '''Abstract base symbol which has a location and error checks.'''
    name = None
    location = None
    doc = None

    @property
    def fullname(self):
        return self.name

    def _check(self, expression, msg, *args):
        if expression: return

        msg = msg % args if msg else 'Error'
        if self.location:
            msg = '%s: %s' % (self.location, msg)
        raise PdefException(msg)


class Package(Symbol):
    '''Protocol definition.'''
    @classmethod
    def parse_nodes(cls, name, nodes):
        package = Package(name)
        map(package.parse_module, nodes)
        package.link()
        return package

    def __init__(self, name):
        self.name = name
        self.modules = SymbolTable(self, name='modules')

    def __str__(self):
        return self.name

    def add_module(self, module):
        '''Adds a module to this package.'''
        self.modules.add(module)
        logging.debug('%s: added a module "%s"', self, module)

    def parse_module(self, node):
        '''Parses a module from an AST node and adds it to this package.'''
        module = Module.parse_node(node, self)
        self.add_module(module)

    def get_module(self, name):
        '''Returns a module by its name, or raises and exception.'''
        module = self.modules.get(name)
        if not module:
            raise PdefException('%s: module %r is not found' % (self, name))
        return module

    def link(self):
        '''Links the package.'''
        for module in self.modules.values():
            module.link_imports()

        for module in self.modules.values():
            module.link_definitions()
        logging.debug('%s: linked', self)


class Module(Symbol):
    '''Module in a protocol definition.'''
    @classmethod
    def parse_node(cls, node, package):
        '''Creates a module from an AST node.'''
        module = Module(node.name, package)
        module.location = node.location

        map(module.parse_import, node.imports)
        map(module.parse_definition, node.definitions)

        return module

    def __init__(self, name, package):
        self.name = name
        self.package = package

        self.imports = SymbolTable(self, 'imports')
        self.definitions = SymbolTable(self, 'definitions')

    def __repr__(self):
        return '<%s %s>' % (self.__class__.__name__, self.name)

    def __str__(self):
        return self.name

    def add_import(self, definition):
        '''Adds an imported definition to this module.'''
        check_isinstance(definition, Definition)
        self.imports.add(definition)

    def parse_import(self, node):
        '''Parses an import and adds it to this module.'''
        pass

    def add_definition(self, definition):
        '''Adds a new definition to this module.'''
        check_isinstance(definition, Definition)
        self._check(definition.name not in self.imports,
                    '%s: definition %r clashes with an import' % (self, definition.name))

        self.definitions.add(definition)
        logging.debug('%s: added a definition "%s"', self, definition)

    def parse_definition(self, node):
        '''Parses a definition and adds it to this module.'''
        definition = Definition.parse_node(node, module=self, lookup=self.lookup)
        self.add_definition(definition)

    def get_definition(self, name):
        '''Returns a definition by its name, or raises an exception.'''
        def0 = self.definitions.get(name)
        if not def0: raise PdefException('%s: definitions %r is not found' % (self, name))
        return def0

    def lookup(self, ref):
        '''Returns a lazy definition lookup.'''
        check_isinstance(ref, ast.Ref)
        return lambda: self._lookup(ref)

    def _lookup(self, ref):
        def0 = NativeTypes.get_by_type(ref.type)
        if def0:
            return def0  # It's a simple value.

        t = ref.type
        if t == Type.LIST: return List(ref.element, module=self)
        elif t == Type.SET: return Set(ref.element, module=self)
        elif t == Type.MAP: return Map(ref.key, ref.value, module=self)
        elif t == Type.ENUM_VALUE:
            enum = self._lookup(ref.enum)
            return enum.get_value(ref.value)

        # It must be an import or a user defined type.
        name = ref.name
        if name in self.definitions:
            return self.definitions[name]
        raise PdefException('%s: type is not found, "%s"' % (self, ref))

    def link_imports(self):
        '''Links this method imports, must be called before link_definitions().'''
        for import0 in self.imports:
            import0.link()
        logging.debug('%s: linked imports', self)

    def link_definitions(self):
        '''Links this module definitions, must be called after link_imports().'''
        for def0 in self.definitions.values():
            def0.link()
        logging.debug('%s: linked definitions', self)


class Definition(Symbol):
    '''Base definition.'''
    @classmethod
    def parse_node(cls, node, module, lookup):
        '''Creates a definition from an AST node.'''
        if node.type == Type.ENUM: return Enum.parse_node(node, module, lookup)
        elif node.type == Type.MESSAGE: return Message.parse_node(node, module, lookup)
        elif node.type == Type.INTERFACE: return Interface.parse_node(node, module, lookup)

        raise ValueError('Unsupported definition node %s' % node)

    def __init__(self, type, name, doc=None):
        self.type = type
        self.name = name
        self.module = None
        self.doc = doc
        self._linked = False

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

    def link(self):
        if self._linked: return

        self._linked = True
        self._link()

    def _link(self):
        pass


class Enum(Definition):
    '''Enum definition.'''
    @classmethod
    def parse_node(cls, node, module, lookup):
        '''Creates an enum from an AST node.'''
        check_isinstance(node, ast.Enum)
        enum = Enum(node.name)
        map(enum.add_value, node.values)
        return enum

    def __init__(self, name):
        super(Enum, self).__init__(Type.ENUM, name)
        self.values = SymbolTable(self, 'values')

    def add_value(self, value_name):
        '''Creates a new enum value by its name, adds it to this enum, and returns it.'''
        value = EnumValue(self, value_name)
        self.values.add(value)
        return value

    def get_value(self, name):
        '''Gets a value by its name or raises an exception.'''
        value = self.values.get(name)
        if not value: raise PdefException('%s: value is not found, "%s"' % (self, name))
        return name

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
        '''Creates a message from an AST node.'''
        check_isinstance(node, ast.Message)

        message = Message(node.name, is_exception=node.is_exception, doc=node.doc)
        message.base = lookup(node.base) if node.base else None
        message.base_type = lookup(node.base_type) if node.base_type else None

        for fn in node.fields:
            message.parse_field(fn, lookup)
        return message

    def __init__(self, name, is_exception=False, doc=None):
        super(Message, self).__init__(Type.MESSAGE, name, doc=doc)
        self.is_exception = is_exception

        self.base = None
        self.base_type = None
        self.subtypes = OrderedDict()
        self._discriminator = None

        self.declared_fields = SymbolTable(self, 'declared_fields')

    @property
    def fields(self):
        return self.declared_fields + self.base.fields if self.base else self.declared_fields

    @property
    def inherited_fields(self):
        return self.base.fields if self.base else SymbolTable(self, 'inherited_fields')

    @property
    def discriminator(self):
        '''Returns this message discriminator field, base discriminator field, or None.'''
        if self._discriminator:
            return self._discriminator
        return self.base.discriminator if self.base else None

    def add_field(self, field):
        '''Adds a new field to this message and returns the field.'''
        check_isinstance(field, Field)
        self.declared_fields.add(field)

        if field.is_discriminator:
            self._check(not self.discriminator, '%s: duplicate discriminator', self)
            self._discriminator = field

        logging.debug('%s: added a field %s', self, field)
        return field

    def create_field(self, name, definition, is_discriminator=False):
        '''Adds a new field to this message and returns the field.'''
        field = Field(name, definition, self, is_discriminator)
        return self.add_field(field)

    def parse_field(self, node, lookup):
        '''Creates a new field from an AST node and adds it to this message.'''
        field = Field.parse_from(node, self, lookup=lookup)
        return self.add_field(field)

    def _link(self):
        '''Initializes this message from its AST node if present.'''
        self._link_base()
        self._link_fields()
        logging.debug('%s: linked', self)

    def _link_base(self):
        self.base = self.base() if callable(self.base) else self.base
        self.base_type = self.base_type() if callable(self.base_type) else self.base_type

        base = self.base
        base_type = self.base_type
        if base:
            check_isinstance(base, Message)
            self._check(self != base, '%s: cannot inherit itself', self)
            self._check(self not in base._bases, '%s: circular inheritance with %s', self, base)
            self._check(self.is_exception == base.is_exception, '%s: cannot inherit %s',
                        self, base.fullname)

        if base_type:
            check_isinstance(base_type, EnumValue)
            self._check(base.discriminator,
                        '%s: polymorphic inheritance of a non-polymorphic base %s', self, base)
            self.base_type = base_type
            self.base._add_subtype(self)
        else:
            self._check(not base.discriminator,
                        '%s: non-polymorphic inheritance of a polymorphic base %s', self, base)

    def _add_subtype(self, subtype):
        '''Adds a new subtype to this message, checks its base_type.'''
        check_isinstance(subtype, Message)
        self._check(self.discriminator, '%s: is not polymorphic, no discriminator field', self)
        self._check(subtype.base_type in self.discriminator.type,
                    '%s: wrong polymorphic enum value', self)
        self._check(subtype.base_type not in self.subtypes, '%s: duplicate subtype %s',
                    self, subtype.base_type)

        self.subtypes[subtype.base_type] = subtype
        if self.base and self.base.discriminator == self.discriminator:
            self.base._add_subtype(subtype)

    def _link_fields(self):
        for field in self.fields:
            field.link()
        # TODO: duplicate fields in inherited fields.

    @property
    def _bases(self):
        '''Internal, returns all this message bases.'''
        bases = []

        b = self
        while b.base:
            bases.append(b.base)
            b = b.base

        return bases


class Field(object):
    '''Single message field.'''
    @classmethod
    def parse_from(cls, node, message, lookup):
        '''Creates a field from an AST node.'''
        check_isinstance(node, ast.Field)
        return Field(node.name, type=lookup(node.type), message=message,
                     is_discriminator=node.is_discriminator)

    def __init__(self, name, type, message, is_discriminator=False):
        self.name = name
        self.type = type
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
        iface = Interface(node.name, doc=node.doc)
        iface.base = lookup(node.base) if node.base else None

        for mnode in node.methods:
            iface.parse_method(mnode, lookup)

        return iface

    def __init__(self, name, doc=None):
        super(Interface, self).__init__(Type.INTERFACE, name, doc=doc)
        self.base = None
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
        logging.debug('%s: set a base "%s"', self, base)

    def add_method(self, method):
        '''Adds a method to this interface.'''
        self.declared_methods.add(method)
        self.methods.add(method)
        logging.debug('%s: added a method "%s"', self, method)

    def create_method(self, name, result=NativeTypes.VOID, *args_tuples):
        '''Adds a new method to this interface and returns the method.'''
        method = Method(name, result, self, args_tuples)
        self.add_method(method)
        return method

    def parse_method(self, node, lookup):
        '''Creates a new method and adds it to this interface.'''
        method = Method.parse_from(node, self, lookup)
        self.add_method(method)

    def _link(self):
        '''Initializes this interface from its AST node if present.'''
        self._link_base()
        self._link_methods()
        logging.debug("%s: linked", self)

    def _link_base(self):
        self.base = self.base() if callable(self.base) else self.base
        base = self.base
        if base:
            check_isinstance(base, Interface)
            self._check(base is not self, '%s: self inheritance', self)
            self._check(self not in base._all_bases, '%s: circular inheritance with %s', self, base)

    def _link_methods(self):
        # TODO: check inherited methods duplicates
        for method in self.methods:
            method.link()
        pass

    @property
    def _all_bases(self):
        '''Internal, returns all bases including the ones from the inherited interfaces.'''
        bases = []
        b = self.base
        while b:
            bases.append(b)
            b = b.base
        return bases


class Method(object):
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
        return '%s(%s)=>%s' % (self.name, ', '.join(str(a) for a in self.args.values()), self.result)

    @property
    def fullname(self):
        return '%s.%s' % (self.interface.fullname, self)

    def add_arg(self, arg):
        '''Appends an argument to this method.'''
        self.args.add(arg)
        logging.debug('%s: added an arg %s', self, arg)

    def parse_arg(self, node, lookup):
        '''Creates a new argument and adds it to this method.'''
        arg = MethodArg.parse_from(node, self, lookup)
        self.add_arg(arg)

    def link(self):
        self.result = self.result() if callable(self.result) else self.result
        for arg in self.args: arg.link()
        logging.debug('%s: linked', self)


class MethodArg(object):
    '''Single method argument.'''
    @classmethod
    def parse_from(cls, node, method, lookup):
        return MethodArg(node.name, lookup(node.type))

    def __init__(self, name, definition):
        self.name = name
        self.type = definition
        check_isinstance(definition, Definition)

    def __repr__(self):
        return '%s %s' % (self.name, self.type)

    def link(self):
        self.type = self.type() if callable(self.type) else self.type
        self._check(self.type.is_datatype, '%s: must be a data type', self)


class NativeType(Definition):
    '''Native type definition, i.e. it defines a native language type such as string, int, etc.'''
    def __init__(self, type):
        super(NativeType, self).__init__(type, type)
        self.type = type


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
                if not isinstance(v, NativeType): continue
                cls._BY_TYPE[v.type] = v

        return cls._BY_TYPE.get(t)


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
        '''Adds an item by with item.name as the key.'''
        self[item.name] = item

    def __setitem__(self, key, value):
        check_state(key not in self, '%s.%s: duplicate %s', self.parent, self.name, key)
        super(SymbolTable, self).__setitem__(key, value)

    def __add__(self, other):
        table = SymbolTable()
        map(table.add, self.values())
        map(table.add, other.values())
        return table
