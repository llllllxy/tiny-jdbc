package org.tinycloud.jdbc.annotation;


public enum IdType {
    AUTO_INCREMENT(0),
    INPUT(1),
    OBJECT_ID(2),
    ASSIGN_ID(3),
    UUID(4);

    private final int key;

    private IdType(int key) {
        this.key = key;
    }

    public int getKey() {
        return this.key;
    }
}
