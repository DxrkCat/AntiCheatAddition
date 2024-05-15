package de.photon.anticheataddition.util.reflection;

import de.photon.anticheataddition.util.log.Log;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

public record FieldReflect(Field field)
{
    public TempValueReflect from(Object obj)
    {
        return new TempValueReflect(field, obj);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    public static class TempValueReflect
    {
        private final Field field;
        private final Object obj;

        public byte[] asBytes()
        {
            return (byte[]) get();
        }

        public double asDouble()
        {
            try {
                return this.field.getDouble(obj);
            } catch (IllegalAccessException e) {
                Log.error("Unable to get field as double via reflection", e);
            }

            return 0;
        }

        public <T> T as(Class<T> clazz)
        {
            return (T) get();
        }

        public <T> List<T> asList(Class<T> clazz)
        {
            return (List<T>) get();
        }

        public <T> Set<T> asSet(Class<T> clazz)
        {
            return (Set<T>) get();
        }

        public Object get()
        {
            try {
                return this.field.get(obj);
            } catch (IllegalAccessException e) {
                Log.error("Unable to get field via reflection", e);
            }

            return null;
        }
    }
}
