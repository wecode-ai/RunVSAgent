import * as fs from "fs"
import * as fsPromises from "fs/promises"
import * as iconv from "iconv-lite"
import * as jschardet from "jschardet"

// BOM (Byte Order Mark) constants
const UTF8_BOM = [0xef, 0xbb, 0xbf]
const UTF16BE_BOM = [0xfe, 0xff]
const UTF16LE_BOM = [0xff, 0xfe]

// Encoding constants
const UTF8 = "utf8"
const UTF8_WITH_BOM = "utf8bom"
const UTF16BE = "utf16be"
const UTF16LE = "utf16le"

// Thresholds for detection
const ZERO_BYTE_DETECTION_BUFFER_MAX_LEN = 512 // For binary detection
const AUTO_ENCODING_GUESS_MIN_BYTES = 512 * 8 // Minimum bytes for encoding guess
const AUTO_ENCODING_GUESS_MAX_BYTES = 512 * 128 // Maximum bytes for encoding guess

// Supported encodings map (matches encoding.ts)
export const SUPPORTED_ENCODINGS = {
	utf8: {
		labelLong: "UTF-8",
		labelShort: "UTF-8",
		order: 1,
		alias: "utf8bom",
		guessableName: "UTF-8",
	},
	utf8bom: {
		labelLong: "UTF-8 with BOM",
		labelShort: "UTF-8 with BOM",
		encodeOnly: true,
		order: 2,
		alias: "utf8",
	},
	utf16le: {
		labelLong: "UTF-16 LE",
		labelShort: "UTF-16 LE",
		order: 3,
		guessableName: "UTF-16LE",
	},
	utf16be: {
		labelLong: "UTF-16 BE",
		labelShort: "UTF-16 BE",
		order: 4,
		guessableName: "UTF-16BE",
	},
	windows1252: {
		labelLong: "Western (Windows 1252)",
		labelShort: "Windows 1252",
		order: 5,
		guessableName: "windows-1252",
	},
	iso88591: {
		labelLong: "Western (ISO 8859-1)",
		labelShort: "ISO 8859-1",
		order: 6,
	},
	iso88593: {
		labelLong: "Western (ISO 8859-3)",
		labelShort: "ISO 8859-3",
		order: 7,
	},
	iso885915: {
		labelLong: "Western (ISO 8859-15)",
		labelShort: "ISO 8859-15",
		order: 8,
	},
	macroman: {
		labelLong: "Western (Mac Roman)",
		labelShort: "Mac Roman",
		order: 9,
	},
	cp437: {
		labelLong: "DOS (CP 437)",
		labelShort: "CP437",
		order: 10,
	},
	windows1256: {
		labelLong: "Arabic (Windows 1256)",
		labelShort: "Windows 1256",
		order: 11,
	},
	iso88596: {
		labelLong: "Arabic (ISO 8859-6)",
		labelShort: "ISO 8859-6",
		order: 12,
	},
	windows1257: {
		labelLong: "Baltic (Windows 1257)",
		labelShort: "Windows 1257",
		order: 13,
	},
	iso88594: {
		labelLong: "Baltic (ISO 8859-4)",
		labelShort: "ISO 8859-4",
		order: 14,
	},
	iso885914: {
		labelLong: "Celtic (ISO 8859-14)",
		labelShort: "ISO 8859-14",
		order: 15,
	},
	windows1250: {
		labelLong: "Central European (Windows 1250)",
		labelShort: "Windows 1250",
		order: 16,
		guessableName: "windows-1250",
	},
	iso88592: {
		labelLong: "Central European (ISO 8859-2)",
		labelShort: "ISO 8859-2",
		order: 17,
		guessableName: "ISO-8859-2",
	},
	cp852: {
		labelLong: "Central European (CP 852)",
		labelShort: "CP 852",
		order: 18,
	},
	windows1251: {
		labelLong: "Cyrillic (Windows 1251)",
		labelShort: "Windows 1251",
		order: 19,
		guessableName: "windows-1251",
	},
	cp866: {
		labelLong: "Cyrillic (CP 866)",
		labelShort: "CP 866",
		order: 20,
		guessableName: "IBM866",
	},
	cp1125: {
		labelLong: "Cyrillic (CP 1125)",
		labelShort: "CP 1125",
		order: 21,
		guessableName: "IBM1125",
	},
	iso88595: {
		labelLong: "Cyrillic (ISO 8859-5)",
		labelShort: "ISO 8859-5",
		order: 22,
		guessableName: "ISO-8859-5",
	},
	koi8r: {
		labelLong: "Cyrillic (KOI8-R)",
		labelShort: "KOI8-R",
		order: 23,
		guessableName: "KOI8-R",
	},
	koi8u: {
		labelLong: "Cyrillic (KOI8-U)",
		labelShort: "KOI8-U",
		order: 24,
	},
	iso885913: {
		labelLong: "Estonian (ISO 8859-13)",
		labelShort: "ISO 8859-13",
		order: 25,
	},
	windows1253: {
		labelLong: "Greek (Windows 1253)",
		labelShort: "Windows 1253",
		order: 26,
		guessableName: "windows-1253",
	},
	iso88597: {
		labelLong: "Greek (ISO 8859-7)",
		labelShort: "ISO 8859-7",
		order: 27,
		guessableName: "ISO-8859-7",
	},
	windows1255: {
		labelLong: "Hebrew (Windows 1255)",
		labelShort: "Windows 1255",
		order: 28,
		guessableName: "windows-1255",
	},
	iso88598: {
		labelLong: "Hebrew (ISO 8859-8)",
		labelShort: "ISO 8859-8",
		order: 29,
		guessableName: "ISO-8859-8",
	},
	iso885910: {
		labelLong: "Nordic (ISO 8859-10)",
		labelShort: "ISO 8859-10",
		order: 30,
	},
	iso885916: {
		labelLong: "Romanian (ISO 8859-16)",
		labelShort: "ISO 8859-16",
		order: 31,
	},
	windows1254: {
		labelLong: "Turkish (Windows 1254)",
		labelShort: "Windows 1254",
		order: 32,
	},
	iso88599: {
		labelLong: "Turkish (ISO 8859-9)",
		labelShort: "ISO 8859-9",
		order: 33,
	},
	windows1258: {
		labelLong: "Vietnamese (Windows 1258)",
		labelShort: "Windows 1258",
		order: 34,
	},
	gbk: {
		labelLong: "Simplified Chinese (GBK)",
		labelShort: "GBK",
		order: 35,
	},
	gb18030: {
		labelLong: "Simplified Chinese (GB18030)",
		labelShort: "GB18030",
		order: 36,
	},
	cp950: {
		labelLong: "Traditional Chinese (Big5)",
		labelShort: "Big5",
		order: 37,
		guessableName: "Big5",
	},
	big5hkscs: {
		labelLong: "Traditional Chinese (Big5-HKSCS)",
		labelShort: "Big5-HKSCS",
		order: 38,
	},
	shiftjis: {
		labelLong: "Japanese (Shift JIS)",
		labelShort: "Shift JIS",
		order: 39,
		guessableName: "SHIFT_JIS",
	},
	eucjp: {
		labelLong: "Japanese (EUC-JP)",
		labelShort: "EUC-JP",
		order: 40,
		guessableName: "EUC-JP",
	},
	euckr: {
		labelLong: "Korean (EUC-KR)",
		labelShort: "EUC-KR",
		order: 41,
		guessableName: "EUC-KR",
	},
	windows874: {
		labelLong: "Thai (Windows 874)",
		labelShort: "Windows 874",
		order: 42,
	},
	iso885911: {
		labelLong: "Latin/Thai (ISO 8859-11)",
		labelShort: "ISO 8859-11",
		order: 43,
	},
	koi8ru: {
		labelLong: "Cyrillic (KOI8-RU)",
		labelShort: "KOI8-RU",
		order: 44,
	},
	koi8t: {
		labelLong: "Tajik (KOI8-T)",
		labelShort: "KOI8-T",
		order: 45,
	},
	gb2312: {
		labelLong: "Simplified Chinese (GB 2312)",
		labelShort: "GB 2312",
		order: 46,
		guessableName: "GB2312",
	},
	cp865: {
		labelLong: "Nordic DOS (CP 865)",
		labelShort: "CP 865",
		order: 47,
	},
	cp850: {
		labelLong: "Western European DOS (CP 850)",
		labelShort: "CP 850",
		order: 48,
	},
}

// Create a map of guessable encodings
export const GUESSABLE_ENCODINGS = (() => {
	const guessableEncodings: any = {}
	for (const encoding in SUPPORTED_ENCODINGS) {
		if ((SUPPORTED_ENCODINGS as any)[encoding].guessableName) {
			guessableEncodings[encoding] = (SUPPORTED_ENCODINGS as any)[encoding]
		}
	}
	return guessableEncodings
})()

// Encodings we explicitly ignore from auto guessing
const IGNORE_ENCODINGS = ["ascii", "utf-16", "utf-32"]

/**
 * Detects encoding by BOM (Byte Order Mark)
 * @param buffer - File content buffer
 * @returns Detected encoding or null if no BOM found
 */
function detectEncodingByBOM(buffer: Buffer | null, bytesRead: number): string | null {
	if (!buffer || bytesRead < UTF16BE_BOM.length) {
		return null
	}

	const b0 = buffer[0]
	const b1 = buffer[1]

	// UTF-16 BE
	if (b0 === UTF16BE_BOM[0] && b1 === UTF16BE_BOM[1]) {
		return UTF16BE
	}

	// UTF-16 LE
	if (b0 === UTF16LE_BOM[0] && b1 === UTF16LE_BOM[1]) {
		return UTF16LE
	}

	if (bytesRead < UTF8_BOM.length) {
		return null
	}

	const b2 = buffer[2]

	// UTF-8 with BOM
	if (b0 === UTF8_BOM[0] && b1 === UTF8_BOM[1] && b2 === UTF8_BOM[2]) {
		return UTF8_WITH_BOM
	}

	return null
}

/**
 * Checks if buffer seems to be binary by looking for zero bytes
 * @param buffer - File content buffer
 * @param bytesRead - Number of bytes read from the buffer
 * @returns Object with binary detection and encoding if UTF-16 is detected
 */
function checkBinaryAndUTF16(buffer: Buffer, bytesRead: number): { seemsBinary: boolean; encoding: string | null } {
	let seemsBinary = false
	let encoding: string | null = null

	// Detect 0 bytes to see if file is binary or UTF-16 LE/BE
	if (buffer) {
		let couldBeUTF16LE = true // e.g. 0xAA 0x00
		let couldBeUTF16BE = true // e.g. 0x00 0xAA
		let containsZeroByte = false

		// This is a simplified guess to detect UTF-16 BE or LE by just checking if
		// the first 512 bytes have the 0-byte at a specific location. For UTF-16 LE
		// this would be the odd byte index and for UTF-16 BE the even one.
		for (let i = 0; i < bytesRead && i < ZERO_BYTE_DETECTION_BUFFER_MAX_LEN; i++) {
			const isEndian = i % 2 === 1 // assume 2-byte sequences typical for UTF-16
			const isZeroByte = buffer[i] === 0

			if (isZeroByte) {
				containsZeroByte = true
			}

			// UTF-16 LE: expect e.g. 0xAA 0x00
			if (couldBeUTF16LE && ((isEndian && !isZeroByte) || (!isEndian && isZeroByte))) {
				couldBeUTF16LE = false
			}

			// UTF-16 BE: expect e.g. 0x00 0xAA
			if (couldBeUTF16BE && ((isEndian && isZeroByte) || (!isEndian && !isZeroByte))) {
				couldBeUTF16BE = false
			}

			// Return if this is neither UTF16-LE nor UTF16-BE and thus treat as binary
			if (isZeroByte && !couldBeUTF16LE && !couldBeUTF16BE) {
				seemsBinary = true
				break
			}
		}

		// Handle case of 0-byte included
		if (containsZeroByte) {
			if (couldBeUTF16LE) {
				encoding = UTF16LE
			} else if (couldBeUTF16BE) {
				encoding = UTF16BE
			} else {
				seemsBinary = true
			}
		}
	}

	return { seemsBinary, encoding }
}

/**
 * Guesses encoding from buffer using jschardet
 * @param buffer - File content buffer
 * @returns Guessed encoding or null if detection fails
 */
/**
 * Converts a buffer to a Latin1 string
 * @param buffer - Buffer to convert
 * @returns Latin1 string representation of the buffer
 */
function encodeLatin1(buffer: Uint8Array): string {
	let result = ""
	for (let i = 0; i < buffer.length; i++) {
		result += String.fromCharCode(buffer[i])
	}
	return result
}

async function guessEncodingByBuffer(buffer: Buffer): Promise<string | null> {
	if (!buffer || buffer.length === 0) {
		return null
	}

	// Ensure to limit buffer for guessing
	const limitedBuffer = buffer.slice(0, AUTO_ENCODING_GUESS_MAX_BYTES)

	try {
		// Convert buffer to binary string as jschardet expects
		const binaryString = encodeLatin1(limitedBuffer)
		const result = jschardet.detect(binaryString)

		if (!result || !result.encoding) {
			return null
		}

		const enc = result.encoding.toLowerCase()

		// Ignore some encodings
		if (IGNORE_ENCODINGS.includes(enc)) {
			return null
		}

		return enc
	} catch (error) {
		return null // jschardet throws for unknown encodings
	}
}

/**
 * Detects encoding from buffer using multiple strategies
 * @param buffer - File content buffer
 * @param bytesRead - Number of bytes read from the buffer
 * @param autoGuessEncoding - Whether to use jschardet for additional guessing
 * @returns Object with detected encoding and whether file seems binary
 */
async function detectEncodingFromBuffer(
	buffer: Buffer,
	bytesRead: number,
	autoGuessEncoding: boolean = true,
): Promise<{ encoding: string | null; seemsBinary: boolean }> {
	// Always first check for BOM to find out about encoding
	let encoding = detectEncodingByBOM(buffer, bytesRead)

	// Detect 0 bytes to see if file is binary or UTF-16 LE/BE
	// unless we already know that this file has a UTF-16 encoding
	let seemsBinary = false
	if (encoding !== UTF16BE && encoding !== UTF16LE) {
		const result = checkBinaryAndUTF16(buffer, bytesRead)
		seemsBinary = result.seemsBinary

		// If we detected UTF-16 through zero bytes, use that
		if (!encoding && result.encoding) {
			encoding = result.encoding
		}
	}

	// Auto guess encoding if configured
	if (autoGuessEncoding && !seemsBinary && !encoding && buffer) {
		encoding = await guessEncodingByBuffer(buffer)
	}

	return { seemsBinary, encoding }
}

/**
 * Creates a decoder for the specified encoding
 */
class TextDecoder {
	private encoding: string
	private bomLength: number
	private remainingBytes: Buffer | null = null

	constructor(encoding: string) {
		this.encoding = encoding

		// Determine BOM length for skipping
		if (encoding === UTF8_WITH_BOM) {
			this.bomLength = 3
		} else if (encoding === UTF16BE || encoding === UTF16LE) {
			this.bomLength = 2
		} else {
			this.bomLength = 0
		}
	}

	decode(chunk: Buffer, isFirst: boolean = false, isLast: boolean = false): string {
		// Skip BOM on first chunk if needed
		let buffer = chunk
		if (isFirst && this.bomLength > 0) {
			buffer = chunk.slice(this.bomLength)
		}

		// Handle any remaining bytes from previous chunks
		if (this.remainingBytes) {
			buffer = Buffer.concat([this.remainingBytes, buffer])
			this.remainingBytes = null
		}

		// For multi-byte encodings, ensure we don't cut in the middle of a character
		if (!isLast && this.encoding !== UTF8 && this.encoding !== "utf-8") {
			// For UTF-16, ensure we have an even number of bytes
			if (this.encoding === UTF16BE || this.encoding === UTF16LE) {
				if (buffer.length % 2 !== 0) {
					this.remainingBytes = buffer.slice(buffer.length - 1)
					buffer = buffer.slice(0, buffer.length - 1)
				}
			}
		}

		// Decode the buffer
		try {
			if (this.encoding === UTF8 || this.encoding === "utf-8") {
				return buffer.toString("utf8")
			} else if (this.encoding === UTF8_WITH_BOM) {
				return buffer.toString("utf8")
			} else {
				return iconv.decode(buffer, this.encoding)
			}
		} catch (error) {
			// Fallback to UTF-8 if decoding fails
			return buffer.toString("utf8")
		}
	}
}

/**
 * Reads a file in chunks and detects its encoding
 * @param filePath - Path to the file
 * @param options - Reading options
 * @returns Buffer with enough bytes for encoding detection and detected encoding
 */
async function readFileForEncodingDetection(
	filePath: string,
	options: { autoGuessEncoding?: boolean } = {},
): Promise<{ buffer: Buffer; encoding: string | null; seemsBinary: boolean }> {
	return new Promise((resolve, reject) => {
		const detectionBuffer = Buffer.alloc(AUTO_ENCODING_GUESS_MIN_BYTES)
		let bytesRead = 0
		let fileStream: fs.ReadStream | null = null
		let isResolved = false

		// Ensure stream is properly closed in all cases
		const cleanup = () => {
			if (fileStream) {
				fileStream.removeAllListeners()
				fileStream.destroy()
				fileStream = null
			}
		}

		// Handle result with proper resource cleanup
		const handleResult = async (buffer: Buffer, bytesCount: number) => {
			if (isResolved) return

			try {
				const { encoding, seemsBinary } = await detectEncodingFromBuffer(
					buffer,
					bytesCount,
					options.autoGuessEncoding !== false,
				)

				isResolved = true
				resolve({
					buffer: buffer.slice(0, bytesCount),
					encoding,
					seemsBinary,
				})
			} catch (err) {
				cleanup()
				isResolved = true
				reject(err)
			}
		}

		try {
			fileStream = fs.createReadStream(filePath, {
				highWaterMark: 4096, // 4KB chunks
				start: 0,
				end: AUTO_ENCODING_GUESS_MIN_BYTES - 1,
			})

			fileStream.on("data", (chunk: Buffer | string) => {
				// Ensure chunk is a Buffer
				const bufferChunk = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk)
				const bytesToCopy = Math.min(bufferChunk.length, detectionBuffer.length - bytesRead)
				bufferChunk.copy(detectionBuffer, bytesRead, 0, bytesToCopy)
				bytesRead += bytesToCopy

				// If we've read enough bytes, close the stream
				if (bytesRead >= AUTO_ENCODING_GUESS_MIN_BYTES) {
					cleanup()
					handleResult(detectionBuffer.slice(0, bytesRead), bytesRead)
				}
			})

			fileStream.on("end", () => {
				if (!isResolved) {
					handleResult(detectionBuffer.slice(0, bytesRead), bytesRead)
				}
			})

			fileStream.on("error", (err) => {
				cleanup()
				if (!isResolved) {
					isResolved = true
					reject(err)
				}
			})
		} catch (err) {
			cleanup()
			reject(err)
		}
	})
}

/**
 * Reads file with automatic encoding detection and streaming decoding
 * Inspired by VSCode's encoding detection mechanism
 * @param filePath - Path to file
 * @param options - Options for reading file
 * @returns Decoded file content and detected encoding
 */
export async function readFileWithEncoding(
	filePath: string,
	options: {
		autoGuessEncoding?: boolean
		acceptTextOnly?: boolean
	} = {},
): Promise<{ content: string; encoding: string }> {
	// Get file stats to check size
	const stats = await fsPromises.stat(filePath)
	const fileSize = stats.size

	// For small files, use the original implementation for simplicity and performance
	if (fileSize <= AUTO_ENCODING_GUESS_MIN_BYTES) {
		const buffer = await fsPromises.readFile(filePath)
		const bytesRead = buffer.length

		// Detect encoding
		const { encoding: detectedEncoding, seemsBinary } = await detectEncodingFromBuffer(
			buffer,
			bytesRead,
			options.autoGuessEncoding !== false,
		)

		// If file seems binary and we only accept text, throw error
		if (seemsBinary && options.acceptTextOnly) {
			throw new Error(`File seems to be binary but only text is accepted: ${filePath}`)
		}

		// Decode the buffer based on detected encoding
		let content: string
		let finalEncoding = detectedEncoding || UTF8

		// Create a decoder to handle the content
		const decoder = new TextDecoder(finalEncoding)
		try {
			content = decoder.decode(buffer, true, true)
		} catch (error) {
			// Fallback to UTF-8 if decoding fails
			content = buffer.toString("utf8")
			finalEncoding = UTF8
		}

		return { content, encoding: finalEncoding }
	}

	// For larger files, use streaming approach
	// First, read enough bytes to detect encoding
	const { encoding: detectedEncoding, seemsBinary } = await readFileForEncodingDetection(filePath, {
		autoGuessEncoding: options.autoGuessEncoding,
	})

	// If file seems binary and we only accept text, throw error
	if (seemsBinary && options.acceptTextOnly) {
		throw new Error(`File seems to be binary but only text is accepted: ${filePath}`)
	}

	const finalEncoding = detectedEncoding || UTF8
	const decoder = new TextDecoder(finalEncoding)

	// Now read the entire file in streaming mode
	return new Promise((resolve, reject) => {
		let content = ""
		let isFirstChunk = true
		let fileStream: fs.ReadStream | null = null

		try {
			fileStream = fs.createReadStream(filePath, {
				highWaterMark: 64 * 1024, // 64KB chunks
			})

			fileStream.on("data", (chunk: Buffer | string) => {
				// Ensure chunk is a Buffer
				const bufferChunk = Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk)
				// Decode chunk and append to content
				content += decoder.decode(bufferChunk, isFirstChunk, false)
				isFirstChunk = false
			})

			fileStream.on("end", () => {
				// Decode any remaining bytes
				content += decoder.decode(Buffer.alloc(0), false, true)
				resolve({ content, encoding: finalEncoding })
			})

			fileStream.on("error", (err) => {
				reject(err)
			})
		} catch (err) {
			if (fileStream) {
				fileStream.destroy()
			}
			reject(err)
		}
	})
}
