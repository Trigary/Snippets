/**
 * Executes class files in a new JVM. The file has to have an entry point and it has to be
 * at the following directory: {@code java/code/Code.class}<br>
 * Information about policies:
 * <a href=https://docs.oracle.com/javase/8/docs/technotes/guides/security/PolicyFiles.html>guide</a>,
 * <a href=https://docs.oracle.com/javase/8/docs/technotes/guides/security/permissions.html>list</a>
 */
public class JvmExecuteProcess implements ExecuteProcess {
	private static final int MAX_MEMORY = 64;
	private static final int TIMEOUT_SECONDS = 10;
	private static final int MAX_CHARS = 1024;
	
	private static final Logger LOGGER = new Logger(JvmExecuteProcess.class);
	private final File folder = new File("execute");
	private final ProcessBuilder processBuilder = new ProcessBuilder("java", "-cp", folder.getAbsolutePath(), "-Xmx" + MAX_MEMORY + "M",
			"-Djava.security.manager", "-Djava.security.policy=" + folder.getAbsolutePath() + File.separator + "code.policy", "code.Code")
			.directory(folder)
			.redirectErrorStream(true);
	
	public JvmExecuteProcess() throws IOException {
		Files.write(Resources.toByteArray(getClass().getResource("/code.policy")), new File(folder, "code.policy"));
	}
	
	
	
	@Nullable
	@Override
	public Result execute() {
		try {
			long time = System.currentTimeMillis();
			Process process = processBuilder.start();
			if (process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				return new Result(readCharacters(process.getInputStream()), time);
			} else {
				return new Result(null, time);
			}
		} catch (IOException e) {
			LOGGER.log(Level.SEVERE, "Error while starting or reading the output of the process", e);
			return null;
		} catch (InterruptedException e) {
			LOGGER.log(Level.SEVERE, "Got interrupted while waiting for process to finish", e);
			return null;
		}
	}
	
	
	
	private String readCharacters(InputStream stream) throws IOException {
		StringBuilder builder = new StringBuilder();
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charsets.UTF_8));
		String line;
		while ((line = reader.readLine()) != null) {
			if (builder.length() + line.length() < MAX_CHARS) {
				builder.append(line);
				if (builder.length() < MAX_CHARS) {
					builder.append('\n');
				} else {
					break;
				}
			} else if (builder.length() < MAX_CHARS) {
				builder.append(line.isEmpty() ? '\n' : line.substring(0, MAX_CHARS - builder.length()));
				break;
			} else {
				break;
			}
		}
		return StringUtils.strip(builder.toString());
	}
}
