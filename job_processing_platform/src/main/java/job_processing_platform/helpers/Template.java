package job_processing_platform.helpers;

public final class Template {

    public static String format(String template, Object... args) {
        for (Object arg : args) {
            template = template.replaceFirst("\\{}",
                    arg == null ? "null" : arg.toString());
        }
        return template;
    }
}