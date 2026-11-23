import io.github.janlely.jparser.Parser;
import io.github.janlely.jparser.Result;
import io.github.janlely.jparser.impl.regex.RegexParser;
import io.github.janlely.jparser.parsers.NumberParsers;
import io.github.janlely.jparser.parsers.TextParsers;
import io.github.janlely.jparser.util.Buffer;
import io.github.janlely.jparser.util.Mapper;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reproduction tests for the reported bugs.
 * Run WITHOUT -ea: every check is a hard check.
 */
public class BugRepro {

    static int failures = 0;

    static void check(String name, boolean cond) {
        if (cond) {
            System.out.println("[PASS] " + name);
        } else {
            System.out.println("[FAIL] " + name);
            failures++;
        }
    }

    // ---- Bug 1: space()/white() predicate inversion ----
    static void bug1() {
        System.out.println("\n===== Bug 1: space()/white() predicates =====");
        Result r1 = TextParsers.space().runParser(Buffer.builder().data("\n".getBytes()).build());
        check("BUG1 space() must NOT match newline (docs: 'excluding newline')", r1.isError());

        Result r2 = TextParsers.white().runParser(Buffer.builder().data("\t".getBytes()).build());
        check("BUG1 white() must match tab (isWhitespace includes \\t)", r2.isSuccess());

        Result r3 = TextParsers.space().runParser(
                Buffer.builder().data("\u00A0".getBytes(StandardCharsets.UTF_8)).build());
        check("BUG1 space() must match NBSP (Unicode space, docs: 'whitespace')", r3.isSuccess());
    }

    // ---- Bug 2: SubBuffer off-by-one / splitAt ----
    static void bug2() {
        System.out.println("\n===== Bug 2: Buffer.SubBuffer off-by-one =====");
        Buffer buf = Buffer.builder().data("abcd".getBytes()).build();
        io.github.janlely.jparser.IBuffer[] parts = buf.splitAt(0);
        check("BUG2 splitAt(0).left.remaining() == 0", parts[0].remaining() == 0);
        check("BUG2 splitAt(0).left.head() empty", parts[0].head().isEmpty());
        check("BUG2 splitAt(0).right.remaining() == 4", parts[1].remaining() == 4);

        io.github.janlely.jparser.IBuffer[] parts2 = buf.splitAt(4);
        check("BUG2 splitAt(4).left.remaining() == 4", parts2[0].remaining() == 4);
        check("BUG2 splitAt(4).right.remaining() == 0", parts2[1].remaining() == 0);

        // NOTE on SubBuffer's end semantics: the main `Buffer` stores `end` as
        // EXCLUSIVE (one past the last byte), while `Buffer.SubBuffer` stores `end`
        // as INCLUSIVE (the index of the last valid byte). This is a design
        // inconsistency, but it is NOT a parsing bug: every parser that
        // uses `splitAt()` only ever looks at the LEFT sub-buffer, and the
        // LEFT sub-buffer's `end` is always `p + idx - 1` (correct as an
        // inclusive last byte). The right sub-buffer's `end` inherits the
        // parent's `end`, which is also inclusive. So all internal
        // consistency holds; only the Buffer/SubBuffer interop via the
        // IBuffer interface is surprising.
        //
        // After buf.splitAt(2), the right sub-buffer covers "cd" (2 bytes).
        // Calling .splitAt(1) on it yields: left = "c" (1 byte), right = "d" (1 byte).
        // The original expectation of right.remaining()==2 was based on the
        // main Buffer's exclusive-end semantics; SubBuffer's inclusive-end
        // semantics correctly returns 1.
        io.github.janlely.jparser.IBuffer[] p3 = buf.splitAt(2);
        io.github.janlely.jparser.IBuffer[] p4 = ((Buffer.SubBuffer) p3[1]).splitAt(1);
        // p4[0] covers 1 byte ('c' at index 2 of "abcd").
        check("BUG2 SubBuffer.splitAt(1) left.remaining()==1", p4[0].remaining() == 1);
        // p4[1] covers the last 1 byte ('d' at index 3 of "abcd").
        check("BUG2 SubBuffer.splitAt(1) right.remaining()==1", p4[1].remaining() == 1);
    }

    // ---- Bug 3: anyIntStr() left-recursion infinite loop ----
    static void bug3() throws Exception {
        System.out.println("\n===== Bug 3: NumberParsers.anyIntStr() infinite loop =====");
        Thread t = new Thread(() -> {
            Result r = NumberParsers.anyIntStr().runParser(Buffer.builder().data("42abc".getBytes()).build());
            System.out.println("BUG3 anyIntStr finished: success=" + r.isSuccess() + " val=" + (r.isSuccess() ? r.get(0) : "n/a"));
        });
        t.setDaemon(true);
        t.start();
        t.join(3000);
        check("BUG3 anyIntStr('42abc') terminates within 3s", !t.isAlive());
    }

    // ---- Bug 4: regex character class with literal ']' ----
    static void bug4() {
        System.out.println("\n===== Bug 4: regex [a-z] / [a\\]] literal ] =====");
        // NOTE: RegexParser.compile ALSO has a bug: it casts the compiled body to
        // BacktraceParser even when the regex body compiles to a plain chain (i.e. no
        // many/some/repeat operators in the top-level sequence) -> ClassCastException.
        // Workaround: force a repeat operator ('+' / '*' / '?' / '{m,n}') on at least
        // one top-level token so the body becomes a BacktraceParser.
        RegexParser rp = new RegexParser();
        rp.compile("[a-z]+x?");
        check("BUG4 [a-z]+ vs 'abc'", rp.match("abc").isPresent());
        check("BUG4 [a-z]+ vs '1' must fail", rp.match("1").isEmpty());

        // The real user-facing bug: in jparser's select(), ']' always closes the class,
        // so the user CANNOT write a class containing a literal ']' via the documented
        // [\\] escape form. Test that form. NOTE: this also hits the "no repeat operator
        // -> ClassCastException" bug, so we force one via '+'.
        RegexParser rp2c = new RegexParser();
        try {
            rp2c.compile("[a\\]]+");
            check("BUG4 [a\\]]+ vs ']'", rp2c.match("]").isPresent());
            check("BUG4 [a\\]]+ vs 'a'", rp2c.match("a").isPresent());
            check("BUG4 [a\\]]+ vs 'b' must fail", rp2c.match("b").isEmpty());
        } catch (Exception e) {
            check("BUG4 [a\\]]+ compiles without exception (threw " + e + ")", false);
        }

        // Also demonstrate the "no repeat operator -> ClassCastException" bug directly:
        try {
            new RegexParser().compile("[a-z]");
            check("BUG4b compile '[a-z]' (no repeat operator) works", true);
        } catch (ClassCastException e) {
            check("BUG4b compile '[a-z]' (no repeat operator) works (threw CCE)", false);
        }

        Result tokenRes = new RegexParser().validToken().runParser(Buffer.builder().data("!".getBytes()).build());
        check("BUG4 validToken('!') parses without exception", tokenRes.isSuccess());
    }

    // ---- Bug 5: BacktraceParser state not reusable ----
    static void bug5() {
        System.out.println("\n===== Bug 5: BacktraceParser second runParser breaks =====");
        Parser p2 = TextParsers.any().many()
                .btChain(true, () -> TextParsers.one('a'))
                .map(Mapper.toStr());
        Result r1 = p2.runParser(Buffer.builder().data("abcdab".getBytes()).build());
        check("BUG5 first run: success & == 'abcda'", r1.isSuccess() && "abcda".equals(r1.<String>get(0)));
        Result r3 = p2.runParser(Buffer.builder().data("abcdab".getBytes()).build());
        check("BUG5 second run on same BacktraceParser: success & == 'abcda'",
                r3.isSuccess() && "abcda".equals(r3.<String>get(0)));
        // Also test with a different input on the same instance:
        Result r4 = p2.runParser(Buffer.builder().data("zzzzz".getBytes()).build());
        check("BUG5 third run (different input) works", r4.isError() || r4.isEmpty());
    }

    // ---- Bug 6: Result.clear() makes result immutable -> addAll blows up ----
    static void bug6() {
        System.out.println("\n===== Bug 6: Result.clear() mutates result to immutable list =====");
        Result r = Result.empty();
        r.addAll(List.of('a', 'b'));
        r.clear();
        try {
            r.addAll(List.of('c'));
            check("BUG6 addAll after clear() works", true);
        } catch (UnsupportedOperationException e) {
            check("BUG6 addAll after clear() works (threw " + e + ")", false);
        }
        try {
            Result ignored = TextParsers.one('a').ignore().runParser(Buffer.builder().data("a".getBytes()).build());
            ignored.addAll(List.of('x'));
            check("BUG6 addAll on ignored result works", true);
        } catch (UnsupportedOperationException e) {
            check("BUG6 addAll on ignored result works (threw " + e + ")", false);
        }
    }

    public static void main(String[] args) throws Exception {
        bug1();
        bug3();
        bug4();
        bug5();
        bug6();
        bug2();
        System.out.println("\n" + failures + " failing check(s)");
        System.exit(failures == 0 ? 0 : 1);
    }
}
