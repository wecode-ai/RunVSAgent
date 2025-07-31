import { ModelInfo, ProviderSettings } from "@roo-code/types"
import { Dropdown, DropdownOption } from "vscrui"
import { Checkbox } from "vscrui"

interface WecoderApiOptionsProps {
	apiConfiguration?: ProviderSettings
	setApiConfigurationField: <K extends keyof ProviderSettings>(field: K, value: ProviderSettings[K]) => void
	handleInputChange: <K extends keyof ProviderSettings, E>(
		field: K,
		transform?: (event: E) => ProviderSettings[K],
	) => (event: E | Event) => void
}

const WECODER_MODELS = {
	INTERNAL: [
		{ label: "wecode-auto (自动选择)", value: "wecode-auto" },
		{ value: "wecode-chat", label: "wecode-qwen-chat" },
		{ value: "wecode-deepseek-v3", label: "sina-deepseek-v3" },
		{ value: "sina-kimi-k2", label: "sina-kimi-k2（测试版）" },
	],
	EXTERNAL: [
		{ value: "ali-kimi-k2", label: "ali-kimi-k2 (外部)" },
		{ value: "deepseek-chat", label: "huoshan-deepseek-v3 (外部)" },
		{ value: "deepseek-reasoner", label: "huoshan-deepseek-r1 (外部)" },
		{ value: "wecode-gpt-4.1", label: "wecode-gpt-4.1 (外部)" },
		{ value: "wecode-claude3.7", label: "wecode-claude3.7 (外部)" },
		{ value: "wecode-claude-sonnet-4", label: "wecode-claude-sonnet-4 (外部)" },
		{ value: "wecode-claude3.5", label: "wecode-claude3.5 (外部)" },
		{ value: "wecode-gemini-2.5", label: "wecode-gemini-2.5-pro (外部)" },
		{ value: "wecode-gpt-4.1-mini", label: "wecode-gpt-4.1-mini (外部)" },
		{ value: "wecode-gpt-4.1-nano", label: "wecode-gpt-4.1-nano (外部)" },
	],
}

const DEFAULT_INTERNAL_MODEL = WECODER_MODELS.INTERNAL[0].value
const DEFAULT_EXTERNAL_MODEL = WECODER_MODELS.EXTERNAL[0].value

const isInternalModel = (modelId: string) => {
	return WECODER_MODELS.INTERNAL.some((model) => model.value === modelId)
}

const isExternalModel = (modelId: string) => {
	return WECODER_MODELS.EXTERNAL.some((model) => model.value === modelId)
}

const getWecoderModelOptions = (useExternalModel: boolean) => {
	const options = [...WECODER_MODELS.INTERNAL]

	if (useExternalModel) {
		options.push(...WECODER_MODELS.EXTERNAL)
	}
	return options
}

const getCurrentModelValue = (modelId: string | undefined, useExternalModel: boolean) => {
	if (!modelId) {
		return useExternalModel ? DEFAULT_EXTERNAL_MODEL : DEFAULT_INTERNAL_MODEL
	}

	if (!useExternalModel && isExternalModel(modelId)) {
		return DEFAULT_INTERNAL_MODEL
	}
	return modelId
}

export const WecoderApiOptions = ({ apiConfiguration, handleInputChange }: WecoderApiOptionsProps) => {
	return (
		<div>
			<div style={{ marginTop: 10, marginBottom: 30, position: "relative", zIndex: 1000 }}>
				<label style={{ fontWeight: "500", display: "block", marginBottom: 5 }}>Model</label>
				<Dropdown
					value={getCurrentModelValue(
						apiConfiguration?.wecoderModelConfig,
						apiConfiguration?.useExternalModel || false,
					)}
					onChange={(value: unknown) => {
						if (!value || !(value as DropdownOption).value) {
							console.warn("Invalid model value received:", value)
							return
						}
						const modelId = (value as DropdownOption).value
						handleInputChange("wecoderModelConfig")({
							target: {
								value: modelId,
							},
						})
					}}
					style={{ width: "100%" }}
					options={getWecoderModelOptions(apiConfiguration?.useExternalModel || false)}
				/>
			</div>
			<div style={{ marginTop: 10 }}>
				<Checkbox
					style={{ marginBottom: 10 }}
					checked={apiConfiguration?.useExternalModel || false}
					onChange={(checked: boolean) => {
						const currentModelId = apiConfiguration?.wecoderModelConfig || DEFAULT_INTERNAL_MODEL
						let newModelId = currentModelId

						if (checked) {
							if (isInternalModel(currentModelId)) {
								newModelId = DEFAULT_EXTERNAL_MODEL
							}
						} else {
							if (isExternalModel(currentModelId)) {
								newModelId = DEFAULT_INTERNAL_MODEL
							}
						}

						const updates = [
							{ key: "useExternalModel", value: checked },
							{ key: "wecoderModelConfig", value: newModelId },
						]

						if (!checked) {
							updates.push({
								key: "autoRetryWithExternalModel",
								value: false,
							})
						}

						updates.forEach(({ key, value }) => {
							handleInputChange(key as keyof ProviderSettings)({
								target: { value },
							})
						})
					}}>
					允许使用外部测试模型（请注意代码泄漏风险）
				</Checkbox>
			</div>
		</div>
	)
}

export const wecoderModels = {
	"wecode-auto": {
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: false,
	},
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
		inputPrice: 3.0,
		outputPrice: 15.0,
		cacheWritesPrice: 3.75,
		cacheReadsPrice: 0.3,
	},
	"wecode-claude3.7": {
		maxTokens: 64_000,
		contextWindow: 200_000,
		supportsImages: true,
		supportsComputerUse: true,
		supportsPromptCache: true,
		inputPrice: 3.0, // $3 per million input tokens
		outputPrice: 15.0, // $15 per million output tokens
		cacheWritesPrice: 3.75, // $3.75 per million tokens
		cacheReadsPrice: 0.3, // $0.30 per million tokens
	},
	"wecode-claude-sonnet-4": {
		maxTokens: 39_000,
		contextWindow: 200_000,
		supportsImages: true,
		supportsComputerUse: true,
		supportsPromptCache: true,
		inputPrice: 3.0, // $3 per million input tokens
		outputPrice: 15.0, // $15 per million output tokens
		cacheWritesPrice: 3.75, // $3.75 per million tokens
		cacheReadsPrice: 0.3, // $0.30 per million tokens
	},
	// 外部 deepseek-r1
	"deepseek-reasoner": {
		maxTokens: 8192,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
		inputPrice: 0.55,
		outputPrice: 2.19,
	},
	// 外部 deepseek-v3
	"deepseek-chat": {
		maxTokens: 8192,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
		inputPrice: 0.014,
		outputPrice: 0.28,
	},
	"wecode-deepseek-r1": {
		maxTokens: 8192,
		contextWindow: 64_000,
		supportsImages: false,
		supportsPromptCache: true,
	},
	"wecode-deepseek-v3": {
		maxTokens: 8192,
		contextWindow: 64_000,
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
		inputPrice: 2,
		outputPrice: 8,
		cacheReadsPrice: 0.5,
	},
	"wecode-gpt-4.1-mini": {
		maxTokens: 32_768,
		contextWindow: 1_047_576,
		supportsImages: true,
		supportsPromptCache: true,
		inputPrice: 0.4,
		outputPrice: 1.6,
		cacheReadsPrice: 0.1,
	},
	"wecode-gpt-4.1-nano": {
		maxTokens: 32_768,
		contextWindow: 1_047_576,
		supportsImages: true,
		supportsPromptCache: true,
		inputPrice: 0.1,
		outputPrice: 0.4,
		cacheReadsPrice: 0.025,
	},
} as const satisfies Record<string, ModelInfo>

export function normalizeWecoderApiConfiguration(apiConfiguration?: ProviderSettings) {
	return {
		selectedProvider: "wecoder",
		selectedModelId: apiConfiguration?.wecoderModelConfig || "wecode-chat",
		selectedModelInfo:
			wecoderModels[
				apiConfiguration?.wecoderModelConfig === "wecode-auto"
					? "wecode-chat"
					: apiConfiguration?.wecoderModelConfig || "wecode-chat"
			],
	}
}
