import { ClineProvider } from "../../../core/webview/ClineProvider"
import { WebviewMessage } from "../../../shared/WebviewMessage"
import { WeCoderHandlerType } from "../../shared/wecoder"
import { getLoginWecodeUser, getModelQuota } from "../../utils/wecode-util"
import { debugMode } from "../config/providerManager"
import * as vscode from "vscode"
import { getQrcodeStatus, getQrcodeUrl } from "../../utils/qrcode"

export async function handleWeCoderMessages(provider: ClineProvider, message: WebviewMessage) {
	switch (message.type) {
		case "wecodeRetryConnection":
			try {
				const axios = require("axios")
				Promise.resolve(axios.get("https://copilot.weibo.com", { timeout: 3000 }))
					.then(() => {
						provider.postMessageToWebview({
							type: "wecodeConnectionStatus",
							payload: { networkStatus: "ok" },
						})
					})
					.catch(() => {
						provider.postMessageToWebview({
							type: "wecodeConnectionStatus",
							payload: { networkStatus: "error" },
						})
					})
			} catch (e) {
				provider.postMessageToWebview({ type: "wecodeConnectionStatus", payload: { networkStatus: "error" } })
			}
			break
		case "debug":
			debugMode(provider)
			break
		case "externalRiskNoticeConfirmed":
			await provider.updateGlobalState("externalRiskNoticeConfirmed", message.bool ?? false)
			await provider.postStateToWebview()
			break
		case "currentAiModelSoure":
			await provider.updateGlobalState("currentAiModelSoure", message.text)
			await provider.postStateToWebview()
			break
		case "useExternalModel":
			await provider.updateGlobalState("useExternalModel", message.bool ?? false)
			await provider.postStateToWebview()
			break
		case "wecoderModelConfig":
			await provider.updateGlobalState("wecoderModelConfig", message.text as WeCoderHandlerType)
			await provider.postStateToWebview()
			break
		case "openWecodeChat":
			try {
				await vscode.commands.executeCommand("wecode.focusContinueInput")
			} catch (error) {
				console.warn("open wecode chat:", error)
			}
			break
		case "getModelQuota":
			// Import dynamically to avoid webview build issues
			try {
				const providerName = message.text
				console.log(`Extension: Received ${providerName} request`)
				if (!providerName) {
					console.error(`Extension: Unknown provider for message: ${providerName}`)
					break
				}

				const username = getLoginWecodeUser()
				console.log("Extension: Wecode user:", username)

				if (username) {
					const quotaData = await getModelQuota(username, providerName)
					console.log(`Extension: ${providerName} quota data:`, quotaData)

					if (quotaData) {
						provider.postMessageToWebview({
							type: "modelQuotaResponse",
							modelQuotaData: quotaData.data,
						})
						console.log("Extension: Sent modelQuotaResponse to webview")
					} else {
						console.error(`Extension: Failed to get ${providerName} quota data`)
					}
				} else {
					console.warn("Extension: No Wecode user found")
				}
			} catch (error) {
				console.error(`Extension: Error in ${message.text} handler:`, error)
			}
			break
		case "getQrcodeInfo":
			try {
				console.log("Extension: Received getQrcodeInfo request")
				const qrcodeInfo = await getQrcodeUrl()

				if (qrcodeInfo && qrcodeInfo.loginUrl && qrcodeInfo.qrcodeSid) {
					await provider.postMessageToWebview({
						type: "qrcodeInfo",
						text: JSON.stringify({
							loginUrl: qrcodeInfo.loginUrl,
							qrcodeSid: qrcodeInfo.qrcodeSid,
						}),
					})
					console.log("Extension: Sent qrcodeInfo to webview")
				} else {
					console.error("Extension: Failed to get QR code info")
				}
			} catch (error) {
				console.error("Extension: Error in getQrcodeInfo handler:", error)
			}
			break
		case "getQrcodeStatus":
			try {
				console.log("Extension: Received getQrcodeStatus request")
				if (message.text) {
					const username = await getQrcodeStatus(message.text)

					await provider.postMessageToWebview({
						type: "qrcodeStatus",
						username: username,
					})
					console.log("Extension: Sent qrcodeStatus to webview")

					// 如果登录成功，刷新状态
					if (username) {
						await provider.postStateToWebview()
					}
				}
			} catch (error) {
				console.error("Extension: Error in getQrcodeStatus handler:", error)
			}
			break
		case "getUserInfo":
			try {
				console.log("Extension: Received getUserInfo request")
				const username = getLoginWecodeUser()

				await provider.postMessageToWebview({
					type: "userInfo",
					username: username,
				})
				console.log("Extension: Sent userInfo to webview")
			} catch (error) {
				console.error("Extension: Error in getUserInfo handler:", error)
			}
			break
	}
}
