# CraftingInterpreters

![actions status](https://github.com/pin3da/CraftingInterpreters/workflows/kotlin%20CI/badge.svg)

Kotlin implementation of https://craftinginterpreters.com

## Compiling, testing and runing

The project can be loaded with IntellIJ and run from the IDE. But it also has maven configured so you can run it without any IDE with the following commands.

### Compile

```bash
mvn package
```

### Test

This includes unit tests and Ktlint test for style checking

```bash
mvn verify
```

### Run 

```bash
./lox [filename]
```

## Notes

- Unlike the original code, this one does not use the visitor pattern. Instead,
this code implements a "pattern matching"-like approach using when statements
and sealed classes. As result, the code makes a linear search across all the
subtypes of Expr (instead of the constant indirection in the visitor pattern),
but removes a lot of boilerplate and code generation.
