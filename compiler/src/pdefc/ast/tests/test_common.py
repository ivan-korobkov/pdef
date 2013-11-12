# encoding: utf-8
import unittest
from pdefc.ast.common import Validatable


class TestValidatable(unittest.TestCase):
    def test_validate(self):
        v = Validatable()
        v._validate = lambda: ['error']

        assert v.validate() == ('error', )
        assert v.validated
        assert not v.is_valid

    def test_validate__save_state(self):
        v = Validatable()
        v._validate = lambda: ['error']
        assert v.validate() == ('error', )

        v._validate = lambda: ['another error']
        assert v.validate() == ('error', )
