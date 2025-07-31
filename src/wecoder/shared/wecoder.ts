import { ModelInfo } from "@roo-code/types"

export interface WeCoderOptions {
	retry?: boolean
	retryWithExternalModel?: boolean
}

export type WeCoderWebViewMessageType =
	| "modelConfig"
	| "useExternalModel"
	| "autoRetryWithExternalModel"
	| "wecoderModelConfig"
	| "debug"
	| "openWecodeChat"
	| "getModelQuota"
	| "currentAiModelSoure"
	| "externalRiskNoticeConfirmed"
	| "getQrcodeInfo"
	| "getQrcodeStatus"
	| "qrcodeInfo"
	| "getUserInfo"

export type WeCoderMessageType =
	| "modelConfig"
	| "wecoderModelConfig"
	| "debug"
	| "modelQuotaResponse"
	| "userInfo"
	| "qrcodeInfo"
	| "qrcodeStatus"
	| "externalRiskNotice"

export type WeCoderHandlerType =
	| "wecode-auto"
	| "wecode-chat"
	| "wecode-reasoning"
	| "wecode-claude3.5"
	| "wecode-claude3.7"
	| "wecode-claude-sonnet-4"
	| "deepseek-reasoner"
	| "deepseek-chat"
	| "wecode-deepseek-r1"
	| "wecode-deepseek-v3"
	| "wecode-gemini-2.5"
	| "wecode-gpt-4.1"
	| "wecode-gpt-4.1-mini"
	| "wecode-gpt-4.1-nano"
	| "sina-kimi-k2"
	| "ali-kimi-k2"

export interface ModelQuotaData {
	quota: number
	remaining: number
	usage: number
	user: string
	user_quota_detail?: {
		monthly_quota: number
		monthly_usage: number
		permanent_quota: number
		permanent_usage: number
	}
}

export type WecoderModelId = keyof typeof wecoderModels
export const wecoderDefaultModelId: WecoderModelId = "wecode-deepseek-v3"
export const wecoderModels = {
	"wecode-chat": {
		maxTokens: -1,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
	},
	"wecode-reasoning": {
		maxTokens: -1,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
	},
	"wecode-claude3.5": {
		maxTokens: 8192,
		contextWindow: 200_000,
		supportsImages: true,
		supportsComputerUse: true,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		cacheWritesPrice: 0,
		cacheReadsPrice: 0,
		// inputPrice: 3.0,
		// outputPrice: 15.0,
		// cacheWritesPrice: 3.75,
		// cacheReadsPrice: 0.3,
	},
	"wecode-claude3.7": {
		maxTokens: 64_000,
		contextWindow: 200_000,
		supportsImages: true,
		supportsComputerUse: true,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		cacheWritesPrice: 0,
		cacheReadsPrice: 0,
		// inputPrice: 3.0, // $3 per million input tokens
		// outputPrice: 15.0, // $15 per million output tokens
		// cacheWritesPrice: 3.75, // $3.75 per million tokens
		// cacheReadsPrice: 0.3, // $0.30 per million tokens
	},
	"wecode-claude-sonnet-4": {
		maxTokens: 39_000,
		contextWindow: 200_000,
		supportsImages: true,
		supportsComputerUse: true,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		cacheWritesPrice: 0,
		cacheReadsPrice: 0,
		// inputPrice: 3.0, // $3 per million input tokens
		// outputPrice: 15.0, // $15 per million output tokens
		// cacheWritesPrice: 3.75, // $3.75 per million tokens
		// cacheReadsPrice: 0.3, // $0.30 per million tokens
	},
	// 外部 deepseek-r1
	"deepseek-reasoner": {
		maxTokens: 8192,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		// inputPrice: 0.55,
		// outputPrice: 2.19,
	},
	// 外部 deepseek-v3
	"deepseek-chat": {
		maxTokens: 8192,
		contextWindow: 128_000,
		supportsImages: false,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		// inputPrice: 0.014,
		// outputPrice: 0.28,
	},
	"wecode-deepseek-r1": {
		maxTokens: 8192,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
	},
	"wecode-deepseek-v3": {
		maxTokens: 8192,
		contextWindow: 128_000,
		supportsImages: false,
		supportsPromptCache: true,
	},
	"sina-kimi-k2": {
		maxTokens: 8192,
		contextWindow: 128_000,
		supportsImages: false,
		supportsPromptCache: true,
	},
	"ali-kimi-k2": {
		maxTokens: 8192,
		contextWindow: 128_000,
		supportsImages: false,
		supportsPromptCache: true,
	},
	"wecode-gemini-2.5": {
		maxTokens: 65_535,
		contextWindow: 1_048_576,
		supportsImages: true,
		supportsPromptCache: false,
		inputPrice: 0,
		outputPrice: 0,
	},
	"wecode-gpt-4.1": {
		maxTokens: 32_768,
		contextWindow: 1_047_576,
		supportsImages: true,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		cacheReadsPrice: 0,
	},
	"wecode-gpt-4.1-mini": {
		maxTokens: 32_768,
		contextWindow: 1_047_576,
		supportsImages: true,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		cacheReadsPrice: 0,
	},
	"wecode-gpt-4.1-nano": {
		maxTokens: 32_768,
		contextWindow: 1_047_576,
		supportsImages: true,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
		cacheReadsPrice: 0,
	},
	// 伪模型 auto，用于动态分发
	"wecode-auto": {
		maxTokens: -1,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
		inputPrice: 0,
		outputPrice: 0,
	},
} as const satisfies Record<WeCoderHandlerType, ModelInfo>

export const wecoderCreditSpeedMap: Record<string, string> = {
	"wecode-reasoning": "-0.5x",
	"sina-deepseek-v3": "-0.5x",
	"sina-kimi-k2（测试版）": "-3x",
	"ali-kimi-k2": "-3x",
	"wecode-chat": "-0.5x",
	"huoshan-deepseek-v3": "-0.5x",
	"wecode-gpt-4.1": "1x",
	"wecode-claude3.7": "4x",
	"wecode-claude-sonnet-4": "4x",
	"wecode-gemini-2.5": "3x",
	"wecode-auto": "-0.5x",
	"wecode-china-auto": "-0.5x",
	"wecode-global-auto": "1x",
	"wecode-gpt-4.1-mini": "0.2x",
	"wecode-gpt-4.1-nano": "0.1x",
	// 如有更多模型请补充
}
