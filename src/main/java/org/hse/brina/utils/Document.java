package org.hse.brina.utils;

import org.hse.brina.Config;

public class Document {
    private final StringBuilder name;
    private final StringBuilder image = new StringBuilder(Config.getPathToAssets()+"unlocked.png");
    private final String access;
    private STATUS status;

    public Document(String access, String name, STATUS status) {
        this.access = access;
        this.name = new StringBuilder(name);
        this.status = status;
    }

    public String getName() {
        return name.toString();
    }

    public void setName(String newName) {
        name.replace(0, name.length(), newName);
    }

    public String getAccess() {
        return access;
    }

    public String getImage() {
        return image.toString();
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(String statusValue) {
        if (statusValue.equals("unlocked")) {
            status = STATUS.UNLOCKED;
        } else {
            status = STATUS.LOCKED;
        }
    }

    public enum STATUS {
        LOCKED,
        UNLOCKED
    }
}
