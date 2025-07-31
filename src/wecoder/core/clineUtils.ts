import Anthropic from "@anthropic-ai/sdk/index.mjs"
import { TextBlockParam } from "@anthropic-ai/sdk/resources/index.mjs"
import { findLastIndex } from "../../shared/array"

export function deduplicateConversationHistory(apiConversationHistory: (Anthropic.MessageParam & { ts?: number })[]) {
	const assistantHistory = apiConversationHistory.filter((value) => value.role === "assistant")
	if (assistantHistory.length > 2) {
		const content0 = assistantHistory[assistantHistory.length - 1].content
		const content1 = assistantHistory[assistantHistory.length - 2].content
		if (typeof content0 === "string" || typeof content1 === "string") {
			//ignored now
		} else {
			const text0 = content0
				.filter((value): value is TextBlockParam => value.type === "text")
				.map((v) => v.text)
				.join()
			const text1 = content1
				.filter((value): value is TextBlockParam => value.type === "text")
				.map((v) => v.text)
				.join()
			if (text0.length > 10 && text0 === text1) {
				apiConversationHistory
					.filter((v) => v.role === "user")
					.forEach((v) => {
						if (
							typeof v.content !== "string" &&
							v.content[0].type === "text" &&
							v.content[0].text.startsWith("[read_file for ")
						) {
							v.content.splice(1, 1)
						}
					})
			}
		}
	}

	for (let i = apiConversationHistory.length - 1; i >= 0; i--) {
		const conversation = apiConversationHistory[i]
		if (conversation.role === "user") {
			const contentItem = conversation.content[0]
			if (typeof contentItem !== "string" && "type" in contentItem && contentItem.type === "text") {
				if (!contentItem.text.startsWith("[read_file for ")) {
					continue
				}
				for (let j = i - 1; j >= 0; j--) {
					const prevConversation = apiConversationHistory[j]
					if (prevConversation.role === "assistant") continue
					const prevContentItem = prevConversation.content[0]
					if (
						typeof prevConversation.content !== "string" &&
						typeof prevContentItem !== "string" &&
						"type" in prevContentItem &&
						prevContentItem.type === "text"
					) {
						const prevContent = prevContentItem.text
						if (prevContent && prevContent === contentItem.text && prevConversation.content.length === 3) {
							prevConversation.content.splice(1, 1)
							break
						}
					}
				}
			}
		}
	}
}

export function handleWeCodeProxyError(error: any) {
	// 命中黑名单的仓库禁止使用外部大模型, 与 proxy 约定的状态码是406, 这里特殊判断一下
	// 400 错误码 可能是已输入的内容超出模型上下文
	// 413 错误码 输入内容过长
	if (error.status === 406 || error.status === 400 || error.status === 413) {
		if (error?.error?.content) {
			// eslint-disable-next-line no-unsafe-optional-chaining
			for (const block of error?.error?.content) {
				if (block?.type === "text") {
					throw new Error(block?.text)
				}
			}
		} else if (error?.error && typeof error.error === "string") {
			throw new Error(error?.error)
		} else if (error?.message) {
			throw new Error(error.message)
		}
	}

	// 兼容一下 gemini errorMessage 的格式
	handlerGeminiError(error)
}

export function handlerGeminiError(error: any) {
	// errorMessage = `got status: ${status} ${statusText}. ${JSON.stringify(errorBody)}`;

	// 处理 ClientError 类型的错误
	if (
		error &&
		typeof error === "object" &&
		"name" in error &&
		error.name === "ClientError" &&
		"message" in error &&
		typeof error.message === "string"
	) {
		const message = error.message
		const statusMatch = message.match(/^got status: (\d+) (.*?)\. (.*)$/)

		// 只处理 status === 400 的情况
		if (statusMatch && parseInt(statusMatch[1], 10) === 400) {
			const errorBodyString = statusMatch[3]
			let errorTextToThrow: string

			// 尝试解析错误体并提取有用信息
			try {
				const errorBody = JSON.parse(errorBodyString)
				// 优先使用结构化错误信息，如果不存在则回退到原始错误字符串
				errorTextToThrow = errorBody.candidates?.[0]?.content?.parts?.[0]?.text ?? errorBodyString
			} catch (parseError) {
				// 解析失败时使用原始错误消息
				errorTextToThrow = message
			}
			// 在 try...catch 块外部抛出错误，保持与原始逻辑一致
			throw new Error(errorTextToThrow)
		}
	}
}

export function removeLastInteractContent(
	apiConversationHistory: (Anthropic.Messages.MessageParam & { ts?: number })[],
) {
	let lastUserIndex = findLastIndex(apiConversationHistory, (m) => m.role === "user")
	let newHistory = apiConversationHistory
	if (lastUserIndex > 1) {
		newHistory = apiConversationHistory.slice(0, lastUserIndex - 1)
	}
	return newHistory
}
