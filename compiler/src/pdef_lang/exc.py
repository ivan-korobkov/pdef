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
