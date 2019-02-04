package net.etalia.jalia;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;
import org.junit.rules.ExpectedException;

public class TestBase {

    @Rule
    public final ErrorCollector collect = new ErrorCollector();

    @Rule
    public final ExpectedException expected = ExpectedException.none();

    public <T> void checkThat(T value, Matcher<T> matcher) {
        collect.checkThat(value, matcher);
    }

    public <T> void checkThat(String reason, T value, Matcher<T> matcher) {
        collect.checkThat(reason, value, matcher);
    }

    public void expect(Matcher<?> matcher) {
        expected.expect(matcher);
    }

    public void expect(Class<? extends Throwable> type) {
        expected.expect(type);
    }

    public void expectMessage(String substring) {
        expected.expectMessage(substring);
    }

    public void expectMessage(Matcher<String> matcher) {
        expected.expectMessage(matcher);
    }

    public void expectCause(Matcher<? extends Throwable> expectedCause) {
        expected.expectCause(expectedCause);
    }
}
