package com.backend.backend.shared.structures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class QueueTest {

    private Queue<String> queue;

    @BeforeEach
    void setUp() {
        queue = new Queue<>();
    }

    @Test
    void newQueueIsEmpty() {
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void enqueueDequeuePreservesFifoOrder() {
        queue.enqueue("first");
        queue.enqueue("second");
        queue.enqueue("third");

        assertEquals("first", queue.dequeue());
        assertEquals("second", queue.dequeue());
        assertEquals("third", queue.dequeue());
        assertTrue(queue.isEmpty());
    }

    @Test
    void dequeueOnEmptyQueueThrows() {
        assertThrows(NoSuchElementException.class, queue::dequeue);
    }

    @Test
    void peekOnEmptyQueueThrows() {
        assertThrows(NoSuchElementException.class, queue::peek);
    }

    @Test
    void peekReturnsFrontWithoutRemoving() {
        queue.enqueue("only");
        assertEquals("only", queue.peek());
        assertEquals(1, queue.size());
        assertFalse(queue.isEmpty());
        assertEquals("only", queue.dequeue());
    }

    @Test
    void sizeTracksEnqueuedAndDequeuedElements() {
        assertEquals(0, queue.size());
        queue.enqueue("a");
        queue.enqueue("b");
        assertEquals(2, queue.size());
        queue.dequeue();
        assertEquals(1, queue.size());
        queue.dequeue();
        assertEquals(0, queue.size());
        assertTrue(queue.isEmpty());
    }
}
