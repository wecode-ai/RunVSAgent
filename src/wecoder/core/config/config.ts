import { ProviderProfiles } from "../../../core/config/ProviderSettingsManager"
import { generateId } from "../../utils/wecode-util"

export const WECODE_CHINA_AUTO_DISPLAYNAME = "公网国内模型(⚠️)"
export const WECODE_AUTO_DISPLAYNAME = "内网模型"
export const WECODE_GLOBAL_AUTO_DISPLAYNAME = "公网海外模型(⚠️)"

export const MODEL_NAME_ID_MAP: Record<string, string> = {
	[WECODE_CHINA_AUTO_DISPLAYNAME]: "wecode-china-auto",
	[WECODE_GLOBAL_AUTO_DISPLAYNAME]: "wecode-global-auto",
	[WECODE_AUTO_DISPLAYNAME]: "wecode-auto",
}

export const PINNED_API_NAME = ["wecode-auto", "wecode-china-auto", "wecode-global-auto"]

// 新增外部模型时, 需要在此处添加; 任务聊天页面选择模型时, 添加警告图标展示
export const EXTERNAL_MODEL_SET = new Set([
	"wecode-claude3.5",
	"wecode-claude3.7",
	"wecode-claude-sonnet-4",
	"deepseek-reasoner",
	"deepseek-chat",
	"wecode-gemini-2.5",
	"wecode-gpt-4.1",
	"wecode-gpt-4.1-mini",
	"wecode-gpt-4.1-nano",
])

export function isExternalModel(key: any): boolean {
	return EXTERNAL_MODEL_SET.has(key)
}

export const MODEL_SOURCE_MAP: Record<string, string> = {
	"deepseek-chat": WECODE_CHINA_AUTO_DISPLAYNAME,
	"wecode-auto": WECODE_AUTO_DISPLAYNAME,
	"wecode-gpt-4.1": WECODE_GLOBAL_AUTO_DISPLAYNAME,
}

const weCodeReasoningConfigId = generateId()
const weCodeAutoConfigId = generateId()

export const autoConfig: ProviderProfiles = {
	currentApiConfigName: "wecode-auto",
	apiConfigs: {
		"wecode-auto": {
			id: weCodeAutoConfigId,
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-auto",
			apiModelId: "wecode-auto",
		},
		"wecode-china-auto": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "deepseek-chat",
			apiModelId: "deepseek-chat",
			useExternalModel: true,
		},
		"wecode-global-auto": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-gpt-4.1",
			apiModelId: "wecode-gpt-4.1",
			useExternalModel: true,
		},
		"sina-deepseek-v3": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-deepseek-v3",
			apiModelId: "wecode-deepseek-v3",
		},
		"sina-kimi-k2（测试版）": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "sina-kimi-k2",
			apiModelId: "sina-kimi-k2",
		},
		"ali-kimi-k2": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "ali-kimi-k2",
			apiModelId: "ali-kimi-k2",
			useExternalModel: true,
		},
		"wecode-claude3.7": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-claude3.7",
			apiModelId: "wecode-claude3.7",
			useExternalModel: true,
		},
		"wecode-claude-sonnet-4": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-claude-sonnet-4",
			apiModelId: "wecode-claude-sonnet-4",
			useExternalModel: true,
		},
		// deepseek-chat: deepseek-v3 外部模型
		"huoshan-deepseek-v3": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "deepseek-chat",
			apiModelId: "deepseek-chat",
			useExternalModel: true,
		},
		"wecode-gemini-2.5": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-gemini-2.5",
			apiModelId: "wecode-gemini-2.5",
			useExternalModel: true,
		},
		"wecode-gpt-4.1": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-gpt-4.1",
			apiModelId: "wecode-gpt-4.1",
			useExternalModel: true,
		},
		"wecode-gpt-4.1-mini": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-gpt-4.1-mini",
			apiModelId: "wecode-gpt-4.1-mini",
			useExternalModel: true,
		},
		"wecode-gpt-4.1-nano": {
			id: generateId(),
			apiProvider: "wecoder",
			wecoderModelConfig: "wecode-gpt-4.1-nano",
			apiModelId: "wecode-gpt-4.1-nano",
			useExternalModel: true,
		},
	},
	modeApiConfigs: {
		code: weCodeAutoConfigId,
		ask: weCodeReasoningConfigId,
		architect: weCodeAutoConfigId,
		debug: weCodeAutoConfigId,
		orchestrator: weCodeAutoConfigId,
	},
}

// wecoder
export function merge_config(providerProfiles: ProviderProfiles) {
	// 支持多profile, 合并开源项目时请注意保留.
	let needsUpdate = false

	providerProfiles.apiConfigs = providerProfiles.apiConfigs || autoConfig.apiConfigs
	providerProfiles.modeApiConfigs = providerProfiles.modeApiConfigs || autoConfig.modeApiConfigs

	for (const profileName in providerProfiles.apiConfigs) {
		// 如果 profileName 是 wecode-deepseek-v3, 名字改为 sina-deepseek-v3 或者 deepseek-chat 改为 huoshan-deepseek-v3
		if (profileName === "wecode-deepseek-v3") {
			providerProfiles.apiConfigs["sina-deepseek-v3"] = providerProfiles.apiConfigs[profileName]
			delete providerProfiles.apiConfigs[profileName]
			needsUpdate = true
		} else if (profileName === "deepseek-chat") {
			providerProfiles.apiConfigs["huoshan-deepseek-v3"] = providerProfiles.apiConfigs[profileName]
			delete providerProfiles.apiConfigs[profileName]
			needsUpdate = true
		}
	}
	for (const profileName in autoConfig.apiConfigs) {
		if (!providerProfiles.apiConfigs[profileName]) {
			providerProfiles.apiConfigs[profileName] = autoConfig.apiConfigs[profileName]
			needsUpdate = true
		}
	}

	//  下个版本删掉
	for (const key in providerProfiles.modeApiConfigs) {
		const configId = providerProfiles.modeApiConfigs[key]

		for (const profileName in providerProfiles.apiConfigs) {
			const apiConfig = providerProfiles.apiConfigs[profileName]
			if (apiConfig.id === configId) {
				const apiModelId =
					apiConfig.apiProvider === "wecoder" ? apiConfig.wecoderModelConfig : apiConfig.apiModelId
				if (apiModelId === profileName) {
					continue
				}
				if (
					profileName === "default" ||
					profileName === "ask" ||
					profileName === "architect" ||
					profileName === "debug" ||
					profileName === "orchestrator"
				) {
					if (apiModelId) {
						if (providerProfiles.apiConfigs[apiModelId]) {
							if (providerProfiles.apiConfigs[apiModelId].id) {
								providerProfiles.modeApiConfigs[key] = providerProfiles.apiConfigs[apiModelId].id
							}
						} else {
							providerProfiles.apiConfigs[apiModelId] = apiConfig
						}
					}
					delete providerProfiles.apiConfigs[profileName]
					needsUpdate = true
				}
			}
		}
	}

	for (const profileName in providerProfiles.apiConfigs) {
		if (
			profileName === "default" ||
			profileName === "ask" ||
			profileName === "architect" ||
			profileName === "debug" ||
			profileName === "orchestrator"
		) {
			delete providerProfiles.apiConfigs[profileName]
		}
	}

	// 下个版本删掉
	return needsUpdate
}

export function convertAiModelSoure(apiModelId: any) {
	if (apiModelId) {
		return MODEL_SOURCE_MAP[apiModelId] || ""
	}
	return ""
}
