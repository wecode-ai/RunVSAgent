import Anthropic from "@anthropic-ai/sdk/index.mjs"
import { ApiHandler, SingleCompletionHandler } from "../../../api"
import { AnthropicHandler } from "../../../api/providers/anthropic"
import { BaseProvider } from "../../../api/providers/base-provider"
import { OpenAiHandler } from "../../../api/providers/openai"
import { ApiHandlerOptions } from "../../../shared/api"
import { wecoderDefaultModelId, WeCoderHandlerType, wecoderModels } from "../../shared/wecoder"
import { GeminiHandler } from "../../../api/providers/gemini"
import { WECODE_PROXY_URL, WECODE_OPENAI_URL } from "../../../api/providers/wecoder_constants"
import { ModelInfo } from "@roo-code/types"

type WeCoderHandlerDifination = {
	type: "inner" | "external"
	handler: ApiHandler & SingleCompletionHandler
}

export class WecoderHandler extends BaseProvider implements ApiHandler, SingleCompletionHandler {
	private handlers: { [key in WeCoderHandlerType]: WeCoderHandlerDifination }
	private options: ApiHandlerOptions

	constructor(options: ApiHandlerOptions) {
		super()
		this.options = options

		// 初始化所有可能的 handlers
		this.handlers = {
			// 内部模型
			"wecode-chat": {
				type: "inner",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "wecode-agent-max",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"wecode-reasoning": {
				type: "inner",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "wecode-agent-reasoning",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"wecode-deepseek-r1": {
				type: "inner",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "wecode-deepseek-r1",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"wecode-deepseek-v3": {
				type: "inner",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "wecode-deepseek-v3",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"sina-kimi-k2": {
				type: "inner",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "sina-kimi-k2",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},

			// 外部模型
			"ali-kimi-k2": {
				type: "external",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "ali-kimi-k2",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"wecode-claude3.5": {
				type: "external",
				handler: new AnthropicHandler({
					...options,
					apiKey: "weibo-api-key",
					apiModelId: "claude-3-5-sonnet-20241022",
					anthropicBaseUrl: WECODE_PROXY_URL,
				}),
			},
			"wecode-claude3.7": {
				type: "external",
				handler: new AnthropicHandler({
					...options,
					apiKey: "weibo-api-key",
					apiModelId: "claude-3-7-sonnet-20250219",
					anthropicBaseUrl: WECODE_PROXY_URL,
				}),
			},
			"wecode-claude-sonnet-4": {
				type: "external",
				handler: new AnthropicHandler({
					...options,
					apiKey: "weibo-api-key",
					apiModelId: "claude-sonnet-4-20250514",
					anthropicBaseUrl: WECODE_PROXY_URL,
				}),
			},
			// deepseek-reasoner: deepseek-r1 外部模型
			"deepseek-reasoner": {
				type: "external",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "deepseek-reasoner",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			// deepseek-chat: deepseek-v3 外部模型
			"deepseek-chat": {
				type: "external",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "deepseek-chat",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: options.openAiStreamingEnabled,
				}),
			},
			"wecode-gemini-2.5": {
				type: "external",
				handler: new GeminiHandler({
					...options,
					geminiApiKey: "weibo-api-key",
					apiModelId: "gemini-2.5-pro-exp-03-25",
					googleGeminiBaseUrl: WECODE_PROXY_URL,
				}),
			},
			// wecode-gpt-4.1:wecode-gpt-4.1 外部模型
			"wecode-gpt-4.1": {
				type: "external",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "gpt-4.1",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"wecode-gpt-4.1-mini": {
				type: "external",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "gpt-4.1-mini",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"wecode-gpt-4.1-nano": {
				type: "external",
				handler: new OpenAiHandler({
					...options,
					openAiApiKey: "weibo-api-key",
					openAiModelId: "gpt-4.1-nano",
					openAiBaseUrl: WECODE_OPENAI_URL,
					openAiStreamingEnabled: true,
				}),
			},
			"wecode-auto": {
				type: "inner",
				handler: undefined as any, // 占位，永远不会被直接调用
			},
		}
	}

	// 获取当前应该使用的 handler
	private getCurrentHandler(): ApiHandler & SingleCompletionHandler {
		return this.handlers[this.options.wecoderModelConfig || "wecode-deepseek-v3"].handler
	}

	async *createMessage(
		systemPrompt: string,
		messages: Anthropic.Messages.MessageParam[],
		options?: any,
	): AsyncGenerator<any, any, any> {
		const handler = await this.getHandler(options, messages)
		const stream = await handler.createMessage(systemPrompt, messages, options)
		for await (const chunk of stream) {
			if (chunk.type == "usage") {
				chunk.totalCost = undefined
			}
			yield chunk
		}
	}
	private async getHandler(
		options?: any,
		messages?: Anthropic.Messages.MessageParam[],
		prompt?: string,
	): Promise<ApiHandler & SingleCompletionHandler> {
		let modelId = this.options.wecoderModelConfig || "wecode-deepseek-v3"

		if (modelId === "wecode-auto") {
			modelId = await this.selectModelByStrategy(messages, prompt)
			console.log("auto select modelId: ", modelId)
		}

		const handler = this.handlers[modelId]?.handler ?? this.getCurrentHandler()

		if (!options?.retry) {
			return handler
		}

		const handlersToConsider = options?.useExternalModel
			? Object.values(this.handlers).filter((h) => h.handler !== handler)
			: Object.values(this.handlers).filter(
					(h) =>
						h.type === "inner" &&
						h.handler !== handler &&
						h?.handler &&
						!["wecode-deepseek-r1", "wecode-reasoning"].includes(h.handler.getModel().id),
				)

		if (handlersToConsider.length === 0) {
			return handler // 如果没有找到合适的 handler，返回当前 handler
		}

		const randomIndex = Math.floor(Math.random() * handlersToConsider.length)
		return handlersToConsider[randomIndex].handler
	}

	private extractTextLength(content: string | Array<any>): number {
		if (typeof content === "string") {
			return content.length
		}
		if (Array.isArray(content)) {
			let total = 0
			for (const block of content) {
				if (block && typeof block === "object" && block.type === "text" && typeof block.text === "string") {
					total += block.text.length
				}
			}
			return total
		}
		return 0
	}

	private async selectModelByStrategy(
		messages?: Anthropic.Messages.MessageParam[],
		prompt?: string,
	): Promise<Exclude<WeCoderHandlerType, "wecode-auto">> {
		let contentLength = 0
		let combinedText = ""

		if (messages && messages.length > 0) {
			const userMessageCount = messages.filter((m) => m.role === "user").length
			if (userMessageCount >= 3) {
				return "wecode-deepseek-v3"
			}
			for (const m of messages) {
				contentLength += this.extractTextLength(m.content as any)
				if (typeof m.content === "string") {
					combinedText += m.content + " "
				} else if (Array.isArray(m.content)) {
					for (const block of m.content) {
						if (
							block &&
							typeof block === "object" &&
							block.type === "text" &&
							typeof block.text === "string"
						) {
							combinedText += block.text + " "
						}
					}
				}
			}
		} else if (prompt) {
			contentLength = prompt.length
			combinedText = prompt
		}

		// 默认策略
		let defaultModel: Exclude<WeCoderHandlerType, "wecode-auto"> =
			contentLength < 500 ? "wecode-chat" : "wecode-deepseek-v3"

		// 大模型判断复杂度
		if (combinedText.trim().length > 0) {
			try {
				const isSimple = await this.isSimpleTaskByLLM(combinedText)
				if (isSimple) {
					return "wecode-chat"
				} else {
					return "wecode-deepseek-v3"
				}
			} catch (e) {
				// 出错时回退默认策略
				return defaultModel
			}
		} else {
			return defaultModel
		}
	}

	// 使用轻量大模型判断任务是否简单
	private async isSimpleTaskByLLM(prompt: string): Promise<boolean> {
		const judgePrompt = `请判断以下任务是简单还是复杂，只回答“简单”或“复杂”：\n${prompt}`
		try {
			const handler = this.handlers["wecode-chat"].handler
			const reply = await handler.completePrompt(judgePrompt)
			if (reply.includes("简单")) {
				return true
			} else {
				return false
			}
		} catch (e) {
			// 出错时默认认为复杂
			return false
		}
	}

	async completePrompt(prompt: string): Promise<string> {
		const handler = await this.getHandler(undefined, undefined, prompt)
		return handler.completePrompt(prompt)
	}

	getModel(): { id: string; info: ModelInfo } {
		const modelId = this.options.wecoderModelConfig || wecoderDefaultModelId
		return {
			id: modelId,
			info: wecoderModels[modelId as keyof typeof wecoderModels],
		}
	}
}
