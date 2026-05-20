package com.backend.backend.shared.structures;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Generic circular doubly linked list backed by a sentinel head node.
 *
 * <p>The sentinel does not store user data; it anchors the ring so that {@code head.next} is the
 * first logical element and {@code head.prev} is the last. Each node has {@code prev} and {@code
 * next} pointers, and the last node links back to the first, forming a closed ring in both
 * directions.
 */
public class CircularDoublyLinkedList<T> implements Iterable<T> {

    private final Node<T> head = new Node<>(null);
    private int size;

    public CircularDoublyLinkedList() {
        head.next = head;
        head.prev = head;
    }

    /** Appends {@code value} at the end of the ring, immediately before the sentinel head. */
    public void addLast(T value) {
        Node<T> node = new Node<>(value);
        Node<T> tail = head.prev;
        node.next = head;
        node.prev = tail;
        tail.next = node;
        head.prev = node;
        size++;
    }

    public boolean contains(T value) {
        Node<T> current = head.next;
        while (current != head) {
            if (Objects.equals(current.data, value)) {
                return true;
            }
            current = current.next;
        }
        return false;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    /** Returns elements in forward order from the first inserted node to the last. */
    public List<T> toList() {
        List<T> result = new ArrayList<>(size);
        for (T item : this) {
            result.add(item);
        }
        return result;
    }

    /** Forward iteration starting at the first inserted node. */
    public Iterable<T> traverseForward() {
        return this;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            private Node<T> current = head.next;
            private int remaining = size;

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                T data = current.data;
                current = current.next;
                remaining--;
                return data;
            }
        };
    }

    static final class Node<T> {
        T data;
        Node<T> prev;
        Node<T> next;

        Node(T data) {
            this.data = data;
        }
    }
}
