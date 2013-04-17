# encoding: utf-8


def upper_first(s):
    if not s:
        return s
    return s[0].upper() + s[1:]
