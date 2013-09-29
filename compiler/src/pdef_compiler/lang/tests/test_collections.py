# encoding: utf-8




class TestList(unittest.TestCase):
    def test_element_datatype(self):
        '''Should prevent list elements which are not data types.'''
        iface = Interface('Interface')
        d = List(iface)
        d.link()

        try:
            d.validate()
        except CompilerException, e:
            assert 'List elements must be data types' in e.message


class TestSet(unittest.TestCase):
    def test_element_datatype(self):
        '''Should prevent set elements which are not data types.'''
        iface = Interface('Interface')
        d = Set(iface)
        d.link()

        try:
            d.validate()
            self.fail()
        except CompilerException, e:
            assert 'Set elements must be data types' in e.message


class TestMap(unittest.TestCase):
    def test_key_primitive(self):
        msg = Message('Message')
        d = Map(msg, msg)
        d.link()

        try:
            d.validate()
            self.fail()
        except CompilerException, e:
            assert 'Map keys must be primitives' in e.message

    def test_value_datatype(self):
        iface = Interface('Interface')
        d = Map(NativeTypes.STRING, iface)
        d.link()

        try:
            d.validate()
            self.fail()
        except CompilerException, e:
            assert 'Map values must be data types' in e.message
