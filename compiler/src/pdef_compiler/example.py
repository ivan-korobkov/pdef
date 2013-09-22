# encoding: utf-8
import pdef_compiler as compiler

package = compiler.parse(paths)
compiler.check(package)
compiler.compile(package, generators)
