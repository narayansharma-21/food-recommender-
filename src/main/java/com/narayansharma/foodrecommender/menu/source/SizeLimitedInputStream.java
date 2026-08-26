package com.narayansharma.foodrecommender.menu.source;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

final class SizeLimitedInputStream extends FilterInputStream {
	private long remaining;

	SizeLimitedInputStream(InputStream input, long maximumBytes) {
		super(input);
		this.remaining = maximumBytes;
	}

	@Override
	public int read() throws IOException {
		if (remaining == 0) {
			if (super.read() == -1) {
				return -1;
			}
			throw new MenuImageTooLargeException();
		}
		int value = super.read();
		if (value != -1) {
			remaining--;
		}
		return value;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (length == 0) {
			return 0;
		}
		if (remaining == 0) {
			return read();
		}
		int count = super.read(buffer, offset, (int) Math.min(length, remaining));
		if (count > 0) {
			remaining -= count;
		}
		return count;
	}

	static final class MenuImageTooLargeException extends IOException {
		private static final long serialVersionUID = 1L;
	}
}
