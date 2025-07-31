import { ClineProvider } from "../../../core/webview/ClineProvider"
import { record, recordParam } from "../../utils/telemetry"
import { autoConfig, MODEL_NAME_ID_MAP } from "./config"
import { WebviewMessage } from "../../../shared/WebviewMessage"

export async function loadWeCodeModelConfig(message: WebviewMessage, provider: ClineProvider) {
	let currentAiModelSoure = (await provider.getGlobalState("currentAiModelSoure")) as string
	if (message.aiModelSource) {
		const m = message.mode as string
		currentAiModelSoure = message.aiModelSource
		await switchAiModeByAiSource(currentAiModelSoure, m, provider)
		const profileName = MODEL_NAME_ID_MAP[currentAiModelSoure]

		if (message?.mode && profileName) {
			message.text = profileName
		}

		await Promise.all([provider.updateGlobalState("currentAiModelSoure", currentAiModelSoure)])

		// 上报用户的一键切换行为
		const id = crypto.randomUUID()
		const params: recordParam = {
			id: id,
			taskId: provider.getCurrentCline()?.taskId || id,
			codeAddContents: [message.aiModelSource],
			action: "wecoder_switch_model",
			lineAddCount: 0,
		}
		record(params)
	}

	// 如果是外部模型需要进行弹窗确认
	if (message?.text) {
		return handleExternalModelRiskNotice(provider, { text: message.text })
	}
	return false
}

async function handleExternalModelRiskNotice(provider: ClineProvider, message: { text: string }): Promise<boolean> {
	const externalRiskNoticeConfirmed = provider.getGlobalState("externalRiskNoticeConfirmed")
	const configs = await provider.providerSettingsManager.listConfig()
	const apiConfig = configs.find((config) => config.name === message.text)

	if (!externalRiskNoticeConfirmed && apiConfig && apiConfig.isExternalModel) {
		await Promise.all([
			provider.postMessageToWebview({
				type: "externalRiskNotice",
				values: {
					gptWarning: true,
					pendingApiConfigId: apiConfig.id,
					loadApiConfiguration: true,
				},
			}),
		])
		return true
	}
	return false
}

export async function updateWxternalRiskNoticeConfirmed(message: WebviewMessage, provider: ClineProvider) {
	let externalRiskNoticeConfirmed = provider.getGlobalState("externalRiskNoticeConfirmed")
	if (!externalRiskNoticeConfirmed && message?.userExternalRiskNoticeConfirmed) {
		externalRiskNoticeConfirmed = message.userExternalRiskNoticeConfirmed
		await provider.updateGlobalState("externalRiskNoticeConfirmed", externalRiskNoticeConfirmed)
	}
}

export async function switchAiModeByAiSource(
	aiModelSource: string,
	currentMode: string,
	provider: ClineProvider,
): Promise<void> {
	try {
		return await provider.providerSettingsManager.lock(async () => {
			const currentConfigs = await provider.providerSettingsManager.load()

			let targetConfigs = autoConfig
			const profileName = MODEL_NAME_ID_MAP[aiModelSource]
			if (profileName) {
				currentConfigs.apiConfigs = currentConfigs.apiConfigs || targetConfigs.apiConfigs
				currentConfigs.apiConfigs[profileName] = targetConfigs.apiConfigs[profileName]

				currentConfigs.modeApiConfigs = currentConfigs.modeApiConfigs || {}
				currentConfigs.modeApiConfigs[currentMode] = targetConfigs?.apiConfigs[profileName]?.id || ""
				currentConfigs.currentApiConfigName = profileName
				await provider.providerSettingsManager.store(currentConfigs)
			}
		})
	} catch (error) {
		throw new Error(`Failed to switchAiModeByAiSource config: ${error}`)
	}
}

export async function debugMode(provider: ClineProvider) {
	const state = await provider.getStateToPostToWebview()
	provider.postMessageToWebview({ type: "state", state: { ...state, debugMode: true } })
	return
}
