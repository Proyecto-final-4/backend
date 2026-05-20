package com.backend.backend.shared.structures;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Generic last-in-first-out (LIFO) stack backed by a dynamically resizing array.
 *
 * @param <T> element type stored in the stack
 */
public class Stack<T> {

    private static final int INITIAL_CAPACITY = 8;

    private Object[] elements;
    private int size;

    public Stack() {
        this.elements = new Object[INITIAL_CAPACITY];
        this.size = 0;
    }

    /**
     * Pushes an item onto the top of the stack.
     *
     * @param item value to push
     */
    public void push(T item) {
        ensureCapacity(size + 1);
        elements[size++] = item;
    }

    /**
     * Removes and returns the top item.
     *
     * @return the top item
     * @throws NoSuchElementException if the stack is empty
     */
    @SuppressWarnings("unchecked")
    public T pop() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        T item = (T) elements[--size];
        elements[size] = null;
        return item;
    }

    /**
     * Returns the top item without removing it.
     *
     * @return the top item
     * @throws NoSuchElementException if the stack is empty
     */
    @SuppressWarnings("unchecked")
    public T peek() {
        if (isEmpty()) {
            throw new NoSuchElementException("Stack is empty");
        }
        return (T) elements[size - 1];
    }

    /**
     * @return {@code true} if the stack contains no elements
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * @return number of elements currently on the stack
     */
    public int size() {
        return size;
    }

    /**
     * Returns a snapshot of stack contents from bottom to top (first pushed to last pushed) without
     * modifying the stack.
     */
    @SuppressWarnings("unchecked")
    public List<T> toListBottomToTop() {
        List<T> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add((T) elements[i]);
        }
        return list;
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity <= elements.length) {
            return;
        }
        int newCapacity = elements.length * 2;
        if (newCapacity < minCapacity) {
            newCapacity = minCapacity;
        }
        Object[] resized = new Object[newCapacity];
        System.arraycopy(elements, 0, resized, 0, size);
        elements = resized;
    }
}
