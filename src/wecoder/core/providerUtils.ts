import { ClineProvider } from "../../core/webview/ClineProvider"

export async function updateWecoderDefaults(provider: ClineProvider) {
	await Promise.all([
		provider.updateGlobalState("wecoderModelConfig", "wecode-chat"),
		provider.updateGlobalState("apiProvider", "wecoder"),
	])
}
