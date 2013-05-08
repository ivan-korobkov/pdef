# encoding: utf-8
from pdef.common import PdefException


def check_not_none(argument, msg=None, *args):
    '''Checks that argument is not none or raises PdefException.'''
    if argument:
        return argument

    msg = msg % args if msg else "Cannot be none"
    raise PdefException(msg)


def check_argument(expr, msg=None, *args):
    '''Checks the expression or raises PdefException.'''
    if expr:
       return True

    msg = msg % args if msg else 'Wrong argument'
    raise PdefException(msg)


def check_state(expr, msg=None, *args):
    '''Checks the expression or raises PdefException.'''
    if expr:
        return True

    msg = msg % args if msg else 'Illegal state exception'
    raise PdefException(msg)


def check_isinstance(argument, class_or_type_or_typle, msg=None, *args):
    '''Checks argument type or raises TypeException.'''
    if isinstance(argument, class_or_type_or_typle):
        return argument

    msg = msg % args if msg else\
    'Wrong type, must be %s, got %s' % (class_or_type_or_typle, argument)
    raise TypeError(msg)
