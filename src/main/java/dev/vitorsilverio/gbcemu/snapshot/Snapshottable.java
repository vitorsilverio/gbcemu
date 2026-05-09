package dev.vitorsilverio.gbcemu.snapshot;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

public interface Snapshottable {



    default Snapshot createSnapshot(int version) {
        Map<String, Object> state = new HashMap<>();
        for (Field field : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Savable.class)) {
                Savable ann = field.getAnnotation(Savable.class);
                if (version >= ann.sinceVersion()) {
                    field.setAccessible(true);
                    try {
                        Object value = field.get(this);
                        state.put(field.getName(), cloneValue(value));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return new Snapshot(this.getClass().getName(), version, state);
    }

    default void restoreSnapshot(Snapshot snapshot) {
        for (Field field : this.getClass().getDeclaredFields()) {
            if (snapshot.state().containsKey(field.getName())) {
                field.setAccessible(true);
                try {
                    Object newValue = snapshot.state().get(field.getName());
                    Object currentValue = field.get(this);

                    if (field.getType().isArray() && newValue != null && newValue.getClass().isArray()) {
                        deepArrayCopy(newValue, currentValue);
                    } else if (currentValue instanceof Collection && newValue instanceof Collection) {
                        Collection<Object> currentColl = (Collection<Object>) currentValue;
                        currentColl.clear();
                        currentColl.addAll((Collection<?>) newValue);
                    }
                    else if (currentValue instanceof Map && newValue instanceof Map) {
                        Map<Object, Object> currentMap = (Map<Object, Object>) currentValue;
                        currentMap.clear();
                        currentMap.putAll((Map<?, ?>) newValue);
                    }
                    else {
                        field.set(this, newValue);
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Object cloneValue(Object value) {
        if (value instanceof List) return new ArrayList<>((List<?>) value);
        if (value instanceof Set) return new HashSet<>((Set<?>) value);
        if (value instanceof Map) return new HashMap<>((Map<?, ?>) value);
        if (value.getClass().isArray()) {
            return deepArrayClone(value);
        }
        return value;
    }

    private void deepArrayCopy(Object source, Object destination) {
        int length = Array.getLength(source);
        if (length != Array.getLength(destination)) {
            throw new IllegalStateException("Tamanho do array mudou, impossível restaurar em campo final.");
        }

        for (int i = 0; i < length; i++) {
            Object sourceElement = Array.get(source, i);
            if (sourceElement != null && sourceElement.getClass().isArray()) {
                // Se for um array dentro de outro (multidimensão), chama recursivo
                deepArrayCopy(sourceElement, Array.get(destination, i));
            } else {
                // Se for o nível final (valor), apenas atribui
                Array.set(destination, i, sourceElement);
            }
        }
    }

    private Object deepArrayClone(Object array) {
        int length = Array.getLength(array);
        Object copy = Array.newInstance(array.getClass().getComponentType(), length);

        for (int i = 0; i < length; i++) {
            Object element = Array.get(array, i);
            if (element != null && element.getClass().isArray()) {
                Array.set(copy, i, deepArrayClone(element));
            } else {
                Array.set(copy, i, element);
            }
        }
        return copy;
    }


}
