package org.omni.placeholder;

public enum PlaceholderFormat {
    BRACKET {
        @Override
        public String format(String prefix, int id) {
            return "{" + prefix + id + "}";
        }
    },
    HTML {
        @Override
        public String format(String prefix, int id) {
            return "<" + prefix + id + ">";
        }
    };

    public abstract String format(String prefix, int id);
}