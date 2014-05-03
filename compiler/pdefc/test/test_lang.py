# encoding: utf-8
import unittest
from pdefc.lang import *


class TestNode(unittest.TestCase):
    def test_create_reference__should_add_child_reference(self):
        node = Node()
        ref = node.create_reference('Type')
        
        assert ref.name == 'Type'
        assert ref in node.children
    
    def test_walk__should_walk_this_node_tree(self):
        node0 = Node()
        node1 = Node()
        node2 = Node()
        
        node1.add_child(node2)
        node0.add_child(node1)
        
        seq = list(node0.walk())
        assert seq == [node0, node1, node2]


class TestPackage(unittest.TestCase):
    def test_add_file__should_set_package_and_add_file_to_children(self):
        file = File('test')
        package = Package()
        package.add_file(file)
        
        assert file.package is package
        assert file in package.children
    
    def test_compile(self):
        pass
    
    def test_find__should_return_type_by_name(self):
        struct = Struct('Struct')
        file = File('test')
        file.add_type(struct)
        package = Package(files=[file])
        
        assert package.find('Struct') is struct
    
    def test_find__should_return_None_when_absent(self):
        package = Package()
        assert package.find('Absent') is None
    
    def test_has_duplicate_types__should_prevent_duplicate_type_names(self):
        struct0 = Struct('struct')
        struct1 = Struct('struct')
        
        file0 = File('test0', types=[struct0])
        file1 = File('test1', types=[struct1])
        
        package = Package(files=[file0, file1])
        errors = package._has_duplicate_types(Errors())
        
        assert len(errors) == 1
        assert 'Duplicate type "struct"' in errors[0]


class TestFile(unittest.TestCase):
    def test_dotname__should_return_path_without_ext_with_dots_as_separators(self):
        file = File('hello/world/test.pdef')
        assert file.dotname == 'hello.world.test'
    
    def test_add_type__should_set_type_file_and_add_to_children(self):
        struct = Struct('Struct')
        file = File('file')
        file.add_type(struct)
        
        assert struct.file is file
        assert struct in file.children
    
    def test_validate__should_prevent_invalid_dotname(self):
        errors = Errors()
        file = File('hello-word/test.pdef')
        file.validate(errors)
        
        assert 'Wrong file name' in errors[0]
    
    def test_validate__should_allow_valid_dotname(self):
        errors = Errors()
        file = File('hello/world/test_file.pdef')
        file.validate(errors)
        
        assert len(errors) == 0


class TestReference(unittest.TestCase):
    def test_link__should_find_type_in_scope(self):
        errors = Errors()
        struct = Struct('Hello')
        file = File('file', types=[struct])
        package = Package(files=[file])
        
        ref = Reference('Hello')
        ref.link(errors, package)
        
        assert ref.dereference() is struct
    
    def test_link__should_add_error_when_type_not_found(self):
        errors = Errors()
        package = Package()
        ref = Reference('Absent')
        ref.link(errors, package)

        assert len(errors) == 1
        assert 'Symbol not found "Absent"' in errors[0]


class TestType(unittest.TestCase):
    def test_referenced_types__should_return_set_of_all_children_types(self):
        struct = Struct('Test')
        struct.create_field('field0', INT16)
        struct.create_field('field1', STRING)
        struct.create_field('field2', struct)
        
        referenced = struct.referenced_types
        assert referenced == {INT16, STRING}


class TestList(unittest.TestCase):
    def test_validate__element_should_be_data_type(self):
        errors = Errors()
        iface = Interface('Test')
        list0 = List(iface)
        list0.validate(errors)

        assert len(errors) == 1        
        assert 'List element must be a data type' in errors[0]


class TestSet(unittest.TestCase):
    def test_validate__element_should_be_data_type(self):
        errors = Errors()
        iface = Interface('Test')
        set0 = Set(iface)
        set0.validate(errors)

        assert len(errors) == 1
        assert 'Set element must be a data type' in errors[0]


class TestMap(unittest.TestCase):
    def test_validate__key_should_be_number_or_string(self):
        errors = Errors()
        struct = Struct('Test')
        map0 = Map(struct, INT32)
        map0.validate(errors)
        
        assert len(errors) == 1
        assert 'Map key must be a number or a string' in errors[0]

    def test_validate__key_should_be_number_or_string_not_enum(self):
        errors = Errors()
        enum = Enum('Test')
        map0 = Map(enum, INT32)
        map0.validate(errors)

        assert len(errors) == 1
        assert 'Map key must be a number or a string' in errors[0]
    
    def test_validate__value_should_be_data_type(self):
        errors = Errors()
        iface = Interface('Test')
        map0 = Map(INT32, iface)
        map0.validate(errors)

        assert len(errors) == 1
        assert 'Map value must be a data type' in errors[0]


class TestEnum(unittest.TestCase):
    def test_add_value__should_set_value_enum_and_add_to_children(self):
        enum = Enum('Test')
        value = enum.create_value('ONE')
        
        assert isinstance(value, EnumValue)
        assert value.enum is enum
        assert value in enum.children
    
    def test_validate__should_prevent_duplicate_values(self):
        errors = Errors()
        enum = Enum('Test', values=['ONE', 'TWO', 'ONE'])
        enum.validate(errors)
        
        assert len(errors) == 1
        assert 'Duplicate enum value "ONE"' in errors[0]


class TestStruct(unittest.TestCase):
    def test_add_field__should_set_field_struct_and_add_to_children(self):
        struct = Struct('Test')
        field = struct.create_field('field', INT32)
        
        assert isinstance(field, Field)
        assert field.name == 'field'
        assert field.struct is struct
        assert field in struct.children
    
    def test_validate__should_prevent_duplicate_fields(self):
        struct = Struct('Test')
        struct.create_field('field0', INT32)
        struct.create_field('field1', INT32)
        struct.create_field('field0', STRING)
        
        errors = Errors()
        struct.validate(errors)
        
        assert len(errors) == 1
        assert 'Duplicate field "field0"' in errors[0]


class TestField(unittest.TestCase):
    def test_validate__type_should_be_data_type(self):
        errors = Errors()
        iface = Interface('Test')
        field = Field('field', iface)
        field.validate(errors)
        
        assert len(errors) == 1
        assert 'Field type must be a data type' in errors[0]


class TestInterface(unittest.TestCase):
    def test_add_method__should_set_method_interface_and_add_to_children(self):
        iface = Interface('Test')
        method = iface.create_method('method')
        
        assert method.name == 'method'
        assert method.interface is iface
        assert method in iface.children
    
    def test_validate__should_prevent_duplicate_methods(self):
        iface = Interface('Test')
        iface.create_method('method0')
        iface.create_method('method1')
        iface.create_method('method0')
        
        errors = Errors()
        iface.validate(errors)
        
        assert len(errors) == 1
        assert 'Duplicate method "method0"' in errors[0]


class TestMethod(unittest.TestCase):
    def test_add_arg__should_set_arg_method_and_add_to_children(self):
        method = Method('method')
        arg = method.create_arg('arg', INT32)
        
        assert arg.name == 'arg'
        assert arg.method is method
        assert arg.type is INT32
        assert arg in method.children
    
    def test_validate__should_allow_only_get_and_post_method_types(self):
        method = Method('method')
        method.type = 'WRONG'
        
        errors = Errors()
        method.validate(errors)
        
        assert len(errors) == 1
        assert 'Unknown method type "WRONG"' in errors[0]
    
    def test_validate__post_method_should_have_data_type_result(self):
        iface = Interface('Test')
        method = Method('method', type=MethodType.POST, result=iface)
        
        errors = Errors()
        method.validate(errors)
        
        assert len(errors) == 1
        assert 'POST method must have a data type result or be void' in errors[0]
    
    def test_validate__should_prevent_duplicate_args(self):
        method = Method('method')
        method.create_arg('arg0', INT32)
        method.create_arg('arg1', INT32)
        method.create_arg('arg0', INT32)
        
        errors = Errors()
        method.validate(errors)
        
        assert len(errors) == 1
        assert 'Duplicate method argument "arg0"' in errors[0]


class TestArgument(unittest.TestCase):
    def test_validate__type_should_be_data_type(self):
        arg = Argument('arg', VOID)
        
        errors = Errors()
        arg.validate(errors)
        
        assert len(errors) == 1
        assert '"arg" argument must be a data type' in errors[0]
