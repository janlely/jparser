package org.jay.parser;

import org.jay.parser.parsers.ByteParsers;
import org.jay.parser.parsers.CharParsers;
import org.jay.parser.util.ErrorUtil;
import org.jay.parser.util.Separator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class Parser {
    protected boolean ignore = false;
    public Result runParser(Context context) {
        Result result = parse(context);
        if (result.result == null) {
            return result;
        }
        if (isIgnore()) {
            result.result = new ArrayList(0);
        }
        return result;
    }
    public abstract Result parse(Context context);

    public Parser connect(Parser parser) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                Result step1 = Parser.this.runParser(context);
                if (step1.result == null) {
                    return Result.builder().errorMsg(step1.errorMsg).build();
                }
                Result step2 = parser.runParser(context);
                if (step2.result == null) {
                    return Result.builder().errorMsg(step2.errorMsg).build();
                }
                Result result = Result.builder().result(new ArrayList()).length(0).build();
                result.length += step1.length + step2.length;
                result.result.addAll(step1.result);
                result.result.addAll(step2.result);
                return result;
            }
        };
    }

    public Parser must(Predicate p) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                int pos = context.getPos();
                Result result = Parser.this.runParser(context);
                if (result.result != null && p.test(result.result)) {
                    return result;
                }
                context.jump(pos);
                return Result.builder()
                        .errorMsg(ErrorUtil.error(pos))
                        .build();
            }
        };
    }

    public Parser some() {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                Result first = Parser.this.runParser(context);
                if (first.result == null) {
                    return Result.builder().errorMsg(first.errorMsg).build();
                }
                Result result = Result.builder().result(new ArrayList(1)).length(0).build();
                result.length += first.length;
                result.result.addAll(first.result);
                Result tmp = Parser.this.runParser(context);
                while(tmp.result != null) {
                    result.length += tmp.length;
                    result.result.addAll(tmp.result);
                    tmp = Parser.this.runParser(context);
                }
                return result;
            }
        };
    }
    public Parser many() {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                Result result = Result.builder().result(new ArrayList(0)).length(0).build();
                Result first = Parser.this.runParser(context);
                if (first.result == null) {
                    return result;
                }
                result.length += first.length;
                result.result.addAll(first.result);
                Result tmp = Parser.this.runParser(context);
                while(tmp.result != null) {
                    result.length += tmp.length;
                    result.result.addAll(tmp.result);
                    tmp = Parser.this.runParser(context);
                }
                return result;
            }
        };
    }

    public Parser repeat(int n) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                Result result = Result.builder().result(new ArrayList(1)).length(0).build();
                if (n <= 0) {
                    return Result.builder().length(0).result(new ArrayList(0)).build();
                }
                for(int i = 0; i < n; i++) {
                    Result tmp = Parser.this.runParser(context);
                    if (tmp.result == null) {
                        return Result.builder().errorMsg(tmp.errorMsg).build();
                    }
                    result.length += tmp.length;
                    result.result.addAll(tmp.result);
                }
                return result;
            }
        };
    }

    public Parser map(Function<List, ?> mapper) {
        return new Parser() {
            @Override
            public Result parse(Context context) {
                Result result = Parser.this.parse(context);
                if (result.result == null) {
                    return result;
                }
                result.result = List.of(mapper.apply(result.result));
                return result;
            }
        };
    }

    public Parser trim() {
        Parser spaces = CharParsers.space().many();
        return spaces.connect(this).connect(spaces);
    }

    public Parser sepBy(Parser parser) {
        return connect(parser.connect(this).many());
    }
    public boolean isIgnore() {
        return this.ignore;
    }

    public Parser ignore() {
        this.ignore = true;
        return this;
    }

}
