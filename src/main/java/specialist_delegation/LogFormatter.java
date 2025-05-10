package specialist_delegation;

import java.util.logging.LogRecord;

public class LogFormatter extends java.util.logging.Formatter {

	@Override
    public String format(LogRecord rec) {
        return String.format("%s: %s%n",
            rec.getLevel(), rec.getMessage());
    }

}
