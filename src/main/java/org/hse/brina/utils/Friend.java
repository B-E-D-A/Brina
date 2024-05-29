package org.hse.brina.utils;

/**
 * Класс Friend представляет пару имени пользователя и его доступа к документу.
 */
public class Friend {
    private final StringBuilder name;
    private final StringBuilder access;
    public Friend(String access, String name) {
        this.access = new StringBuilder(access);
        this.name = new StringBuilder(name);
    }

    public String getName() {
        return name.toString();
    }

    public void setName(String newName) {
        name.replace(0, name.length(), newName);
    }

    public String getAccess() {
        return access.toString();
    }

    private void setAccess(String accessLevel) {
        access.replace(0, access.length(), accessLevel);
    }

    public void setReaderCheckBox(Boolean newValue) {
        if (newValue) {
            setAccess("r");
        } else {
            setAccess("none");
        }
    }

    public void setWriterCheckBox(Boolean newValue) {
        if (newValue) {
            setAccess("w");
        } else {
            setAccess("none");
        }
    }
}
