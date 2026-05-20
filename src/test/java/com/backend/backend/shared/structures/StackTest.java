package com.backend.backend.shared.structures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class StackTest {

    @Test
    void pushPopPreservesLifoOrder() {
        Stack<String> stack = new Stack<>();
        stack.push("first");
        stack.push("second");
        stack.push("third");

        assertThat(stack.pop()).isEqualTo("third");
        assertThat(stack.pop()).isEqualTo("second");
        assertThat(stack.pop()).isEqualTo("first");
        assertThat(stack.isEmpty()).isTrue();
    }

    @Test
    void popOnEmptyStackThrows() {
        Stack<String> stack = new Stack<>();

        assertThatThrownBy(stack::pop)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void peekReturnsTopWithoutRemoving() {
        Stack<Integer> stack = new Stack<>();
        stack.push(10);
        stack.push(20);

        assertThat(stack.peek()).isEqualTo(20);
        assertThat(stack.size()).isEqualTo(2);
        assertThat(stack.pop()).isEqualTo(20);
    }

    @Test
    void peekOnEmptyStackThrows() {
        Stack<String> stack = new Stack<>();

        assertThatThrownBy(stack::peek)
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void sizeTracksElementCount() {
        Stack<String> stack = new Stack<>();
        assertThat(stack.size()).isZero();

        stack.push("a");
        stack.push("b");
        assertThat(stack.size()).isEqualTo(2);

        stack.pop();
        assertThat(stack.size()).isOne();
    }

    @Test
    void toListBottomToTopPreservesPushOrder() {
        Stack<String> stack = new Stack<>();
        stack.push("a");
        stack.push("b");
        stack.push("c");

        assertThat(stack.toListBottomToTop()).containsExactly("a", "b", "c");
        assertThat(stack.size()).isEqualTo(3);
    }
}
