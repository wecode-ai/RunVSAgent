import axios from "axios"
import type { ExtensionMessage } from "../../shared/ExtensionMessage"

/**
 * 检查 copilot.weibo.com 连接状态
 * @returns Promise<"ok" | "error">
 */
async function checkCopilotConnection(): Promise<"ok" | "error"> {
	try {
		await axios.get("https://copilot.weibo.com", { timeout: 3000 })
		return "ok"
	} catch {
		return "error"
	}
}

/**
 * 检查 copilot.weibo.com 连接并自动 post 到 webview
 * @param post postMessageToWebview 函数
 */
export async function checkAndPostCopilotConnectionStatus(
	post: (msg: ExtensionMessage) => Promise<void>,
): Promise<void> {
	const networkStatus = await checkCopilotConnection()
	await post({ type: "wecodeConnectionStatus", payload: { networkStatus } })
}
