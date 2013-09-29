# encoding: utf-8
from pdef_compiler import CompilerException


class ValidatorException(CompilerException):
    def __init__(self, errors=None):
        super(ValidatorException, self).__init__('Invalid code')
        self.errors = errors or []


class ValidatorError(object):
    def __init__(self, symbol, message, *args):
        self.symbol = symbol
        self.message = message
        if args:
            self.message = message % args

    def __repr__(self):
        return '<ValidatorError %s>' % self

    def __str__(self):
        if hasattr(self.symbol, 'location'):
            return '%s: %s' % (self.symbol.location, self.message)
        return '%s: %s' % (self.symbol, self.message)
