package com.narayansharma.foodrecommender.menu.extraction.ocr.local;

import com.narayansharma.foodrecommender.menu.extraction.ocr.OcrException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class TesseractProcessRunner implements TesseractRunner {
	private static final int MAXIMUM_OUTPUT_BYTES = 5 * 1024 * 1024;
	private final String executable;
	private final String language;
	private final Duration timeout;
	private final TesseractTsvParser parser;

	TesseractProcessRunner(
			@Value("${ocr.tesseract.executable:tesseract}") String executable,
			@Value("${ocr.tesseract.language:eng}") String language,
			@Value("${ocr.tesseract.timeout:PT30S}") Duration timeout,
			TesseractTsvParser parser) {
		if (executable == null || executable.isBlank()) {
			throw new IllegalArgumentException("Tesseract executable is required");
		}
		if (language == null || !language.matches("[a-z]{3}(\\+[a-z]{3})*")) {
			throw new IllegalArgumentException("Tesseract language is invalid");
		}
		if (timeout == null || timeout.isNegative() || timeout.isZero() || timeout.compareTo(Duration.ofMinutes(2)) > 0) {
			throw new IllegalArgumentException("Tesseract timeout is invalid");
		}
		this.executable = executable;
		this.language = language;
		this.timeout = timeout;
		this.parser = parser;
	}

	@Override
	public TesseractOutput extract(byte[] image) {
		Process process = startProcess();
		try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
			Future<byte[]> standardOutput = executor.submit(() -> readLimited(process.getInputStream()));
			Future<byte[]> errorOutput = executor.submit(() -> readLimited(process.getErrorStream()));
			Future<Void> inputWriter = executor.submit(() -> {
				try (var input = process.getOutputStream()) {
					input.write(image);
				}
				return null;
			});
			if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
				throw new OcrException("Local OCR timed out");
			}
			inputWriter.get(5, TimeUnit.SECONDS);
			byte[] output = standardOutput.get(5, TimeUnit.SECONDS);
			errorOutput.get(5, TimeUnit.SECONDS);
			if (process.exitValue() != 0) {
				throw new OcrException("Local OCR command failed with exit code " + process.exitValue());
			}
			return parser.parse(new String(output, java.nio.charset.StandardCharsets.UTF_8));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			process.destroyForcibly();
			throw new OcrException("Local OCR was interrupted", exception);
		} catch (ExecutionException | TimeoutException exception) {
			process.destroyForcibly();
			throw new OcrException("Local OCR could not complete", exception);
		}
	}

	private Process startProcess() {
		try {
			return new ProcessBuilder(List.of(executable, "stdin", "stdout", "-l", language, "tsv")).start();
		} catch (IOException exception) {
			throw new OcrException("Local OCR executable is unavailable", exception);
		}
	}

	private byte[] readLimited(InputStream input) throws IOException {
		byte[] output = input.readNBytes(MAXIMUM_OUTPUT_BYTES + 1);
		if (output.length > MAXIMUM_OUTPUT_BYTES) {
			throw new IOException("Local OCR output exceeded its limit");
		}
		return output;
	}
}
