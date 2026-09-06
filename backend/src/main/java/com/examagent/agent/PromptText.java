package com.examagent.agent;

/** Small shared helper for formatting prompt text - used by every agent that builds a bullet list from lecture knowledge. */
final class PromptText {

    private PromptText() {
    }

    static String bulletList(Iterable<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("- ").append(item).append("\n");
        }
        return sb.isEmpty() ? "(none provided)" : sb.toString();
    }
}
