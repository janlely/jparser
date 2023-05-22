# jparser

# Haskell style Parser Combinater implemented in Java.
Just like the usage of Parser Combinator in Haskell, jparser does not generate AST, so you need to use the map method to map the results to a custom class. You can see how it's done specifically from the implementation of JsonParser.

## Maven (jdk11+ is required)
```xml
<dependency>
  <groupId>io.github.janlely</groupId>
  <artifactId>jparser</artifactId>
  <version>1.0.4</version>
</dependency>
```

## hello world: Simple Calculator
```java
public class Calculator {

    @Test
    public void testCalc() {
        Result result = expr().parse(Buffer.builder().data("(1+2)*3-(4*2)".getBytes()).build());
        assert result.<Double>get(0).compareTo(1.0) == 0;
        result = expr().parse(Buffer.builder().data("1+2*3-(4*2)".getBytes()).build());
        assert result.<Double>get(0).compareTo(-1.0) == 0;
    }

    public Parser expr() {
        return Parser.choose(
                () -> term().chain(TextParsers.one('+').ignore())
                        .chain(() -> expr()).map(s -> (double)s.get(0) + (double)s.get(1)),
                () -> term().chain(TextParsers.one('-').ignore())
                        .chain(() -> expr()).map(s -> (double)s.get(0) - (double)s.get(1)),
                () -> term()
        );
    }

    public Parser term() {
        return Parser.choose(
                () -> factor().chain(TextParsers.one('*').trim(false).ignore())
                        .chain(() -> term()).map(s -> (double)s.get(0) * (double)s.get(1)),
                () -> factor().chain(TextParsers.one('/').trim(false).ignore())
                        .chain(() -> term()).map(s -> (double)s.get(0) / (double)s.get(1)),
                () -> factor()
        );
    }

    public Parser factor() {
        return Parser.choose(
                TextParsers.one('(').ignore()
                        .chain(() -> expr())
                        .chain(TextParsers.one(')').ignore()),
                number()
        );
    }

    public Parser number() {
        return NumberParsers.anyDoubleStr();
    }
}

```

## basic parsers
```haskell
ByteParsers::satisfy //parse a byte based on a condition.
ByteParsers::any //parse any byte
ByteParsers::one //parse a specified byte
ByteParsers::take //parse N arbitrary bytes.
ByteParsers::skip //skip N arbitrary bytes.
ByteParsers::takeWhile //parse any number of bytes that meet a certain condition.
ByteParsers::skipWhile //skip any number of bytes that meet a certain condition.

TextParsers::satisfy //parse a character that satisfies a condition according to the given encoding.
TextParsers::one //parse a character according to the given encoding
TextParsers::oneOf //parse a character which is one of the given string 
TextParsers::noneOf //parse a character which is none of the given string 
TextParsers::string //parse a given string according to the given encoding.
TextParsers::any //parse any n characters according to the specified encoding.
TextParsers::take //parse any n characters according to the specified encoding.
TextParsers::takeWhile //parse characters that satisfy a condition according to the given encoding and return a string.
TextParsers::skip //skip n characters of the given encoding.
TextParsers::skipWhile //skip characters that satisfy a condition according to the given encoding
TextParsers::eof //end of line
TextParsers::space //parse one whitespace character, excluding newline characters.
TextParsers::spaces //parse whitespace characters, excluding newline characters.
TextParsers::white //parse one whitespace character
TextParsers::whites //parse whitespace characters

NumberParsers::intStr //parse a specified integer encoded as a string.
NumberParsers::anyIntStr //parse a any integer encoded as a string.
NumberParsers::intLE //parse a specified integer encoded in little-endian format
NumberParsers::intBE //parse a specified integer encoded in big-endian format
NumberParsers::longLE //parse a specified long integer encoded in little-endian format
NumberParsers::longBE //parse a specified long integer encoded in big-endian format
NumberParsers::anyIntLE //parse any integer encoded in little-endian format
NumberParsers::anyIntBE //parse any integer encoded in big-endian format
NumberParsers::anyLongLE //parse any long integer encoded in little-endian format
NumberParsers::andLongBE //parse any long integer encoded in big-endian format
```
## usefull combinaters
```haskell
Parser::chain //chain another parser
Parser::btChain //chain another parser with backtrace enabled
Parser::chainWith //chain another parser generator(res -> Parser)
Parser::or //if this parser failed than try another
Parser::ignore //do parse, but ignore result
Parser::map //do parse and map the result
Parser::sepBy //parse multiply times seperate by another parser
Parser::scan //continuously reduce the input and attempt to parse it.
Parser::optinal //make this parser optional
Parser::trim //ignore leading and trailing whitespace and attempt to parse.
Parser::many //parse zero or more times
Parser::manyTill //implement with repeatTill
Parser::some //parse one or more times
Parser::someTill //implement with someTill
Parser::repeat //parse n times
Parser::repeatTill //parse several time till subparser matched
Parser::range //parse bettwen m and n times
Parser::attempt //parse zero to n times
Parser::must //add a predicate on the result.
Parser::choose //chain of Parser::or
```


## advanced usage:
* to parse any input: implement IBuffer

## Sample Usage: implement a CSV parser
* Parser a simple csv line:
```java
    @Test
    public void testSimpleCsv() {
        Parser csvLineParser = TextParsers.satisfy(Character::isLetterOrDigit)
                .many().map(Mapper.toStr())
                .sepBy(TextParsers.one(',').ignore());
        Result result = csvLineParser.runParser(Buffer.builder()
                .data("field1,field2,field3,field4".getBytes())
                .build());
        assert result.<String>get(0).equals("field1");
        assert result.<String>get(1).equals("field2");
        assert result.<String>get(2).equals("field3");
        assert result.<String>get(3).equals("field4");
    }
```
* Parser a standard csv
see [CSVParser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/csv/CsvParser.java)

## Sample Usage: implement a Json parser
```java
    public static Parser jsonParser() {
        return stringParser()
                .or(() -> objectParser().trim(true))
                .or(() -> arrayParser().trim(true))
                .or(() -> nullParser().trim(true))
                .or(() -> boolParser().trim(true))
                .or(() -> numberParser().trim(true))
                .trim(true);
    }

    public static Parser objectParser() {
        return TextParsers.one('{').ignore()
                .chain(() -> membersParser())
                .chain(() -> TextParsers.one('}').ignore());
    }

    public static Parser arrayParser() {
        return TextParsers.one('[').ignore()
                .chain(() -> jsonParser().sepBy(TextParsers.one(',').ignore()))
                .chain(() -> TextParsers.one(']').ignore())
                .map(ary -> JsonValue.builder()
                        .type(JsonType.ARRAY)
                        .value(new JsonArray().addAll(ary))
                        .build());
    }

    public static Parser membersParser() {
        ...
    }
    ...


    @Test
    public void test() {
        String source1 = "  {\"hello\":\"world\",\"array\":[\"a\",{\"b\":\"c\"},null,123.4],\"name\":\"jay\"}  ";
        Result result1 = JsonParser.parser().runParser(Buffer.builder()
                .data(source1.getBytes())
                .build());
        assert result1.isSuccess();
    }
```
see [JsonPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/json/JsonParser.java)


## Sample Usage: implement a XML parser
```java
    /**
     * Parse a full xml: <tag> content or children node </tag>
     * @return
     */
    public static Parser fullParser() {
        return headParser()
                .chain(() -> nodeParser().some().or(() -> contentParser()))
                .chainWith(result -> {
                    String name = result.<XmlNode>get(0).getName();
                    return TextParsers.string("</").ignore()
                            .chain(() -> TextParsers.string(name).trim(true))
                            .chain(() -> TextParsers.string(">").ignore())
                            .ignore();
                }).map(values -> {
        ...
    }

    @Test
    public void testNode() {
        String src = "<note hello=\"world\">\n" +
                       "<to>Tove</to>\n" +
                       "<from>Jani</from>\n" +
                       "<heading>Reminder</heading>\n" +
                       "<body>Don't forget me this weekend!</body>\n" +
                     "</note>";
        Result result1 = XmlParser.parser().runParser(Buffer.builder()
                .data(src.getBytes())
                .build());
        assert result1.isSuccess();
    }

```
see [XmlPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/xml/XmlParser.java)


## Sample Usage: implement a Regex parser
```java

    public Parser parser() {
        return Parser.choose(
                () -> many(),
                () -> some(),
                () -> range(),
                () -> repeat(),
                () -> optional(),
                () -> validToken()
        ).many().map(s -> {
        ...
    }

    @Test
    public void testMatch() {
        RegexParser regexParser = new RegexParser();
        regexParser.compile("(a+)(b+)c?$");
        Optional<String> result = regexParser.match("aaaabbbbc");
        assert result.isPresent();
    }

    @Test
    public void testGroup() {
        RegexParser regexParser = new RegexParser();
        regexParser.compile("(a+)b\\1");
        Optional<String> result = regexParser.match("aabaa");
        assert result.isPresent();
        List<String> searchResult = regexParser.search("aaabaaa");
        assert searchResult.size() == 2;
    }
```
see [RegexPaser](https://github.com/janlely/jparser/blob/main/src/main/java/org/jay/parser/impl/regex/RegexParser.java)


