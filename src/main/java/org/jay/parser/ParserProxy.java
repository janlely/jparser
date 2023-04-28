package org.jay.parser;

public abstract class ParserProxy extends Parser{

    private Parser parser;

    public abstract Result proxyParse(Context context);

    @Override
    public Result runParser(Context context) {
        return proxyParse(context);
    }
}
