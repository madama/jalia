package net.etalia.jalia;

import java.util.*;

public class ChangeRecorder {

    private static final ChangeRecorder sharedInstance = new ChangeRecorder();

    public static ChangeRecorder getSharedInstance() {
        return sharedInstance;
    }

    private WeakHashMap<Object, Map<String,Change<Object>>> changes = new WeakHashMap<>();

    public void recordBeanChange(Object bean, String property, Object oldValue, Object newValue) {
        Map<String, Change<Object>> map = changes.get(bean);
        if (map == null) {
            map = new HashMap<>();
            changes.put(bean, map);
        }
        map.put(property, new Change(property, oldValue, newValue));
    }

    public Collection<Change<Object>> getChanges(Object bean) {
        Map<String, Change<Object>> map = changes.get(bean);
        if (map == null) {
            return Collections.EMPTY_LIST;
        }
        return Collections.unmodifiableCollection(map.values());
    }

    public <T> Change<T> getChange(Object bean, String property) {
        Map<String, Change<Object>> map = changes.get(bean);
        if (map == null) {
            return null;
        }
        return (Change<T>)map.get(property);
    }

    public static class Change<T> {
        private final String field;
        private final T oldValue;
        private final T newValue;


        public Change(String field, T oldValue, T newValue) {
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getField() {
            return field;
        }

        public T getOldValue() {
            return oldValue;
        }

        public T getNewValue() {
            return newValue;
        }
    }
}
