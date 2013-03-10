# encoding: utf-8


def check_not_none(argument, msg=None, *args):
    if argument:
        return argument

    msg = msg % args if msg else "Cannot be none"
    raise ValueError(msg)


def check_isinstance(argument, class_or_type_or_typle, msg=None, *args):
    if isinstance(argument, class_or_type_or_typle):
        return argument

    msg = msg % args if msg else \
        'Wrong type, must be %s, got %s' % (class_or_type_or_typle, argument)
    raise TypeError(msg)


def check_argument(expr, msg=None, *args):
    if expr:
       return True

    msg = msg % args if msg else 'Wrong argument'
    raise ValueError(msg)


def check_state(expr, msg=None, *args):
    if expr:
        return True

    msg = msg % args if msg else 'Illegal state exception'
    raise IllegalStateException(msg)


class IllegalStateException(ValueError):
    pass
