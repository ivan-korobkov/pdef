# encoding: utf-8
import pdef_lang


class Linker(object):
    def link(self, def0, module):
        if not def0.is_reference:
            return def0, []

        return self.find(def0, module)

    def find(self, node, module):
        def0, errors = self._find(node, module)
        if def0 and not errors:
            return def0, []

        return def0, ['%s: %s' % (node.location or 'nofile', e) for e in errors]

    def _find(self, node, module):
        if node is None:
            raise ValueError('Node is None')
        if module is None:
            raise ValueError('Module is None')

        if isinstance(node, pdef_lang.ListRe):
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
        return lang.List(element), errors

    def _find_set(self, node, module):
        element, errors = self.find(node.element, module)
        return lang.Set(element), errors

    def _find_map(self, node, module):
        key, errors0 = self.find(node.key, module)
        value, errors1 = self.find(node.value, module)

        return lang.Map(key, value), errors0 + errors1

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

            imported_module = module.get_imported_module(lname)
            if not imported_module:
                continue

            # Try to get a definition or an enum value from the imported module.
            def0 = self._get_definition_or_enum_value(rname, imported_module)
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
