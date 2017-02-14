package util;

/**
 * Class full name: package.ClassName
 */
public class FullName implements Comparable<FullName> {
    public final String packageName;
    public final String className;

    public FullName(String fullClassName) {
        int pos = fullClassName.lastIndexOf(".");
        if (pos == -1) {
            packageName = "";
            className = fullClassName;
        } else {
            packageName = fullClassName.substring(0, pos);
            className = fullClassName.substring(pos + 1);
        }
    }

    @Override
    public String toString() {
        if (packageName.length() > 0)
            return String.format("%s.%s", packageName, className);
        else
            return className;
    }

    @Override
    public int compareTo(FullName another) {
        return this.className.compareTo(another.className);
    }
}
