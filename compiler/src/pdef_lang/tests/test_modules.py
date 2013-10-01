# encoding: utf-8


class TestModule(unittest.TestCase):
    def test_add_import(self):
        '''Should add a new import to a module.'''
        import0 = Import('imported', Module('imported'))
        module = Module('module')
        module.add_import(import0)

        assert module.get_import('imported')

    def test_add_definition(self):
        '''Should add a new definition to a module.'''
        def0 = Definition(Type.REFERENCE, 'Test')

        module = Module('test')
        module.add_definition(def0)
        assert module.get_definition('Test')

    def test_get_definition(self):
        '''Should return a definition by its name.'''
        def0 = Definition(Type.REFERENCE, 'Test')

        module = Module('test')
        module.add_definition(def0)

        assert def0 is module.get_definition('Test')

    def test_link_imports(self):
        '''Should link module imports.'''
        module0 = Module('module0')

        module1 = Module('module1')
        module1.add_import(Import('module0', lambda: module0))
        module1.link_imports()

        assert module1.imports_linked
        assert module1.find_import('module0').module is module0


class TestImport(unittest.TestCase):
    def test_parse_list_from_node__absolute(self):
        node = ast.AbsoluteImport('absolute.module')
        lookup = mock.Mock()
        imports = Import.parse_list_from_node(node, lookup)

        assert len(imports) == 1
        assert imports[0].name == 'absolute.module'
        lookup.assert_called_with('absolute.module')

    def test_parse_list_from_node__relative(self):
        node = ast.RelativeImport('absolute', 'module0', 'module1')
        lookup = mock.Mock()
        imports = Import.parse_list_from_node(node, lookup)

        assert len(imports) == 2
        assert imports[0].name == 'module0'
        assert imports[1].name == 'module1'
        assert lookup.call_args_list[0] == (('absolute.module0', ), {})
        assert lookup.call_args_list[1] == (('absolute.module1', ), {})
