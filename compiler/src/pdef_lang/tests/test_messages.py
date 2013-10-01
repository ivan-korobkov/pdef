# encoding: utf-8


class TestMessage(unittest.TestCase):
    def test_parse_node(self):
        '''Should create a message from an AST node.'''
        node = ast.Message('Msg', base=ast.DefRef('Base'),
                           fields=[ast.Field('field', ast.ValueRef(Type.INT32))])
        lookup = mock.Mock()

        msg = Message.parse_node(node, lookup)
        assert msg.name == node.name
        assert msg.base
        assert len(msg.declared_fields) == 1
        assert msg.declared_fields[0].name == 'field'

    def test_create_field(self):
        '''Should create and add a field to a message.'''
        msg = Message('Msg')
        field = msg.create_field('field', NativeTypes.INT32)

        assert [field] == msg.declared_fields
        assert field.name == 'field'
        assert field.type == NativeTypes.INT32

    def test_create_field__set_discriminator(self):
        '''Should set a message discriminator when a field is a discriminator.'''
        enum = Enum('Type')
        msg = Message('Msg')

        field = msg.create_field('type', enum, is_discriminator=True)
        msg.link()
        assert field.is_discriminator
        assert msg.discriminator is field

    def test_inherited_fields(self):
        '''Should correctly compute message inherited fields.'''
        enum = Enum('Type')
        type0 = enum.add_value('Type0')
        type1 = enum.add_value('Type1')

        base = Message('Base')
        type_field = base.create_field('type', enum, is_discriminator=True)
        msg0 = Message('Msg0')
        field0 = msg0.create_field('field0', NativeTypes.INT32)
        msg0.set_base(base, type0)

        msg1 = Message('Msg1')
        field1 = msg1.create_field('field1', NativeTypes.STRING)
        msg1.set_base(msg0, type1)

        assert msg1.fields == [type_field, field0, field1]
        assert msg1.inherited_fields == [type_field, field0]
        assert msg0.fields == [type_field, field0]
        assert msg0.inherited_fields == [type_field]


class TestField(unittest.TestCase):
    def test_parse_node(self):
        node = ast.Field('field', ast.ValueRef(Type.STRING), is_discriminator=True)
        lookup = mock.Mock()

        field = Field.parse_node(node, lookup)
        assert field.name == 'field'
        assert field.is_discriminator
        lookup.assert_called_with(node.type)

    def test_fullname(self):
        message = Message('Message')
        field = Field('field', NativeTypes.STRING, message=message)

        assert field.fullname == 'Message.field'

