# encoding: utf-8


class LanguageException(Exception):
    '''Base Pdef exception.'''
    pass


class ValidationException(LanguageException):
    def __init__(self, errors=None):
        super(ValidationException, self).__init__('Invalid code')
        self.errors = errors or []


class LinkingException(LanguageException):
    def __init__(self, errors=None):
        super(LinkingException, self).__init__('Symbols not found')
        self.errors = errors


class LanguageError(object):
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


def error(symbol, message, *args):
    return LanguageError(symbol, message, *args)
