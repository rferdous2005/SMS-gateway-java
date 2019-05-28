import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogWrapper {
	private static final int LOG_IN_FILE_ENABLED = 1;
	private static final int LOG_IN_SCREEN_ENABLED = 2;
	private Logger logger;
	private String loggedClassName;

	public LogWrapper(String loggedClassName) {
		this.loggedClassName = loggedClassName;
		this.logger = Logger.getLogger(loggedClassName);
		this.setLogLevel();
	}

	public Logger getLogger() {
		return this.logger;
	}

	public String getLoggedClassName() {
		return this.loggedClassName;
	}

	public void debug(String message) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.debug(message);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
		}
	}

	public void debug(Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.debug(this.getStackTrace(exception));
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(this.getStackTrace(exception));
		}
	}

	public void debug(String message, Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.debug(message, exception);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
			exception.printStackTrace();
		}
	}

	public void error(String message) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.error(message);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
		}
	}

	public void error(Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.error(this.getStackTrace(exception));
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(this.getStackTrace(exception));
		}
	}

	public void error(String message, Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.error(message, exception);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
			exception.printStackTrace();
		}
	}

	public void info(String message) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.info(message);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
		}
	}

	public void info(Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.info(this.getStackTrace(exception));
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(this.getStackTrace(exception));
		}
	}

	public void info(String message, Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.info(message, exception);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
			exception.printStackTrace();
		}
	}

	public void warn(String message) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.warn(message);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
		}
	}

	public void warn(Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.warn(this.getStackTrace(exception));
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(this.getStackTrace(exception));
		}
	}

	public void warn(String message, Throwable exception) {
		if(AppConstant.LogEnable == LOG_IN_FILE_ENABLED) {
			this.logger.warn(message, exception);
		} else if(AppConstant.LogEnable == LOG_IN_SCREEN_ENABLED) {
			System.out.println(message);
			exception.printStackTrace();
		}
	}

	private void setLogLevel() {
		switch (AppConstant.LogLevel) {
			case 0:
				this.logger.setLevel(Level.OFF);
				break;
			case 1:
				this.logger.setLevel(Level.FATAL);
				break;
			case 2:
				this.logger.setLevel(Level.ERROR);
				break;
			case 3:
				this.logger.setLevel(Level.WARN);
				break;
			case 4:
				this.logger.setLevel(Level.INFO);
				break;
			case 5:
				this.logger.setLevel(Level.DEBUG);
				break;
			case 6:
				this.logger.setLevel(Level.ALL);
				break;
			default:
				this.logger.setLevel(Level.DEBUG);
				break;
		}
	}

	private String getStackTrace(Throwable throwable) {
		StringWriter writer = new StringWriter();
		PrintWriter printWriter = new PrintWriter(writer);
		throwable.printStackTrace(printWriter);
		return writer.toString();
	}
}
