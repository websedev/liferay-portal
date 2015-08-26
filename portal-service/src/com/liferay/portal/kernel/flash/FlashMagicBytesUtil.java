/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.kernel.flash;

import com.liferay.portal.kernel.util.ArrayUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * @author Brian Wing Shun Chan
 * @author Mika Koivisto
 */
public class FlashMagicBytesUtil {

	public static FlashMagicBytesUtilResult check(InputStream inputStream)
		throws IOException {

		PushbackInputStream pushbackInputStream = new PushbackInputStream(
			inputStream, 3);

		byte[] magicBytes = new byte[3];

		int length = pushbackInputStream.read(magicBytes);

		if (length < 0) {
			return new FlashMagicBytesUtilResult(false, inputStream);
		}

		pushbackInputStream.unread(magicBytes, 0, length);

		inputStream = pushbackInputStream;

		if (ArrayUtil.containsAll(_FLASH_MAGIC_BYTES[0], magicBytes) ||
			ArrayUtil.containsAll(_FLASH_MAGIC_BYTES[1], magicBytes) ||
			ArrayUtil.containsAll(_FLASH_MAGIC_BYTES[2], magicBytes)) {

			return new FlashMagicBytesUtilResult(true, inputStream);
		}

		return new FlashMagicBytesUtilResult(false, inputStream);
	}


	private static final byte[][] _FLASH_MAGIC_BYTES =
		{{0x46, 0x57, 0x53}, {0x43, 0x57, 0x53}, {0x5a, 0x57, 0x53}};

}