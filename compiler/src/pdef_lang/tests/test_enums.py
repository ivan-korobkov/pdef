# encoding: utf-8


class TestEnum(unittest.TestCase):
    def test_parse_node(self):
        '''Should create an enum from an AST node.'''
        node = ast.Enum('Number', values=('ONE', 'TWO', 'THREE'))
        lookup = mock.Mock()

        enum = Enum.parse_node(node, lookup)
        assert len(enum.values) == 3
        assert enum.get_value('ONE')
        assert enum.get_value('TWO')
        assert enum.get_value('THREE')

    def test_add_value(self):
        '''Should add to enum a new value by its name.'''
        enum = Enum('Number')
        one = enum.add_value('ONE')

        assert one.is_enum_value
        assert one.name == 'ONE'
        assert one.enum is enum
