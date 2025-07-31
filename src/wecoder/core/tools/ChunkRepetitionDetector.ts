export class ChunkRepetitionDetector {
	private readonly maxBufferSize: number
	private readonly enableTokenBasedDetection: boolean
	private readonly debugMode: boolean

	constructor(config?: { maxBufferSize?: number; enableTokenBasedDetection?: boolean; debugMode?: boolean }) {
		this.maxBufferSize = config?.maxBufferSize ?? 1000
		this.enableTokenBasedDetection = config?.enableTokenBasedDetection ?? true
		this.debugMode = config?.debugMode ?? false
	}

	public detectChunkRepetition(fullText: string): boolean {
		if (!fullText) return false

		// 确保文本不超过最大缓冲区大小
		const text = fullText.length > this.maxBufferSize ? fullText.slice(-this.maxBufferSize) : fullText

		// 标准检测逻辑 - 只检测连续重复的文本
		return (
			this.detectRepeatingPatternWithKMP(text) || this.detectRepeatedSubstringRaw(text, 10)
			// 移除了isHighlyRepeatedTokens检测，只保留连续重复文本检测
		)
	}

	/** 检测字符串是否完全由某个子串重复构成（KMP） */
	private detectRepeatingPatternWithKMP(text: string): boolean {
		if (!text || text.length < 20) return false

		const recentText = text.slice(-1000)
		const n = recentText.length
		const lps = new Array(n).fill(0)
		let j = 0

		for (let i = 1; i < n; i++) {
			while (j > 0 && recentText[i] !== recentText[j]) {
				j = lps[j - 1]
			}
			if (recentText[i] === recentText[j]) {
				j++
			}
			lps[i] = j
		}

		const repeatLen = n - lps[n - 1]
		if (lps[n - 1] > 0 && n % repeatLen === 0) {
			const pattern = recentText.substring(0, repeatLen)

			// 检查模式是否只包含空格，如果是则不判定为重复
			if (pattern.trim() === "") return false

			if (n / repeatLen >= 10) {
				this.debug(`[KMP] 重复子串 "${pattern}" × ${n / repeatLen}`)
				return true
			}
		}
		return false
	}

	/** 检测连续重复子串（无分词） */
	private detectRepeatedSubstringRaw(text: string, minRepeat = 5): boolean {
		const maxWindowSize = 30
		const minWindowSize = 5
		const checkRange = Math.min(1000, text.length)
		const segment = text.slice(-checkRange)

		for (let windowSize = minWindowSize; windowSize <= maxWindowSize; windowSize++) {
			if (segment.length < windowSize * minRepeat) continue

			const pattern = segment.slice(-windowSize)

			// 检查模式是否只包含空格，如果是则跳过
			if (pattern.trim() === "") continue

			let count = 0

			for (let i = segment.length - windowSize * 2; i >= 0; i -= windowSize) {
				const candidate = segment.slice(i, i + windowSize)
				if (candidate === pattern) {
					count++
				} else {
					break
				}
			}

			if (count + 1 >= minRepeat) {
				this.debug(`[RawRepeat] 连续重复子串 "${pattern}" × ${count + 1}`)
				return true
			}
		}
		return false
	}

	/** 高重复 token 检测 */
	private isHighlyRepeatedTokens(text: string, threshold = 0.8): boolean {
		const tokens = text.split(/[\s\-\.|,:<>\n]+/).filter((t) => t.length > 0)
		if (tokens.length < 5) return false

		const freq: Record<string, number> = {}
		for (const token of tokens) {
			freq[token] = (freq[token] ?? 0) + 1
		}

		const [most, count] = Object.entries(freq).reduce((a, b) => (a[1] > b[1] ? a : b), ["", 0])
		const ratio = count / tokens.length

		if (ratio >= threshold) {
			this.debug(`[Token重复] 高频 token "${most}" 占比 ${(ratio * 100).toFixed(1)}%`)
			return true
		}
		return false
	}

	private countOccurrences(text: string, pattern: string): number {
		let count = 0
		let position = 0

		while (true) {
			position = text.indexOf(pattern, position)
			if (position === -1) break
			count++
			position += 1 // 移动到下一个可能的位置
		}

		return count
	}

	private debug(message: string) {
		if (this.debugMode) {
			console.log(message)
		}
	}
}
