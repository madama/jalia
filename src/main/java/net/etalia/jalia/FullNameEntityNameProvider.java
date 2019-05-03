package net.etalia.jalia;

public class FullNameEntityNameProvider implements EntityNameProvider {

    @Override
    public String getEntityName(Class<?> clazz) {
        return clazz.getName();
    }

    @Override
    public Class<?> getEntityClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot find class '" + name + "'", e);
        }
    }
}
