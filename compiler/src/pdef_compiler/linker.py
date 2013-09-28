# encoding: utf-8
from pdef_compiler import ast, lang


class Lookup(object):
    def find(self, node, module):
        if node is None:
            raise ValueError('Node is None')
        if module is None:
            raise ValueError('Module is None')

        if isinstance(node, ast.ValueRef):
            return self._find_value(node)

        elif isinstance(node, ast.ListRef):
            return self._find_list(node, module)

        elif isinstance(node, ast.SetRef):
            return self._find_set(node, module)

        elif isinstance(node, ast.MapRef):
            return self._find_map(node, module)

        # It must be a definition, an enum value or an imported type.
        # (i.e. import.module.Enum.Value).
        return self._find_definition(node, module)

    def _find_value(self, node):
        type0 = lang.NativeTypes.get_by_type(node.type)
        if type0:
            return type0, []

        return None, [node.type]

    def _find_list(self, node, module):
        element, errors = self.find(node.element, module)
        return lang.List(element, module=module), errors

    def _find_set(self, node, module):
        element, errors = self.find(node.element, module)
        return lang.Set(element, module=module), errors

    def _find_map(self, node, module):
        key, errors0 = self.find(node.key, module)
        value, errors1 = self.find(node.value, module)

        return lang.Map(key, value, module=module), errors0 + errors1

    def _find_definition(self, node, module):
        name = node.name

        # Try to find a definition in the current module.
        def0 = self._get_definition_or_enum_value(name, module)
        if def0:
            return def0, []

        # It must be an imported definition.
        left = []
        right = name.split('.')
        while right:
            left.append(right.pop(0))
            lname = '.'.join(left)
            rname = '.'.join(right)

            import0 = module.get_import(lname)
            if not import0:
                continue

            # Try to get a definition or an enum value from the imported module.
            def0 = self._get_definition_or_enum_value(rname, import0.module)
            if def0:
                return def0, []

            # Still can have more imports to check, i.e.:
            # import com.project
            # import com.project.submodule

        return None, [name]

    def _get_definition_or_enum_value(self, name, module):
        '''Get a definition or an enum value by a name.

        @return A definition or an enum value or None.
        '''
        if '.' not in name:
            # It must be a user-defined type.
            return module.get_definition(name)

        # It can be an enum value.
        left, right = name.split('.', 1)

        enum = self._get_definition_or_enum_value(left, module)
        if not enum or not enum.is_enum:
            return None

        return enum.get_value(right)


class Linker(object):
    def __init__(self):
        self.lookup = Lookup()

    def link_package(self, package):
        errors = []

        for module in package.modules:
            errors += self._link_module_imports(module)

        for module in package.modules:
            errors += self._link_module_defs(module)

        package.linked = True

    # Modules.

    def _link_module_imports(self, module):
        '''Link imports, must be called before link_module_defs().'''
        errors = []

        for import0 in module.imports:
            errors += self._link_import(import0)

        module.imports_linked = True
        return errors

    def _link_import(self, import0):
        import0.module, errors = self._link_module_ref(import0.module)
        # self._check(isinstance(self.module, Module), 'Import must be a module, import=%s', self)
        return errors

    def _link_module_ref(self, ref):
        return ref, []

    def _link_module_defs(self, module):
        '''Link imports and definitions.'''
        errors = []
        if not module.imports_linked:
            raise ValueError('Imports must be linked before the module, module=%s' % module)

        for def0 in module.definitions:
            errors += self._link_def(def0)

        module.linked = True
        return errors

    # Definitions.

    def _link_def(self, def0):
        if def0.linked:
            return []

        if def0.is_message:
            return self._link_message(def0)
        elif def0.is_interface:
            return self._link_interface(def0)
        elif def0.is_list:
            return self._link_list(def0)
        elif def0.is_set:
            return self._link_set(def0)
        elif def0.is_map:
            return self._link_map(def0)

        # All other definitions do not require linking.
        def0.linked = True
        return []

    # Messages and fields.

    def _link_message(self, message):
        errors = []

        message.base, errors0 = self._link_ref(message.base)
        errors += errors0

        message.discriminator_value, errors0 = self._link_ref(message.discriminator_value)
        errors += errors0

        if message.base:
            errors += self._link_message(message.base)

        if message.discriminator_value:
            message.base._add_subtype(message)

        for field in message.declared_fields:
            errors += self._link_field(field)

        return errors

    def _link_field(self, field):
        field.type, errors = self._link_ref(field.type)
        return errors

    # Interfaces and methods.

    def _link_interface(self, interface):
        '''Link the base, the exception and the methods.'''
        errors = []

        interface.base, errors0 = self._link_ref(interface.base)
        errors += errors0

        if interface.base:
            self._link_def(interface.base)

        interface.exc, errors0 = self._link_ref(interface.exc)
        errors += errors0

        for method in interface.declared_methods:
            errors += self._link_method(method)

    def _link_method(self, method):
        errors = []

        method.result, errors0 = self._link_ref(method.result)
        errors += errors0

        for arg in method.args:
            arg.type, errors0 = self._link_ref(arg.type)
            errors += errors0

        return errors

    # Collections.

    def _link_list(self, list0):
        list0.element, errors = self._link_ref(list0.element)
        return errors

    def _link_set(self, set0):
        set0.element, errors = self._link_ref(set0.element)
        return errors

    def _link_map(self, map0):
        errors = []

        map0.key, errors0 = self._link_ref(map0)
        errors += errors0

        map0.value, errors0 = self._link_ref(map0)
        errors += errors0

        return errors

    def _link_ref(self, ref):
        if not isinstance(ref, lang.Reference):
            return ref, []

        return self.lookup.find(ref.node, ref.module)
