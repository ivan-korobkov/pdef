# encoding: utf-8


class LanguageException(Exception):
    '''Base Pdef exception.'''
    pass


class ValidatorException(LanguageException):
    def __init__(self, errors=None):
        super(ValidatorException, self).__init__('Invalid code')
        self.errors = errors or []


class LinkerException(LanguageException):
    def __init__(self, errors=None):
        super(LinkerException, self).__init__('Symbols not found')
        self.errors = errors
