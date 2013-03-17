# encoding: utf-8
from pdef import ast, lang
from pdef.preconditions import *


class PackagesTranslator(object):
    def __init__(self, pdef, nodes):
        self.pdef = check_not_none(pdef)
        self.nodes = nodes

        self.packages = []
        for node in self.nodes:
            translator = PackageTranslator(pdef, node)
            self.packages.append(translator)

        for package in self.packages:
            package.link_imports()


class PackageTranslator(object):
    def __init__(self, pdef, node):
        super(PackageTranslator, self).__init__()
        check_isinstance(node, ast.Package)

        self.pdef = pdef
        self.node = node

        self.package = lang.Package(node.name, version=node.version)
        self.pdef.add_package(self.package)

        for module_node in self.node.modules:
            translator = ModuleTranslator(self.package, module_node)
            self.children.append(translator)


class ModuleTranslator(object):
    def __init__(self, package, node):
        super(ModuleTranslator, self).__init__()
        check_isinstance(node, ast.Module)

        self.package = package
        self.node = node
        self.module = lang.Module(node.name, package=package)

        for def_node in self.node.definitions:
            if isinstance(def_node, lang.Message):
                translator = MessageTranslator(self.module, def_node)
            elif isinstance(node, lang.Enum):
                translator = EnumTranslator(self.module, def_node)
            elif isinstance(node, lang.Native):
                translator = NativeTranslator(self.module, def_node)
            else:
                raise ValueError('Unsupported definition node %s' % node)
            self.children.append(translator)

    def link_imports(self):
        package = self.package
        for node in self.node.imports:
            if not node.import_name in package.modules:
                raise ValueError('Import not found "%s"' % node.import_name)

            imported = package.modules[node.import_name]
            self.module.add_import(imported)


class MessageTranslator(object):
    def __init__(self, module, node):
        super(MessageTranslator, self).__init__()
        check_isinstance(node, ast.Message)

        self.module = module
        self.node = node

        self.message = lang.Message(node.name,
                variables=(lang.Variable(var) for var in node.variables),
                module=module)

    def init(self):
        node = self.node
        message = self.message

        tree_type = self.link(node.tree_type) if node.tree_type else None
        tree_field = node.tree_field

        base = self.link(node.base) if node.base else None
        base_tree_type = self.link(node.base_tree_type) if node.base_tree_type else None

        declared_fields = []
        for field_node in node.declared_fields:
            name = field_node.name
            type = self.link(field_node.type)
            field = lang.Field(name, type)
            declared_fields.append(field)

        message.init(tree_type=tree_type, tree_field=tree_field,
                     base=base, base_tree_type=base_tree_type,
                     declared_fields=declared_fields)


class EnumTranslator(object):
    def __init__(self, module, node):
        super(EnumTranslator, self).__init__()
        check_isinstance(node, ast.Enum)

        self.module = module
        self.node = node

        self.enum = lang.Enum(node.name, module=module)
        for name in node.values:
            self.enum.add_value(lang.EnumValue(name, self.enum))


class NativeTranslator(object):
    def __init__(self, module, node):
        super(NativeTranslator, self).__init__()
        check_isinstance(node, ast.Native)

        self.module = module
        self.node = node

        self.native = lang.Native(node.name,
                variables=(lang.Variable(var) for var in node.variables),
                module=module)
