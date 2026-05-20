package com.backend.backend.shared.structures;

import java.util.NoSuchElementException;

/**
 * A generic FIFO queue backed by a singly-linked list of nodes.
 *
 * <p>Elements are removed in the same order they were added (first in, first out). {@link
 * #enqueue(Object)}, {@link #dequeue()}, and {@link #peek()} run in O(1) time; {@link #size()} is
 * O(1).
 */
public class Queue<T> {

    private static final class Node<T> {
        final T value;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    /** Adds {@code item} to the rear of the queue. */
    public void enqueue(T item) {
        Node<T> node = new Node<>(item);
        if (tail == null) {
            head = node;
            tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    /**
     * Removes and returns the element at the front of the queue.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public T dequeue() {
        if (head == null) {
            throw new NoSuchElementException("Queue is empty");
        }
        T value = head.value;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return value;
    }

    /**
     * Returns the element at the front without removing it.
     *
     * @throws NoSuchElementException if the queue is empty
     */
    public T peek() {
        if (head == null) {
            throw new NoSuchElementException("Queue is empty");
        }
        return head.value;
    }

    /**
     * @return {@code true} if the queue contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * @return the number of elements currently in the queue
     */
    public int size() {
        return size;
    }
}
