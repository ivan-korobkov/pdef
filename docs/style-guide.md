Pdef Style Guide
================
This style guide gives Pdef coding conventions. Following them makes code consistent,
and allows generators to change style for the generated code.

- Maximum 100 characters per line (to allow opening several files side-by-side).
- Spaces not tabs.
- 4 spaces per indentation level.
- `lowercase_with_underscores` for package and module names.
- Upper `CamelCase` for enums, messages and interfaces.
- Lower `camelCase` for fields, methods and arguments.
- `UPPERCASE_WITH_UNDERSCORES` for enum values.
- Align field names and types in two columns.
- Align argument names and types in two columns when methods are longer than one line.
- Indent fields, methods, and arguments.
- Put the starting brace `{` on the same line with the definition name.
- Put the ending brace `}` on the next line after the definition.
- Separate definitions by one or two empty lines; subtypes without bodies can be grouped together.
- Treat abbreviations as words, do not capitalize all letters. Correct: `UrlResource`,
`RpcRequest`; not `URLResource`, `RPCRequest`.

Example:
```pdef
from package.module import submodule;   // lowercase for packages and modules.

enum ArticleEventType {                 // Upper CamelCase for enums.
    CREATED, PUBLISHED, DELETED;        // UPPERCASE for enum values.
}

// Separate definitions by one or two lines.
message ArticleEvent {                  // Upper CamelCase for messages.
    id          int64                   // Lower camelCase for fields.
    authorId    int64;                  // Fields are aligned as a table.
    type        ArticleEventType @discriminator;
    createdAt   datetime;
}

// Subtypes without bodies can be grouped together.
message ArticleCreated : ArticleEvent(ArticleEventType.CREATED) {}
message ArticlePublished : ArticleEvent(ArticleEventType.PUBLISHED) {}

interface Articles {
    findOne(articleId int32) Article;   // Lower camelCase for methods and arguments.

    // Arguments in long methods are aligned as a table.
    @post
    create(
        title       string,
        text        string,
        date        datetime,
        imageIds    list<int64>,
        tags        list<string>,
        location    Location) Article;
}
```
