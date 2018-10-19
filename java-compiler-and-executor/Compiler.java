/**
 * Compiles Java code. The compiled class file will be at the following path: {@code java/code/Code.class}
 */
public class JavaExecuteCompiler implements ExecuteCompiler {
	private static final String TEMPLATE_FIRST = "package code; import java.util.*; public class Code {" +
			"public static void main(String[] args) { suppliedCode(); } public static void suppliedCode() {";
	private static final String TEMPLATE_SECOND = "}}";
	
	private static final Logger LOGGER = new Logger(JavaExecuteCompiler.class);
	private final File folder = new File("execute");
	private final File sourceFile = new File(folder, "Code.java");
	
	
	
	@Nullable
	@Override
	public Result compile(String code) {
		long time = System.currentTimeMillis();
		if (!isBraceCountValid(code)) {
			return new Result("More braces were closed than were opened.", time);
		}
		
		try (Writer writer = new FileWriter(sourceFile)) {
			writer.write(TEMPLATE_FIRST + code + TEMPLATE_SECOND);
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error writing source file", e);
			return null;
		}
		
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
		StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, Locale.ENGLISH, Charsets.UTF_8);
		
		boolean success = compiler.getTask(null, fileManager, diagnostics, Arrays.asList("-d", folder.getAbsolutePath()),
				null, fileManager.getJavaFileObjectsFromFiles(Collections.singletonList(sourceFile))).call();
		
		try {
			fileManager.close();
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error while closing the file manager", e);
			return null;
		}
		
		if (success) {
			return new Result(null, time);
		}
		
		StringBuilder errors = new StringBuilder();
		for (Iterator<Diagnostic<? extends JavaFileObject>> iterator = diagnostics.getDiagnostics().iterator(); iterator.hasNext(); ) {
			Diagnostic<? extends JavaFileObject> error = iterator.next();
			errors.append("Error on line ").append(error.getLineNumber()).append(" at column ")
					.append(error.getColumnNumber() == -1 ? "?" :
							error.getColumnNumber()).append(": ").append(error.getMessage(Locale.ENGLISH));
			if (iterator.hasNext()) {
				errors.append(System.lineSeparator());
			}
		}
		
		return new Result(errors.toString(), time);
	}
	
	
	
	private boolean isBraceCountValid(String code) {
		int braces = 0;
		for (int i = 0; i < code.length(); i++) {
			char character = code.charAt(i);
			if (character == '{') {
				braces++;
			} else if (character == '}') {
				braces--;
				if (braces < 0) {
					return false;
				}
			}
		}
		return true;
	}
}
