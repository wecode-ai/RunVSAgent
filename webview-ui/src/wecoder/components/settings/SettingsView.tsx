import { vscode } from "@/utils/vscode"
import { MODEL_SOURCE_MAP } from "@/wecoder/config/config"
import { ProviderSettings } from "@roo-code/types"

export function wecoderHandleSubmit(apiConfiguration: ProviderSettings) {
	if (apiConfiguration) {
		const modelId = apiConfiguration.apiModelId
		const aiModelSource = modelId ? MODEL_SOURCE_MAP[modelId] || "" : ""
		vscode.postMessage({ type: "currentAiModelSoure", text: aiModelSource })
	}
}
