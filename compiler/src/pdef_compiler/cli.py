# encoding: utf-8


def cli(argv=None):
    '''Run compiler command-line interface.'''
    parser = argparse.ArgumentParser(description='Protocol definition compiler')
    parser.add_argument('--verbose', '-v', action='store_true', help='verbose output')
    parser.add_argument('--debug', action='store_true', help='debug output')
    parser.add_argument('paths', metavar='path', nargs='+',
                        help='path to pdef files and directories')
    args = parser.parse_args(argv)

    level = logging.DEBUG if args.debug else logging.INFO if args.verbose else logging.WARNING
    logging.basicConfig(level=level, format='%(message)s')

    run = lambda: compile_translate(args.paths, args.java, args.python, args.python_modules)
    if args.debug:
        run()
    else:
        try:
            run()
        except PdefCompilerException, e:
            # Get rid of the traceback.
            logging.error('error: %s' % e)


# parser.add_argument('--java', help='java output directory')
# parser.add_argument('--python', help='python output directory')
# parser.add_argument('--python-module', action='append', dest='python_modules',
#                     help='python module name maps, for example pdef.tests:pdef_tests')
