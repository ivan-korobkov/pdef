# encoding: utf-8



class TestInterface(unittest.TestCase):
    def test_parse_node(self):
        '''Should create an interface from an AST node.'''
        base_ref = ast.DefRef('Base')
        exc_ref = ast.DefRef('Exc')

        node = ast.Interface('Iface', base=base_ref, exc=exc_ref,
                             methods=[ast.Method('echo', args=[ast.Field('text', ast.ValueRef(Type.STRING))],
                                                 result=ast.ValueRef(Type.STRING))])
        lookup = mock.Mock()

        iface = Interface.parse_node(node, lookup)
        assert iface.name == node.name
        assert iface.base
        assert iface.exc
        assert len(iface.declared_methods) == 1
        assert iface.declared_methods[0].name == 'echo'
        assert len(lookup.call_args_list) == 4

    def test_methods(self):
        '''Should combine the inherited and declared methods.'''
        iface0 = Interface('Iface0')
        iface1 = Interface('Iface1')
        iface1.set_base(iface0)

        method0 = iface0.create_method('method0')
        method1 = iface1.create_method('method1')

        assert iface1.inherited_methods == [method0]
        assert iface1.methods == [method0, method1]

    def test_create_method(self):
        '''Should create a new method to this interface.'''
        iface = Interface('Calc')
        method = iface.create_method('sum', NativeTypes.INT32,
                                     ('i0', NativeTypes.INT32), ('i1', NativeTypes.INT32))

        assert [method] == iface.declared_methods
        assert method.name == 'sum'
        assert method.result is NativeTypes.INT32
        assert method.args[0].name == 'i0'
        assert method.args[1].name == 'i1'


class TestMethod(unittest.TestCase):
    def test_parse(self):
        node = ast.Method('name', args=[ast.Field('arg0', ast.DefRef('int32'))],
                          result=ast.DefRef('int32'))
        lookup = mock.Mock()

        method = Method.parse_from(node, lookup)
        assert method.name == 'name'
        assert method.result
        assert len(method.args) == 1
        assert method.args[0].name == 'arg0'

    def test_fullname(self):
        method = Method('method', NativeTypes.INT32)
        method.create_arg('i0', NativeTypes.INT32)
        method.create_arg('i1', NativeTypes.INT32)

        iface = Interface('Interface')
        iface.add_method(method)

        assert method.fullname == 'Interface.method'


class TestMethodArg(unittest.TestCase):
    def test_parse_from(self):
        ref = ast.DefRef('int32')
        node = ast.Field('arg', ref)
        lookup = mock.Mock()

        arg = MethodArg.parse_from(node, lookup)
        assert arg.name == 'arg'
        lookup.assert_called_with(ref)

    def test_link(self):
        ref = lambda: NativeTypes.INT32
        arg = MethodArg('name', ref)
        arg.link()

        assert arg.name == 'name'
        assert arg.type is NativeTypes.INT32
