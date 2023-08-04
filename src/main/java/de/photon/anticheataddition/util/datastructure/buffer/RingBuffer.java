package de.photon.anticheataddition.util.datastructure.buffer;

import de.photon.anticheataddition.util.mathematics.ModularInteger;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.AbstractCollection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An implementation of a ring buffer which overwrites the oldest data once it is full.
 * <p></p>
 * Note that the {@link RingBuffer} may be changed during iteration which will NOT cause the iteration to throw a
 * {@link java.util.ConcurrentModificationException}! They are guaranteed to iterate at most maxSize elements.
 * <p>
 * Make sure to properly synchronize access if iteration needs to be
 * stable.
 */
public class RingBuffer<T> extends AbstractCollection<T> implements Collection<T>
{
    @Getter
    private final int maxSize;
    private final T[] array;

    // The position at which the next element will be written.
    private final ModularInteger head;

    // The position of the oldest element (if such an element exists).
    private final ModularInteger tail;
    private int size = 0;

    /**
     * Create a new {@link RingBuffer}.
     *
     * @param maxSize the size of the internal array to store the data. Once it is full, the oldest element will be
     *                overwritten.
     */
    public RingBuffer(int maxSize)
    {
        this.maxSize = maxSize;
        this.array = (T[]) new Object[maxSize];
        this.head = new ModularInteger(0, maxSize);
        this.tail = new ModularInteger(0, maxSize);
    }

    public RingBuffer(int maxSize, T defaultObject)
    {
        this(maxSize);
        Arrays.fill(array, defaultObject);
    }

    @Override
    public boolean add(T elem)
    {
        if (this.size == maxSize) this.onForget(array[tail.getAndIncrement()]);
        else ++this.size;

        this.array[head.getAndIncrement()] = elem;
        return true;
    }

    protected void onForget(T t)
    {
        // This can be extended by subclasses to listen to overwritten elements.
    }

    @Override
    public boolean remove(Object o)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c)
    {
        throw new UnsupportedOperationException();
    }

    public T head()
    {
        return this.array[ModularInteger.decrement(head.get(), maxSize)];
    }

    public T tail()
    {
        return this.array[tail.get()];
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public boolean contains(Object o)
    {
        for (T t : this.array) {
            if (t.equals(o)) return true;
        }
        return false;
    }

    @Override
    public void clear()
    {
        // We don't need to specifically clear the array as the values will be overwritten anyway.
        this.head.setToZero();
        this.tail.setToZero();
        this.size = 0;
    }

    @NotNull
    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<>()
        {
            private int index = tail.get();
            // An element counter to limit the iterated elements.
            private int elements = 0;

            @Override
            public boolean hasNext()
            {
                return elements < size;
            }

            @Override
            public T next()
            {
                if (!hasNext()) throw new NoSuchElementException();
                T elem = array[index];
                index = ModularInteger.increment(index, maxSize);
                ++elements;
                return elem;
            }
        };
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray()
    {
        final var elements = new Object[this.size];
        int i = 0;
        for (T t : this) elements[i++] = t;
        return elements;
    }

    @NotNull
    @Override
    public <T1> T1 @NotNull [] toArray(T1[] a)
    {
        final var elements = a.length < size ? (T1[]) Array.newInstance(a.getClass().getComponentType(), size) : a;
        int i = 0;
        for (T t : this) elements[i++] = (T1) t;
        if (a.length > size) elements[size - 1] = null;
        return elements;
    }

    public Iterator<T> descendingIterator()
    {
        return new Iterator<>()
        {
            // Start at head - 1 as head is the position at which will be written next, but right now there is no
            // element there, or the oldest element.
            private int index = ModularInteger.decrement(head.get(), maxSize);
            // An element counter to limit the iterated elements.
            private int elements = 0;

            @Override
            public boolean hasNext()
            {
                return elements < size;
            }

            @Override
            public T next()
            {
                if (!hasNext()) throw new NoSuchElementException();
                T elem = array[index];
                index = ModularInteger.decrement(index, maxSize);
                ++elements;
                return elem;
            }
        };
    }
}
